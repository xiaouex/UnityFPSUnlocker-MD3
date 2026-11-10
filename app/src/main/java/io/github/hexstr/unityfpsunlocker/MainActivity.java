package io.github.hexstr.UnityFPSUnlocker;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.List;

import io.github.hexstr.UnityFPSUnlocker.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG_HOME = "home";
    private static final String TAG_CONFIG = "config";
    private static final String STATE_SELECTED_TAB = "selected_tab";

    private ActivityMainBinding binding;
    private HomeFragment home_fragment;
    private ConfigFragment config_fragment;
    private int selected_tab = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        Prefs.init(this);
        // Screen modes are queried only once; later launches reuse the cache.
        ScreenModeCache.ensureCached(this);
        UnityGameScanner.get(this).startScan();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setTitle(R.string.app_name);
        binding.toolbar.setSubtitle("v" + BuildConfig.VERSION_NAME);

        setupFragments(savedInstanceState);
        setupBottomNavigation();
        applyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LSPosed scope may have changed while the module UI was in the background.
        UnityGameScanner.get(this).refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean home_selected = selected_tab == R.id.nav_home;

        MenuItem hide_icon_item = menu.findItem(R.id.action_hide_desktop_icon);
        if (hide_icon_item != null) {
            hide_icon_item.setVisible(home_selected);
            hide_icon_item.setChecked(DesktopIconController.isHidden(this));
        }

        MenuItem refresh_item = menu.findItem(R.id.action_refresh_display_info);
        if (refresh_item != null) {
            refresh_item.setVisible(home_selected);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_display_info) {
            refreshScreenModes();
            return true;
        }
        if (item.getItemId() == R.id.action_hide_desktop_icon) {
            boolean hide = !DesktopIconController.isHidden(this);
            if (!DesktopIconController.setHidden(this, hide)) {
                Toast.makeText(
                        this,
                        R.string.hide_desktop_icon_failed,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            item.setChecked(hide);
            invalidateOptionsMenu();
            Toast.makeText(
                    this,
                    hide ? R.string.desktop_icon_hidden : R.string.desktop_icon_restored,
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Re-reads the supported display modes and stores them for the hook. The
     * toast shows which mode the current global FPS would resolve to.
     */
    private void refreshScreenModes() {
        List<DisplayModes.Mode> modes = ScreenModeCache.refresh(this);
        if (modes.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.refresh_display_info_failed,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int fps = ModuleSettings.getGlobalFps(this);
        DisplayModes.Mode matched = DisplayModes.match(modes, fps);
        String message = matched == null
                ? getString(R.string.refresh_display_info_no_match, modes.size(), fps)
                : getString(
                        R.string.refresh_display_info_result,
                        modes.size(),
                        fps,
                        matched.describe());
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void configureWindow() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        boolean light =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        != Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }

    private void setupFragments(Bundle savedInstanceState) {
        FragmentManager fragment_manager = getSupportFragmentManager();
        home_fragment = (HomeFragment) fragment_manager.findFragmentByTag(TAG_HOME);
        config_fragment = (ConfigFragment) fragment_manager.findFragmentByTag(TAG_CONFIG);

        if (home_fragment == null || config_fragment == null) {
            FragmentTransaction transaction = fragment_manager.beginTransaction();
            if (config_fragment == null) {
                config_fragment = new ConfigFragment();
                transaction.add(R.id.fragment_container, config_fragment, TAG_CONFIG);
            }
            if (home_fragment == null) {
                home_fragment = new HomeFragment();
                transaction.add(R.id.fragment_container, home_fragment, TAG_HOME);
            }
            transaction.hide(config_fragment);
            transaction.commitNow();
        }

        if (savedInstanceState != null) {
            selected_tab = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        }
        showTab(selected_tab);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(selected_tab);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int item_id = item.getItemId();
            if (item_id != R.id.nav_home && item_id != R.id.nav_config) {
                return false;
            }
            if (selected_tab == item_id) {
                return true;
            }
            clearFocusAndKeyboard();
            showTab(item_id);
            return true;
        });
    }

    private void showTab(int item_id) {
        Fragment target = item_id == R.id.nav_config ? config_fragment : home_fragment;
        Fragment other = item_id == R.id.nav_config ? home_fragment : config_fragment;
        if (target == null || !target.isAdded()) {
            return;
        }

        boolean home_selected = item_id == R.id.nav_home;
        selected_tab = item_id;
        binding.toolbar.setTitle(home_selected ? R.string.app_name : R.string.config_title);
        binding.toolbar.setSubtitle(
                home_selected ? "v" + BuildConfig.VERSION_NAME : null);
        MenuItem icon_item = binding.toolbar.getMenu().findItem(R.id.action_hide_desktop_icon);
        if (icon_item != null) {
            icon_item.setVisible(home_selected);
        }
        invalidateOptionsMenu();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        transaction.show(target);
        if (other != null && other.isAdded()) {
            transaction.hide(other);
        }
        transaction.commitNow();
    }

    private void clearFocusAndKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
        }
        InputMethodManager input_method_manager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (input_method_manager != null && binding != null) {
            input_method_manager.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
    }

    private void applyInsets() {
        final int app_bar_left = binding.appBar.getPaddingLeft();
        final int app_bar_top = binding.appBar.getPaddingTop();
        final int app_bar_right = binding.appBar.getPaddingRight();
        final int app_bar_bottom = binding.appBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar, (view, window_insets) -> {
            Insets insets = window_insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    app_bar_left + insets.left,
                    app_bar_top + insets.top,
                    app_bar_right + insets.right,
                    app_bar_bottom);
            return window_insets;
        });

        final int container_left = binding.fragmentContainer.getPaddingLeft();
        final int container_top = binding.fragmentContainer.getPaddingTop();
        final int container_right = binding.fragmentContainer.getPaddingRight();
        final int container_bottom = binding.fragmentContainer.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.fragmentContainer,
                (view, window_insets) -> {
                    Insets insets = window_insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            container_left + insets.left,
                            container_top,
                            container_right + insets.right,
                            container_bottom);
                    return window_insets;
                });

        final int nav_left = binding.bottomNavigation.getPaddingLeft();
        final int nav_top = binding.bottomNavigation.getPaddingTop();
        final int nav_right = binding.bottomNavigation.getPaddingRight();
        final int nav_bottom = binding.bottomNavigation.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.bottomNavigation,
                (view, window_insets) -> {
                    Insets insets = window_insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            nav_left + insets.left,
                            nav_top,
                            nav_right + insets.right,
                            nav_bottom + insets.bottom);
                    return window_insets;
                });

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out_state) {
        out_state.putInt(STATE_SELECTED_TAB, selected_tab);
        super.onSaveInstanceState(out_state);
    }
}
