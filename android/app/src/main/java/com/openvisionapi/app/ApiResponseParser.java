package com.openvisionapi.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads only fields returned by Pixicular's requested label and text services. */
final class ApiResponseParser {
    static final class Label {
        final String name;
        final String confidence;
        final String categories;
        final float x, y, width, height;
        final boolean hasBox;
        Label(String name, String confidence, String categories, float x, float y, float width, float height, boolean hasBox) {
            this.name = name; this.confidence = confidence; this.categories = categories;
            this.x = x; this.y = y; this.width = width; this.height = height; this.hasBox = hasBox;
        }
    }

    final List<Label> labels = new ArrayList<>();
    final List<String> textLines = new ArrayList<>();
    String requestId = "", processingTime = "", credits = "";
    boolean hasUsefulResults;

    static ApiResponseParser parse(String raw) throws JSONException {
        if (raw == null || raw.trim().isEmpty()) throw new JSONException("The API returned an empty response.");
        JSONObject root = new JSONObject(raw);
        if (root.has("success") && !root.optBoolean("success", false)) {
            throw new JSONException(clean(root.optString("message", "The API reported that analysis failed.")));
        }
        ApiResponseParser parsed = new ApiResponseParser();
        JSONObject services = root.optJSONObject("services");
        // Some documented responses put service ids directly at the root.
        if (services == null) services = root;
        if (!services.has("detect-labels") && !services.has("detect-text")
                && !root.has("success") && !root.has("photo") && !root.has("meta")) {
            throw new JSONException("Response did not contain the requested service data.");
        }
        JSONObject labelService = services.optJSONObject("detect-labels");
        JSONArray labels = labelService == null ? null : labelService.optJSONArray("labels");
        if (labels != null) for (int i = 0; i < labels.length(); i++) {
            JSONObject item = labels.optJSONObject(i);
            if (item == null) continue;
            String name = clean(item.optString("name", "").trim());
            if (name.isEmpty()) continue;
            String confidence = confidence(item.opt("confidence"));
            JSONArray cats = item.optJSONArray("categories");
            if (cats == null) cats = item.optJSONArray("parents");
            StringBuilder categoryText = new StringBuilder();
            if (cats != null) for (int j = 0; j < cats.length(); j++) {
                String category = clean(cats.optString(j, "").trim());
                if (!category.isEmpty()) { if (categoryText.length() > 0) categoryText.append(", "); categoryText.append(category); }
            }
            JSONObject box = item.optJSONObject("boundingBox");
            float x = number(box, "x"), y = number(box, "y"), w = number(box, "width"), h = number(box, "height");
            boolean valid = box != null && Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(w) && Float.isFinite(h)
                    && x >= 0 && y >= 0 && w > 0 && h > 0 && x + w <= 1.0001f && y + h <= 1.0001f;
            parsed.labels.add(new Label(name, confidence, categoryText.toString(), x, y, w, h, valid));
        }
        JSONObject textService = services.optJSONObject("detect-text");
        JSONArray detections = textService == null ? null : textService.optJSONArray("detections");
        if (detections != null) for (int i = 0; i < detections.length(); i++) {
            JSONObject item = detections.optJSONObject(i);
            if (item == null) continue;
            String line = clean(item.optString("detectedText", "").trim());
            if (line.isEmpty()) continue;
            String type = clean(item.optString("type", "").trim());
            String confidence = confidence(item.opt("confidence"));
            if (!type.isEmpty()) line += " · " + type;
            if (!confidence.isEmpty()) line += " · " + confidence;
            parsed.textLines.add(line);
        }
        JSONObject meta = root.optJSONObject("meta");
        JSONObject photo = root.optJSONObject("photo");
        parsed.requestId = clean(root.optString("requestId", meta == null ? "" : meta.optString("requestId", "")));
        Object millis = root.has("processingTimeMs") ? root.opt("processingTimeMs") : meta == null ? null : meta.opt("processingTimeMs");
        if (millis instanceof Number && ((Number) millis).doubleValue() >= 0) parsed.processingTime = ((Number) millis).longValue() + " ms";
        Object cost = root.opt("creditCost");
        if (cost instanceof Number && ((Number) cost).doubleValue() >= 0) parsed.credits = ((Number) cost).toString();
        parsed.hasUsefulResults = !parsed.labels.isEmpty() || !parsed.textLines.isEmpty();
        return parsed;
    }

    private static float number(JSONObject object, String key) {
        if (object == null) return Float.NaN;
        Object value = object.opt(key);
        if (!(value instanceof Number)) return Float.NaN;
        return ((Number) value).floatValue();
    }
    private static String confidence(Object value) {
        if (!(value instanceof Number)) return "";
        double n = ((Number) value).doubleValue();
        if (!Double.isFinite(n) || n < 0 || n > 100) return "";
        if (n <= 1) n *= 100;
        return String.format(Locale.US, "%.1f%% confidence", n);
    }
    private static String clean(String value) { return value == null || "null".equals(value) ? "" : value; }
}
