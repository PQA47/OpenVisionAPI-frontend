package com.openvisionapi.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

final class AppNavigation {
    private AppNavigation() { }

    static Button createMenuButton(AppCompatActivity activity) {
        int textColor = activity.getColor(R.color.text_primary);
        int surface = activity.getColor(R.color.surface);
        int border = activity.getColor(R.color.border);
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText("☰");
        button.setTextSize(20);
        button.setTextColor(textColor);
        button.setMinWidth(dp(activity, 48));
        button.setMinHeight(dp(activity, 48));
        button.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
        button.setBackground(rounded(activity, surface, border, dp(activity, 6)));
        button.setContentDescription("Open navigation menu");
        button.setOnClickListener(view -> showMenu(activity));
        return button;
    }

    static void showMenu(AppCompatActivity activity) {
        int background = activity.getColor(R.color.app_background);
        int surface = activity.getColor(R.color.surface);
        int text = activity.getColor(R.color.text_primary);
        int muted = activity.getColor(R.color.text_secondary);
        int primary = activity.getColor(R.color.colorPrimary);
        int border = activity.getColor(R.color.border);
        int accentSurface = activity.getColor(R.color.accent_surface);

        Dialog dialog = new Dialog(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, 20), dp(activity, 24), dp(activity, 20), dp(activity, 20));
        panel.setBackground(rounded(activity, background, Color.TRANSPARENT, 0));

        LinearLayout brand = new LinearLayout(activity);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text(activity, "◈", 24, primary, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(activity, accentSurface, border, dp(activity, 6)));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));
        TextView title = text(activity, "OpenVisionAPI", 19, text, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -2);
        titleParams.leftMargin = dp(activity, 12);
        brand.addView(title, titleParams);
        panel.addView(brand);

        View divider = new View(activity);
        divider.setBackgroundColor(border);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(activity, 1));
        dividerParams.topMargin = dp(activity, 22);
        dividerParams.bottomMargin = dp(activity, 14);
        panel.addView(divider, dividerParams);

        addMenuItem(activity, panel, "Home", text, surface, border, () ->
                navigate(activity, dialog, LandingActivity.class));
        addMenuItem(activity, panel, "Analyze", text, surface, border, () ->
                navigate(activity, dialog, MainActivity.class));
        addMenuItem(activity, panel, "History", text, surface, border, () ->
                navigate(activity, dialog, HistoryActivity.class));
        addMenuItem(activity, panel, "Theme", text, surface, border, () -> {
            dialog.dismiss();
            ThemeManager.showPicker(activity);
        });

        View spacer = new View(activity);
        panel.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1));
        View footerDivider = new View(activity);
        footerDivider.setBackgroundColor(border);
        panel.addView(footerDivider, new LinearLayout.LayoutParams(-1, dp(activity, 1)));
        addMenuItem(activity, panel, "Quit", muted, surface, border, () -> {
            dialog.dismiss();
            activity.finishAffinity();
        });

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.gravity = Gravity.END | Gravity.TOP;
            attributes.dimAmount = 0.45f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min(dp(activity, 320), screenWidth - dp(activity, 48)),
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    static void openHome(Activity activity) {
        openActivity(activity, LandingActivity.class);
    }

    private static void addMenuItem(Activity activity, LinearLayout panel, String label, int textColor,
            int surface, int border, Runnable action) {
        TextView item = text(activity, label, 16, textColor, true);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setMinHeight(dp(activity, 52));
        item.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
        item.setBackground(ripple(activity, surface, border));
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(activity, 4);
        panel.addView(item, params);
    }

    private static void navigate(Activity activity, Dialog dialog, Class<?> destination) {
        if (dialog != null) dialog.dismiss();
        openActivity(activity, destination);
    }

    private static void openActivity(Activity activity, Class<?> destination) {
        if (activity.getClass() == destination) return;
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static TextView text(Activity activity, String value, int size, int color, boolean medium) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.create(
                medium ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private static android.graphics.drawable.Drawable ripple(Activity activity, int surface, int border) {
        GradientDrawable shape = rounded(activity, surface, border, dp(activity, 6));
        return new android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(activity.getColor(R.color.accent_surface)), shape, null);
    }

    private static GradientDrawable rounded(Activity activity, int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(activity, 1), stroke);
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
