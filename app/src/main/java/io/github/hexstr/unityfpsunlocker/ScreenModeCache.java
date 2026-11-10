package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.view.Display;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the physical display modes once, stores them in the shared preferences
 * and exposes the cached list to the settings UI. The Xposed hook only reads
 * the stored list, so target processes never query the display service
 * themselves.
 */
final class ScreenModeCache {
    static final String KEY_MODES = "display_modes";
    static final String KEY_UPDATED_AT = "display_modes_updated_at";

    private ScreenModeCache() {
    }

    /**
     * Populates the cache on first launch. Subsequent launches reuse the stored
     * list until the user explicitly asks for a refresh.
     */
    static void ensureCached(Context context) {
        SharedPreferences preferences = Prefs.getSharedPrefs(context);
        Object cached = preferences.getAll().get(KEY_MODES);
        if (!(cached instanceof String) || DisplayModes.decode((String) cached).isEmpty()) {
            refresh(context);
        }
    }

    /**
     * Re-reads the modes from the display service and stores them.
     *
     * @return the freshly read modes, or an empty list when the query failed.
     */
    static List<DisplayModes.Mode> refresh(Context context) {
        List<DisplayModes.Mode> modes = query(context);
        if (modes.isEmpty()) {
            return modes;
        }
        Prefs.getSharedPrefs(context).edit()
                .putString(KEY_MODES, DisplayModes.encode(modes))
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
        return modes;
    }

    static List<DisplayModes.Mode> query(Context context) {
        List<DisplayModes.Mode> result = new ArrayList<>();
        try {
            DisplayManager manager =
                    (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            if (manager == null) {
                return result;
            }

            Display display = manager.getDisplay(Display.DEFAULT_DISPLAY);
            if (display == null) {
                Display[] displays = manager.getDisplays();
                if (displays != null && displays.length > 0) {
                    display = displays[0];
                }
            }
            if (display == null) {
                return result;
            }

            Display.Mode[] modes = display.getSupportedModes();
            if (modes == null || modes.length == 0) {
                Display.Mode current = display.getMode();
                if (current != null) {
                    modes = new Display.Mode[]{current};
                }
            }
            if (modes == null) {
                return result;
            }

            for (Display.Mode mode : modes) {
                if (mode == null) {
                    continue;
                }
                int width = mode.getPhysicalWidth();
                int height = mode.getPhysicalHeight();
                float refresh_rate = mode.getRefreshRate();
                if (width <= 0 || height <= 0 || !(refresh_rate > 0.0f)) {
                    continue;
                }
                result.add(new DisplayModes.Mode(
                        mode.getModeId(),
                        width,
                        height,
                        refresh_rate));
            }
            result.sort(Comparator.comparingInt(mode -> mode.id));
        } catch (Throwable ignored) {
            // A broken display service must not take down the settings screen.
            result.clear();
        }
        return result;
    }

    static long updatedAt(Context context) {
        return Prefs.getSharedPrefs(context).getLong(KEY_UPDATED_AT, 0L);
    }
}
