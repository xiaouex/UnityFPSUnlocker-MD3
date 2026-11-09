package io.github.hexstr.UnityFPSUnlocker;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Toggles the launcher activity-alias only. MainActivity keeps the Xposed
 * MODULE_SETTINGS intent filter, so LSPosed can still open the module after
 * the desktop icon has been hidden.
 */
final class DesktopIconController {
    private static final String LAUNCHER_ALIAS = ".LauncherActivity";

    private DesktopIconController() {
    }

    static boolean isHidden(Context context) {
        int state = context.getPackageManager().getComponentEnabledSetting(
                launcherAlias(context));
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    static boolean setHidden(Context context, boolean hidden) {
        int state = hidden
                ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        try {
            context.getPackageManager().setComponentEnabledSetting(
                    launcherAlias(context),
                    state,
                    PackageManager.DONT_KILL_APP);
            return true;
        } catch (IllegalArgumentException | SecurityException ignored) {
            return false;
        }
    }

    private static ComponentName launcherAlias(Context context) {
        String package_name = context.getPackageName();
        return new ComponentName(package_name, package_name + LAUNCHER_ALIAS);
    }
}
