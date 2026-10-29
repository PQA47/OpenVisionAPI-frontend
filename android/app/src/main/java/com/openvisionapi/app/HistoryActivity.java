package com.openvisionapi.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local analysis history. Opening a row restores the saved response; it does not call the API. */
public class HistoryActivity extends AppCompatActivity {
    private int BG, SURFACE, TEXT, MUTED, PURPLE, BORDER, SURFACE_VARIANT, PRIMARY, ON_PRIMARY, SUCCESS, ERROR;
    private LinearLayout list;
    private TextView count;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.applySavedMode(this);
        super.onCreate(state);
        loadThemeColors();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        boolean dark = isDarkTheme();
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(!dark);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom); return insets;
        });
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(12), dp(20), dp(12));
        Button back = button("← Back", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        TextView title = text("History", 20, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.leftMargin = dp(14);
        header.addView(title, tp);
        Button appearance = button(ThemeManager.buttonLabel(this), false);
        appearance.setContentDescription("Change application appearance");
        appearance.setOnClickListener(v -> ThemeManager.showPicker(this));
        header.addView(appearance);
        Button clear = button("Clear all", false);
        clear.setOnClickListener(v -> confirmClear());
        header.addView(clear);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content);
        count = text("Your previous image analyses", 13, MUTED, false);
        content.addView(count, params(0, 0, 0, 14));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refreshHistory(); }

    private void refreshHistory() {
        list.removeAllViews();
        List<HistoryStore.Entry> entries = HistoryStore.getAll(this);
        count.setText(entries.size() + (entries.size() == 1 ? " saved analysis" : " saved analyses"));
        if (entries.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(42), dp(24), dp(42));
            empty.setBackground(rounded(SURFACE, BORDER, 8));
            TextView icon = text("◷", 44, PURPLE, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, params(0, 0, 0, 14));
            TextView heading = text("No analyses yet", 19, TEXT, true);
            heading.setGravity(Gravity.CENTER);
            empty.addView(heading, params(0, 0, 0, 8));
            TextView detail = text("Images you analyze will appear here. You can reopen saved results without sending the image again.", 13, MUTED, false);
            detail.setGravity(Gravity.CENTER);
            detail.setLineSpacing(dp(4), 1f);
            empty.addView(detail, params(0, 0, 0, 18));
            Button analyze = button("Analyze an Image", true);
            analyze.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
            empty.addView(analyze);
            list.addView(empty);
            return;
        }
        for (HistoryStore.Entry entry : entries) addEntry(entry);
    }

    private void addEntry(HistoryStore.Entry entry) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));
        card.setBackground(rounded(SURFACE, BORDER, 6));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openEntry(entry));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView thumbnail = new ImageView(this);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setBackground(rounded(SURFACE_VARIANT, BORDER, 5));
        Bitmap bitmap = HistoryStore.loadThumbnail(entry);
        if (bitmap != null) thumbnail.setImageBitmap(bitmap);
        else thumbnail.setImageDrawable(null);
        top.addView(thumbnail, new LinearLayout.LayoutParams(dp(76), dp(76)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dpms = new LinearLayout.LayoutParams(0, -2, 1);
        dpms.leftMargin = dp(12);
        TextView name = text(entry.imageName.isEmpty() ? "Image analysis" : entry.imageName, 15, TEXT, true);
        name.setMaxLines(2);
        details.addView(name, params(0, 0, 0, 5));
        TextView date = text(formatDate(entry.timestamp), 11, MUTED, false);
        details.addView(date, params(0, 0, 0, 6));
        TextView status = text(statusLabel(entry), 11, entry.status.equals(HistoryStore.STATUS_SUCCESS)
                ? SUCCESS : ERROR, true);
        details.addView(status);
        top.addView(details, dpms);
        card.addView(top);

        TextView summary = text(entry.summary, 13, MUTED, false);
        summary.setMaxLines(3);
        summary.setLineSpacing(dp(3), 1f);
        card.addView(summary, params(0, 12, 0, 8));
        if (entry.status.equals(HistoryStore.STATUS_SUCCESS)) {
            TextView meta = text(entry.labelCount + (entry.labelCount == 1 ? " detected label" : " detected labels")
                    + (entry.mock ? "  ·  DEMO" : ""), 11, PURPLE, true);
            card.addView(meta, params(0, 0, 0, 8));
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button open = button("View result", true);
        open.setOnClickListener(v -> openEntry(entry));
        actions.addView(open);
        Button delete = button("Delete", false);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, -2);
        deleteParams.leftMargin = dp(8);
        delete.setLayoutParams(deleteParams);
        delete.setOnClickListener(v -> confirmDelete(entry));
        actions.addView(delete);
        card.addView(actions, params(0, 10, 0, 0));
        list.addView(card, params(0, 0, 0, 12));
    }

    private void openEntry(HistoryStore.Entry entry) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_HISTORY_ID, entry.id);
        startActivity(intent);
    }

    private String statusLabel(HistoryStore.Entry entry) {
        if (entry.status.equals(HistoryStore.STATUS_SUCCESS)) return entry.mock ? "SUCCESS · MOCK DATA" : "SUCCESS";
        return entry.mock ? "FAILED · MOCK TEST" : "FAILED";
    }
    private String formatDate(long time) {
        return new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(new Date(time));
    }
    private void confirmDelete(HistoryStore.Entry entry) {
        new AlertDialog.Builder(this).setTitle("Delete this analysis?")
                .setMessage("This removes the saved result and its thumbnail from this device.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Delete", (d, w) -> { HistoryStore.delete(this, entry.id); refreshHistory(); })
                .show();
    }
    private void confirmClear() {
        List<HistoryStore.Entry> entries = HistoryStore.getAll(this);
        if (entries.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("Clear all history?")
                .setMessage("All saved analysis results and thumbnails will be deleted from this device. This cannot be undone.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear history", (d, w) -> { HistoryStore.clear(this); refreshHistory(); })
                .show();
    }

    private boolean isDarkTheme() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void loadThemeColors() {
        BG = getColor(R.color.app_background); SURFACE = getColor(R.color.surface);
        TEXT = getColor(R.color.text_primary); MUTED = getColor(R.color.text_secondary);
        PURPLE = getColor(R.color.colorPrimary); BORDER = getColor(R.color.border);
        SURFACE_VARIANT = getColor(R.color.surface_variant); PRIMARY = getColor(R.color.colorPrimary);
        ON_PRIMARY = getColor(R.color.on_primary); SUCCESS = getColor(R.color.success); ERROR = getColor(R.color.error);
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(label); b.setTextSize(12);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setTextColor(primary ? ON_PRIMARY : TEXT);
        b.setMinHeight(dp(42)); b.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.setBackground(rounded(primary ? PRIMARY : SURFACE, primary ? Color.TRANSPARENT : BORDER, 5));
        return b;
    }
    private TextView text(String value, int size, int color, boolean medium) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false); return t;
    }
    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d;
    }
    private LinearLayout.LayoutParams params(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
