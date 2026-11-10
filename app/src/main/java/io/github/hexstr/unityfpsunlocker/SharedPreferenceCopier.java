package io.github.hexstr.UnityFPSUnlocker;

import android.content.SharedPreferences;

final class SharedPreferenceCopier {
    private SharedPreferenceCopier() {
    }

    static void copyValue(
            SharedPreferences.Editor editor,
            String key,
            Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        }
    }
}
