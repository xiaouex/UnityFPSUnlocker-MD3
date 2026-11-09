package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Reads and writes the same preference keys that {@link MyModule} consumes.
 * Per-app keys only exist while independent configuration is enabled, so the
 * hook naturally falls back to global values after they are removed.
 */
final class ModuleSettings {
    static final int DEFAULT_FPS = 90;
    static final int DEFAULT_DELAY = 10;
    static final float DEFAULT_SCALE = -1.0f;
    static final boolean DEFAULT_MOD_OPCODE = true;

    private ModuleSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return Prefs.getSharedPrefs(context.getApplicationContext());
    }

    static int getGlobalFps(Context context) {
        return readInt(prefs(context), "fps", DEFAULT_FPS);
    }

    static void setGlobalFps(Context context, int value) {
        if (getGlobalFps(context) == value) {
            return;
        }
        prefs(context).edit().putInt("fps", value).apply();
    }

    static float getGlobalScale(Context context) {
        return readFloat(prefs(context), "scale", DEFAULT_SCALE);
    }

    static void setGlobalScale(Context context, float value) {
        if (Float.compare(getGlobalScale(context), value) == 0) {
            return;
        }
        prefs(context).edit().putFloat("scale", value).apply();
    }

    static int getGlobalDelay(Context context) {
        return readInt(prefs(context), "delay", DEFAULT_DELAY);
    }

    static void setGlobalDelay(Context context, int value) {
        if (getGlobalDelay(context) == value) {
            return;
        }
        prefs(context).edit().putInt("delay", value).apply();
    }

    static boolean getGlobalModOpcode(Context context) {
        return readBoolean(prefs(context), "mod_opcode", DEFAULT_MOD_OPCODE);
    }

    static void setGlobalModOpcode(Context context, boolean value) {
        if (getGlobalModOpcode(context) == value) {
            return;
        }
        prefs(context).edit().putBoolean("mod_opcode", value).apply();
    }

    static int getPerAppFps(Context context, String packageName) {
        return readInt(
                prefs(context),
                packageName + "_per_app_fps",
                getGlobalFps(context));
    }

    static void setPerAppFps(Context context, String packageName, int value) {
        if (getPerAppFps(context, packageName) == value) {
            return;
        }
        prefs(context).edit().putInt(packageName + "_per_app_fps", value).apply();
    }

    static float getPerAppScale(Context context, String packageName) {
        return readFloat(
                prefs(context),
                packageName + "_per_app_scale",
                getGlobalScale(context));
    }

    static void setPerAppScale(Context context, String packageName, float value) {
        if (Float.compare(getPerAppScale(context, packageName), value) == 0) {
            return;
        }
        prefs(context).edit().putFloat(packageName + "_per_app_scale", value).apply();
    }

    static int getPerAppDelay(Context context, String packageName) {
        return readInt(
                prefs(context),
                packageName + "_per_app_delay",
                getGlobalDelay(context));
    }

    static void setPerAppDelay(Context context, String packageName, int value) {
        if (getPerAppDelay(context, packageName) == value) {
            return;
        }
        prefs(context).edit().putInt(packageName + "_per_app_delay", value).apply();
    }

    static boolean getPerAppModOpcode(Context context, String packageName) {
        return readBoolean(
                prefs(context),
                packageName + "_per_app_mod_opcode",
                getGlobalModOpcode(context));
    }

    static void setPerAppModOpcode(Context context, String packageName, boolean value) {
        if (getPerAppModOpcode(context, packageName) == value) {
            return;
        }
        prefs(context).edit().putBoolean(packageName + "_per_app_mod_opcode", value).apply();
    }

    static boolean hasPerAppConfig(Context context, String packageName) {
        SharedPreferences preferences = prefs(context);
        return preferences.contains(packageName + "_per_app_fps")
                || preferences.contains(packageName + "_per_app_scale")
                || preferences.contains(packageName + "_per_app_delay")
                || preferences.contains(packageName + "_per_app_mod_opcode")
                || preferences.contains(packageName + "_per_app_display_mode_id");
    }

    static void enablePerAppConfig(Context context, String packageName) {
        prefs(context).edit()
                .putInt(packageName + "_per_app_fps", getGlobalFps(context))
                .putFloat(packageName + "_per_app_scale", getGlobalScale(context))
                .putInt(packageName + "_per_app_delay", getGlobalDelay(context))
                .putBoolean(packageName + "_per_app_mod_opcode", getGlobalModOpcode(context))
                .apply();
    }

    static void disablePerAppConfig(Context context, String packageName) {
        prefs(context).edit()
                .remove(packageName + "_per_app_fps")
                .remove(packageName + "_per_app_scale")
                .remove(packageName + "_per_app_delay")
                .remove(packageName + "_per_app_mod_opcode")
                .remove(packageName + "_per_app_display_mode_id")
                .apply();
    }

    private static int readInt(SharedPreferences preferences, String key, int fallback) {
        Object value = preferences.getAll().get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // Fall through to the caller-provided default.
            }
        }
        return fallback;
    }

    private static float readFloat(SharedPreferences preferences, String key, float fallback) {
        Object value = preferences.getAll().get(key);
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // Fall through to the caller-provided default.
            }
        }
        return fallback;
    }

    private static boolean readBoolean(
            SharedPreferences preferences,
            String key,
            boolean fallback) {
        Object value = preferences.getAll().get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }
}
