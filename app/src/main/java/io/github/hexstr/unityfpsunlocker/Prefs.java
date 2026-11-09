package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Prefs {
    public static final String prefs_name_ = "fps_prefs";
    private static final String tag_ = "UnityFPSUnlocker";

    private static volatile XposedService xposed_service_srv_ = null;
    private static volatile boolean listener_registered_ = false;
    private static final Handler main_handler_ = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<ActivationListener> activation_listeners_ =
            new CopyOnWriteArrayList<>();

    public interface ActivationListener {
        void onActivationChanged(boolean active);
    }

    private static final SharedPreferences.OnSharedPreferenceChangeListener pref_change_listener_ =
            (shared_preferences, key) -> syncKeyToRemote(shared_preferences, key);

    public static synchronized void init(Context context) {
        if (!listener_registered_) {
            listener_registered_ = true;
            final Context app_context = context.getApplicationContext();

            SharedPreferences local_prefs = getSharedPrefs(app_context);
            local_prefs.registerOnSharedPreferenceChangeListener(pref_change_listener_);

            XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
                @Override
                public void onServiceBind(@NonNull XposedService service) {
                    xposed_service_srv_ = service;
                    syncAllToRemote(app_context);
                    notifyActivationChanged(true);
                }

                @Override
                public void onServiceDied(@NonNull XposedService service) {
                    if (xposed_service_srv_ == service) {
                        xposed_service_srv_ = null;
                        notifyActivationChanged(false);
                    }
                }
            });
        }
    }

    public static boolean isXposedActive() {
        return xposed_service_srv_ != null;
    }

    public static void addActivationListener(ActivationListener listener) {
        if (listener != null) {
            activation_listeners_.addIfAbsent(listener);
        }
    }

    public static void removeActivationListener(ActivationListener listener) {
        if (listener != null) {
            activation_listeners_.remove(listener);
        }
    }

    private static void notifyActivationChanged(boolean active) {
        main_handler_.post(() -> {
            for (ActivationListener listener : activation_listeners_) {
                listener.onActivationChanged(active);
            }
        });
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(prefs_name_, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getRemotePrefs() {
        XposedService service = xposed_service_srv_;
        if (service != null) {
            try {
                return service.getRemotePreferences(prefs_name_);
            } catch (Throwable t) {
                Log.w(tag_, "Failed to get remote preferences", t);
            }
        }
        return null;
    }

    public static List<String> getScope() {
        XposedService service = xposed_service_srv_;
        if (service != null) {
            try {
                List<String> scope = service.getScope();
                return scope == null ? Collections.emptyList() : scope;
            } catch (Throwable t) {
                Log.w(tag_, "Failed to get module scope", t);
            }
        }
        return Collections.emptyList();
    }

    public static void syncAllToRemote(Context context) {
        SharedPreferences remote_prefs = getRemotePrefs();
        if (remote_prefs != null) {
            SharedPreferences local_prefs = getSharedPrefs(context);
            Map<String, ?> local_values = local_prefs.getAll();
            SharedPreferences.Editor editor = remote_prefs.edit();
            for (Map.Entry<String, ?> entry : local_values.entrySet()) {
                SharedPreferenceCopier.copyValue(editor, entry.getKey(), entry.getValue());
            }
            // Local preferences are the source of truth; drop stale remote overrides.
            for (String key : remote_prefs.getAll().keySet()) {
                if (!local_values.containsKey(key)) {
                    editor.remove(key);
                }
            }
            editor.apply();
        }
    }

    private static void syncKeyToRemote(SharedPreferences local_prefs, String key) {
        SharedPreferences remote_prefs = getRemotePrefs();
        if (remote_prefs == null || key == null) {
            return;
        }

        Object value = local_prefs.getAll().get(key);
        SharedPreferences.Editor editor = remote_prefs.edit();
        if (value == null) {
            editor.remove(key);
        } else {
            SharedPreferenceCopier.copyValue(editor, key, value);
        }
        editor.apply();
    }
}
