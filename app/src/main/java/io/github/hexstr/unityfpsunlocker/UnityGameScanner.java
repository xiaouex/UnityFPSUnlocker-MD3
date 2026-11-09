package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads only the packages selected in the LSPosed module scope. For those
 * packages it verifies that the APK actually ships a Unity runtime, then
 * exposes the result to both tabs.
 */
final class UnityGameScanner {
    enum State {
        IDLE,
        LOADING,
        READY,
        INACTIVE,
        EMPTY_SCOPE,
        ERROR
    }

    interface Listener {
        void onScanUpdated(State state, List<Game> games);
    }

    static final class Game {
        final String packageName;
        final String label;
        final Drawable icon;

        Game(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private static volatile UnityGameScanner instance;

    private final Context context;
    private final Handler main_handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<Game> games = new ArrayList<>();

    private volatile State state = State.IDLE;
    private int generation = 0;
    private boolean scan_started = false;

    private UnityGameScanner(Context context) {
        this.context = context.getApplicationContext();
        // The scope can become available after the UI has already been created.
        Prefs.addActivationListener(active -> refresh());
    }

    static UnityGameScanner get(Context context) {
        UnityGameScanner scanner = instance;
        if (scanner == null) {
            synchronized (UnityGameScanner.class) {
                scanner = instance;
                if (scanner == null) {
                    scanner = new UnityGameScanner(context);
                    instance = scanner;
                }
            }
        }
        return scanner;
    }

    State getState() {
        return state;
    }

    List<Game> getGames() {
        synchronized (games) {
            return Collections.unmodifiableList(new ArrayList<>(games));
        }
    }

    void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        listeners.addIfAbsent(listener);
        main_handler.post(() -> listener.onScanUpdated(state, getGames()));
    }

    void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    synchronized void startScan() {
        if (scan_started) {
            return;
        }
        scan_started = true;
        refresh();
    }

    void refresh() {
        int current_generation;
        synchronized (this) {
            current_generation = ++generation;
        }
        state = State.LOADING;
        main_handler.post(this::dispatch);
        executor.execute(() -> scanBlocking(current_generation));
    }

    private void scanBlocking(int scan_generation) {
        List<Game> found = new ArrayList<>();
        State next_state;
        try {
            if (!Prefs.isXposedActive()) {
                next_state = State.INACTIVE;
            } else {
                List<String> scope = Prefs.getScope();
                if (scope.isEmpty()) {
                    next_state = State.EMPTY_SCOPE;
                } else {
                    PackageManager package_manager = context.getPackageManager();
                    String own_package = context.getPackageName();
                    for (String package_name : scope) {
                        if (package_name == null
                                || package_name.isEmpty()
                                || package_name.equals(own_package)) {
                            continue;
                        }

                        ApplicationInfo application;
                        try {
                            application = package_manager.getApplicationInfo(package_name, 0);
                        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                            continue;
                        }

                        if (application == null
                                || !application.enabled
                                || package_manager.getLaunchIntentForPackage(package_name) == null
                                || !containsUnityRuntime(application)) {
                            continue;
                        }

                        String label = loadLabel(package_manager, application);
                        Drawable icon = loadIcon(package_manager, application);
                        found.add(new Game(application.packageName, label, icon));
                    }

                    Collator collator = Collator.getInstance(Locale.getDefault());
                    found.sort((left, right) -> collator.compare(left.label, right.label));
                    next_state = State.READY;
                }
            }
        } catch (Throwable t) {
            found.clear();
            next_state = State.ERROR;
        }

        synchronized (this) {
            if (scan_generation != generation) {
                return;
            }
            synchronized (games) {
                games.clear();
                games.addAll(found);
            }
            state = next_state;
        }
        main_handler.post(this::dispatch);
    }

    private void dispatch() {
        List<Game> snapshot = getGames();
        for (Listener listener : listeners) {
            listener.onScanUpdated(state, snapshot);
        }
    }

    private static String loadLabel(PackageManager package_manager, ApplicationInfo application) {
        CharSequence label = application.loadLabel(package_manager);
        String text = label == null ? "" : label.toString().trim();
        return text.isEmpty() ? application.packageName : text;
    }

    private static Drawable loadIcon(PackageManager package_manager, ApplicationInfo application) {
        try {
            return package_manager.getApplicationIcon(application);
        } catch (Throwable ignored) {
            return package_manager.getDefaultActivityIcon();
        }
    }

    private static boolean containsUnityRuntime(ApplicationInfo application) {
        String native_library_dir = application.nativeLibraryDir;
        if (native_library_dir != null) {
            File unity_library = new File(native_library_dir, "libunity.so");
            if (unity_library.isFile()) {
                return true;
            }
        }

        Set<String> apk_paths = new HashSet<>();
        addPath(apk_paths, application.sourceDir);
        addPaths(apk_paths, application.splitSourceDirs);
        addPaths(apk_paths, application.splitPublicSourceDirs);

        for (String apk_path : apk_paths) {
            if (apkContainsUnityLibrary(apk_path)) {
                return true;
            }
        }
        return false;
    }

    private static void addPaths(Set<String> paths, String[] values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addPath(paths, value);
        }
    }

    private static void addPath(Set<String> paths, String path) {
        if (path != null && !path.isEmpty()) {
            paths.add(path);
        }
    }

    private static boolean apkContainsUnityLibrary(String apk_path) {
        try (ZipFile zip_file = new ZipFile(apk_path)) {
            Enumeration<? extends ZipEntry> entries = zip_file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith("lib/")) {
                    continue;
                }
                int slash = name.lastIndexOf('/');
                String file_name = slash >= 0 ? name.substring(slash + 1) : name;
                if (file_name.equals("libunity.so")
                        || file_name.equals("libil2cpp.so")
                        || file_name.equals("libmonobdwgc-2.0.so")) {
                    return true;
                }
            }
        } catch (IOException | SecurityException ignored) {
            // Unreadable or vanished package; it simply cannot be classified as a Unity game.
        }
        return false;
    }
}
