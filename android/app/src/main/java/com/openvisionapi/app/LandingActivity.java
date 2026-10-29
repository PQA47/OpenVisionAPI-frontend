package com.openvisionapi.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Welcome screen. The existing analysis experience remains in MainActivity. */
public class LandingActivity extends AppCompatActivity {
    private int BG, SURFACE, TEXT, MUTED, PURPLE, BORDER, SURFACE_VARIANT, ACCENT_SURFACE, PRIMARY, ON_PRIMARY;

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
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(22), dp(14), dp(22), dp(14));
        TextView mark = text("◈", 25, PURPLE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(ACCENT_SURFACE, BORDER, 6));
        header.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView brand = text("OpenVisionAPI", 19, TEXT, true);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, -2, 1);
        brandParams.leftMargin = dp(12);
        header.addView(brand, brandParams);
        Button appearance = button(ThemeManager.buttonLabel(this), false);
        appearance.setContentDescription("Change appearance. Current mode: " + ThemeManager.label(ThemeManager.getMode(this)));
        appearance.setOnClickListener(v -> ThemeManager.showPicker(this));
        header.addView(appearance);
        Button history = button("History", false);
        history.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-2, -2);
        historyParams.leftMargin = dp(8);
        header.addView(history, historyParams);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(30), dp(24), dp(36));
        scroll.addView(content);

        TextView badge = text("AI VISION WORKSPACE  ·  IMAGE INSIGHTS", 11, PURPLE, true);
        badge.setLetterSpacing(0.08f);
        badge.setPadding(dp(13), dp(10), dp(13), dp(10));
        badge.setBackground(rounded(ACCENT_SURFACE, BORDER, 6));
        content.addView(badge, params(0, 0, 0, 24));

        TextView headline = text("Understand images\nwith AI.", 40, TEXT, true);
        headline.setLetterSpacing(-0.035f);
        headline.setLineSpacing(dp(2), 1f);
        content.addView(headline, params(0, 0, 0, 16));

        TextView description = text("Turn visual information into useful insights. Submit an image, explore detected objects and extracted text, then revisit your previous analysis whenever you need it.", 16, MUTED, false);
        description.setLineSpacing(dp(6), 1f);
        content.addView(description, params(0, 0, 0, 24));

        LinearLayout visual = new LinearLayout(this);
        visual.setOrientation(LinearLayout.VERTICAL);
        visual.setPadding(dp(20), dp(20), dp(20), dp(20));
        visual.setBackground(rounded(SURFACE, BORDER, 6));
        TextView visualIcon = text("◉", 54, PURPLE, true);
        visualIcon.setGravity(Gravity.CENTER);
        visualIcon.setBackground(rounded(ACCENT_SURFACE, BORDER, 8));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(94), dp(94));
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        visual.addView(visualIcon, iconParams);
        TextView visualTitle = text("From pixels to understanding", 17, TEXT, true);
        visualTitle.setGravity(Gravity.CENTER);
        visual.addView(visualTitle, params(0, 18, 0, 8));
        TextView visualDescription = text("Object labels  ·  Text extraction  ·  Saved results", 12, MUTED, false);
        visualDescription.setGravity(Gravity.CENTER);
        visualDescription.setLineSpacing(dp(4), 1f);
        visual.addView(visualDescription);
        content.addView(visual, params(0, 0, 0, 24));

        Button start = button("Analyze an Image  →", true);
        start.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        content.addView(start, params(0, 0, 0, 12));
        Button viewHistory = button("View Analysis History", false);
        viewHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        content.addView(viewHistory, params(0, 0, 0, 24));

        LinearLayout steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.VERTICAL);
        steps.setPadding(dp(18), dp(16), dp(18), dp(16));
        steps.setBackground(rounded(SURFACE_VARIANT, BORDER, 6));
        addStep(steps, "01", "Choose", "Pick a PNG, JPG or WEBP image.");
        addStep(steps, "02", "Analyze", "Use the configured vision API or the built-in demo mode.");
        addStep(steps, "03", "Review", "Read the result and reopen it from local history.");
        content.addView(steps);
    }

    private boolean isDarkTheme() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void loadThemeColors() {
        BG = getColor(com.openvisionapi.app.R.color.app_background);
        SURFACE = getColor(com.openvisionapi.app.R.color.surface);
        TEXT = getColor(com.openvisionapi.app.R.color.text_primary);
        MUTED = getColor(com.openvisionapi.app.R.color.text_secondary);
        PURPLE = getColor(com.openvisionapi.app.R.color.colorPrimary);
        BORDER = getColor(com.openvisionapi.app.R.color.border);
        SURFACE_VARIANT = getColor(com.openvisionapi.app.R.color.surface_variant);
        ACCENT_SURFACE = getColor(com.openvisionapi.app.R.color.accent_surface);
        PRIMARY = getColor(com.openvisionapi.app.R.color.colorPrimary);
        ON_PRIMARY = getColor(com.openvisionapi.app.R.color.on_primary);
    }

    private void addStep(LinearLayout parent, String number, String title, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView num = text(number, 12, PURPLE, true);
        num.setGravity(Gravity.CENTER);
        num.setBackground(rounded(ACCENT_SURFACE, BORDER, 5));
        row.addView(num, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
        cp.leftMargin = dp(13);
        column.addView(text(title, 14, TEXT, true));
        TextView detail = text(description, 12, MUTED, false);
        detail.setPadding(0, dp(3), 0, 0);
        column.addView(detail);
        row.addView(column, cp);
        parent.addView(row);
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? ON_PRIMARY : TEXT);
        button.setMinHeight(dp(48));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setBackground(rounded(primary ? PRIMARY : SURFACE,
                primary ? Color.TRANSPARENT : BORDER, 6));
        return button;
    }

    private TextView text(String value, int size, int color, boolean medium) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }
    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill); drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }
    private LinearLayout.LayoutParams params(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
