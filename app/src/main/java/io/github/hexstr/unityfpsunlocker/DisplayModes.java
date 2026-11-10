package io.github.hexstr.UnityFPSUnlocker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure-Java representation of the display modes reported by the system, plus
 * the frame-rate matching used by both the settings UI and the Xposed hook.
 *
 * <p>This class deliberately has no Android imports so it can be loaded in a
 * target application process without pulling in UI-only classes.</p>
 */
final class DisplayModes {
    private static final char MODE_SEPARATOR = ';';
    private static final char FIELD_SEPARATOR = '|';

    static final class Mode {
        final int id;
        final int width;
        final int height;
        final float refreshRate;

        Mode(int id, int width, int height, float refreshRate) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.refreshRate = refreshRate;
        }

        long pixels() {
            return (long) width * (long) height;
        }

        String describe() {
            return width + "×" + height + " @ " + formatRefresh(refreshRate) + "Hz (ID " + id + ")";
        }
    }

    private DisplayModes() {
    }

    /**
     * Serializes the mode list into a compact, locale-independent string so it
     * can be stored in the preferences that are shared with target processes.
     */
    static String encode(List<Mode> modes) {
        if (modes == null || modes.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Mode mode : modes) {
            if (mode == null
                    || mode.id < 0
                    || mode.width <= 0
                    || mode.height <= 0
                    || !(mode.refreshRate > 0.0f)
                    || !Float.isFinite(mode.refreshRate)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(MODE_SEPARATOR);
            }
            builder.append(mode.id).append(FIELD_SEPARATOR)
                    .append(mode.width).append(FIELD_SEPARATOR)
                    .append(mode.height).append(FIELD_SEPARATOR)
                    .append(Float.toString(mode.refreshRate));
        }
        return builder.toString();
    }

    static List<Mode> decode(String encoded) {
        List<Mode> modes = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return modes;
        }
        String[] entries = encoded.split(String.valueOf(MODE_SEPARATOR));
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            String[] fields = entry.split("\\|");
            if (fields.length != 4) {
                continue;
            }
            try {
                int id = Integer.parseInt(fields[0]);
                int width = Integer.parseInt(fields[1]);
                int height = Integer.parseInt(fields[2]);
                float refresh_rate = Float.parseFloat(fields[3]);
                if (id >= 0
                        && width > 0
                        && height > 0
                        && refresh_rate > 0.0f
                        && Float.isFinite(refresh_rate)) {
                    modes.add(new Mode(id, width, height, refresh_rate));
                }
            } catch (NumberFormatException ignored) {
                // Skip a corrupt entry; the remaining cached modes are still usable.
            }
        }
        return modes;
    }

    static int matchId(String encoded, int fps) {
        Mode mode = match(decode(encoded), fps);
        return mode == null ? -1 : mode.id;
    }

    /**
     * Chooses the display mode whose refresh rate matches {@code fps} and, among
     * all modes with that refresh rate, keeps the highest resolution. When the
     * exact refresh rate is unavailable, the closest one is used.
     */
    static Mode match(List<Mode> modes, int fps) {
        if (modes == null || modes.isEmpty() || fps <= 0) {
            return null;
        }

        Mode exact = null;
        Mode nearest = null;
        int nearest_diff = Integer.MAX_VALUE;

        for (Mode mode : modes) {
            if (mode == null) {
                continue;
            }
            int rounded_refresh = Math.round(mode.refreshRate);
            int diff = Math.abs(rounded_refresh - fps);
            if (diff == 0) {
                if (betterResolution(mode, exact)) {
                    exact = mode;
                }
            } else if (diff < nearest_diff
                    || (diff == nearest_diff && betterFallback(mode, nearest))) {
                nearest = mode;
                nearest_diff = diff;
            }
        }
        return exact != null ? exact : nearest;
    }

    private static boolean betterResolution(Mode candidate, Mode current) {
        if (current == null) {
            return true;
        }
        long candidate_pixels = candidate.pixels();
        long current_pixels = current.pixels();
        if (candidate_pixels != current_pixels) {
            return candidate_pixels > current_pixels;
        }
        if (candidate.width != current.width) {
            return candidate.width > current.width;
        }
        if (candidate.height != current.height) {
            return candidate.height > current.height;
        }
        if (Float.compare(candidate.refreshRate, current.refreshRate) != 0) {
            return candidate.refreshRate > current.refreshRate;
        }
        return candidate.id < current.id;
    }

    private static boolean betterFallback(Mode candidate, Mode current) {
        if (current == null) {
            return true;
        }
        if (Float.compare(candidate.refreshRate, current.refreshRate) != 0) {
            return candidate.refreshRate > current.refreshRate;
        }
        return betterResolution(candidate, current);
    }

    static String formatRefresh(float refresh_rate) {
        if (Math.abs(refresh_rate - Math.round(refresh_rate)) < 0.05f) {
            return String.valueOf(Math.round(refresh_rate));
        }
        return String.format(Locale.US, "%.2f", refresh_rate);
    }
}
