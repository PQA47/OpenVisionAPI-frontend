package com.openvisionapi.app;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

/** Persists the user's appearance preference and applies it consistently to all activities. */
public final class ThemeManager {
    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";
    private static final String PREFS = "appearance_preferences";
    private static final String KEY_MODE = "theme_mode";

    private ThemeManager() { }

    public static String getMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, SYSTEM);
    }

    public static void applySavedMode(Context context) {
        applyMode(getMode(context));
    }

    public static void setMode(Context context, String mode) {
        String safeMode = LIGHT.equals(mode) || DARK.equals(mode) ? mode : SYSTEM;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODE, safeMode).apply();
        applyMode(safeMode);
    }

    private static void applyMode(String mode) {
        int delegateMode;
        if (LIGHT.equals(mode)) delegateMode = AppCompatDelegate.MODE_NIGHT_NO;
        else if (DARK.equals(mode)) delegateMode = AppCompatDelegate.MODE_NIGHT_YES;
        else delegateMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        AppCompatDelegate.setDefaultNightMode(delegateMode);
    }

    public static String label(String mode) {
        if (LIGHT.equals(mode)) return "Light";
        if (DARK.equals(mode)) return "Dark";
        return "System";
    }

    public static String buttonLabel(Context context) {
        return "Theme · " + label(getMode(context));
    }

    public static void showPicker(Context context) {
        String[] options = {"System default", "Light mode", "Dark mode"};
        String[] modes = {SYSTEM, LIGHT, DARK};
        String current = getMode(context);
        int checked = SYSTEM.equals(current) ? 0 : LIGHT.equals(current) ? 1 : 2;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Appearance")
                .setSingleChoiceItems(options, checked, (d, which) -> {
                    setMode(context, modes[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }
}
