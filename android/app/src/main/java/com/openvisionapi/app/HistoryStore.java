package com.openvisionapi.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Small local history store. Only thumbnails and result JSON are persisted, never credentials. */
final class HistoryStore {
    static final String STATUS_SUCCESS = "SUCCESS";
    static final String STATUS_FAILED = "FAILED";
    private static final String PREFS = "analysis_history";
    private static final String KEY_ENTRIES = "entries";

    static final class Entry {
        final String id;
        final long timestamp;
        final String imageName;
        final String thumbnailPath;
        final String responseJson;
        final String status;
        final boolean mock;
        final String errorMessage;
        final String summary;
        final int labelCount;

        Entry(String id, long timestamp, String imageName, String thumbnailPath,
              String responseJson, String status, boolean mock, String errorMessage,
              String summary, int labelCount) {
            this.id = id;
            this.timestamp = timestamp;
            this.imageName = imageName;
            this.thumbnailPath = thumbnailPath;
            this.responseJson = responseJson;
            this.status = status;
            this.mock = mock;
            this.errorMessage = errorMessage;
            this.summary = summary;
            this.labelCount = labelCount;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("timestamp", timestamp);
            object.put("imageName", imageName);
            object.put("thumbnailPath", thumbnailPath);
            object.put("responseJson", responseJson);
            object.put("status", status);
            object.put("mock", mock);
            object.put("errorMessage", errorMessage);
            object.put("summary", summary);
            object.put("labelCount", labelCount);
            return object;
        }

        static Entry fromJson(JSONObject object) {
            return new Entry(object.optString("id", UUID.randomUUID().toString()),
                    object.optLong("timestamp", 0L), object.optString("imageName", "Image"),
                    object.optString("thumbnailPath", ""), object.optString("responseJson", ""),
                    object.optString("status", STATUS_FAILED), object.optBoolean("mock", false),
                    object.optString("errorMessage", ""), object.optString("summary", "Analysis session"),
                    Math.max(0, object.optInt("labelCount", 0)));
        }
    }

    private HistoryStore() {}

    static synchronized Entry save(Context context, byte[] imageBytes, String imageName,
                                   String responseJson, String status, boolean mock,
                                   String errorMessage, String summary, int labelCount) throws IOException {
        Context app = context.getApplicationContext();
        long now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        String thumbnailPath = "";
        try { thumbnailPath = saveThumbnail(app, id, imageBytes); }
        catch (IOException ignored) { /* Preserve the result even when a thumbnail cannot be written. */ }
        Entry entry = new Entry(id, now, imageName == null ? "Image" : imageName,
                thumbnailPath, responseJson == null ? "" : responseJson,
                status, mock, errorMessage == null ? "" : errorMessage,
                summary == null || summary.trim().isEmpty() ? "Analysis session" : summary,
                Math.max(0, labelCount));
        List<Entry> entries = readAll(app);
        entries.add(0, entry);
        writeAll(app, entries);
        return entry;
    }

    static synchronized List<Entry> getAll(Context context) {
        List<Entry> entries = readAll(context.getApplicationContext());
        Collections.sort(entries, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return entries;
    }

    static synchronized Entry get(Context context, String id) {
        if (id == null || id.isEmpty()) return null;
        for (Entry entry : readAll(context.getApplicationContext())) {
            if (id.equals(entry.id)) return entry;
        }
        return null;
    }

    static synchronized void delete(Context context, String id) {
        Context app = context.getApplicationContext();
        List<Entry> entries = readAll(app);
        List<Entry> remaining = new ArrayList<>();
        for (Entry entry : entries) {
            if (id.equals(entry.id)) deleteThumbnail(entry.thumbnailPath);
            else remaining.add(entry);
        }
        writeAll(app, remaining);
    }

    static synchronized void clear(Context context) {
        Context app = context.getApplicationContext();
        for (Entry entry : readAll(app)) deleteThumbnail(entry.thumbnailPath);
        writeAll(app, new ArrayList<>());
    }

    private static List<Entry> readAll(Context context) {
        List<Entry> entries = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ENTRIES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                try { entries.add(Entry.fromJson(object)); } catch (RuntimeException ignored) { }
            }
        } catch (JSONException ignored) {
            // A corrupted preference should result in an empty history, not an app crash.
        }
        return entries;
    }

    private static void writeAll(Context context, List<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            try { array.put(entry.toJson()); } catch (JSONException ignored) { }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_ENTRIES, array.toString()).apply();
    }

    private static String saveThumbnail(Context context, String id, byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) return "";
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return "";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.max(bounds.outWidth / 320, bounds.outHeight / 320));
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
        if (bitmap == null) return "";
        File directory = new File(context.getFilesDir(), "history_thumbnails");
        if (!directory.exists() && !directory.mkdirs()) {
            bitmap.recycle();
            throw new IOException("Could not create the history thumbnail directory.");
        }
        File file = new File(directory, id + ".jpg");
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) {
                throw new IOException("Could not save the history thumbnail.");
            }
        } finally {
            bitmap.recycle();
        }
        return file.getAbsolutePath();
    }

    static Bitmap loadThumbnail(Entry entry) {
        if (entry == null || entry.thumbnailPath.isEmpty()) return null;
        File file = new File(entry.thumbnailPath);
        if (!file.isFile()) return null;
        try { return BitmapFactory.decodeFile(file.getAbsolutePath()); }
        catch (RuntimeException ignored) { return null; }
    }

    private static void deleteThumbnail(String path) {
        if (path == null || path.isEmpty()) return;
        try { new File(path).delete(); } catch (RuntimeException ignored) { }
    }
}
