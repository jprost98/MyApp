package com.example.myapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {

    private static final String PREFS = "SAVED_PREFERENCES"; // same file SettingsFragment already uses
    private static final String KEY_THEME = "app_theme";

    public enum AppTheme {
        SLATE(R.style.MyAppTheme, "Slate"),
        FOREST(R.style.Theme_MyApp_Forest, "Forest"),
        MIDNIGHT(R.style.Theme_MyApp_Midnight, "Midnight"),
        LAVENDER(R.style.Theme_MyApp_Lavender, "Lavender");

        public final int styleRes;
        public final String label;

        AppTheme(int styleRes, String label) {
            this.styleRes = styleRes;
            this.label = label;
        }

        public static AppTheme fromLabel(String label) {
            for (AppTheme t : values()) {
                if (t.label.equals(label)) return t;
            }
            return SLATE;
        }
    }

    public static AppTheme getSelected(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_THEME, AppTheme.SLATE.name());
        try {
            return AppTheme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AppTheme.SLATE;
        }
    }

    public static void setSelected(Context context, AppTheme theme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme.name())
                .apply();
    }

    /** Call before super.onCreate() in every activity. */
    public static void applyTheme(Activity activity) {
        activity.setTheme(getSelected(activity).styleRes);
    }
}