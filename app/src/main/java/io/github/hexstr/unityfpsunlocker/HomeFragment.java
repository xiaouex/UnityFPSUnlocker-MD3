package io.github.hexstr.UnityFPSUnlocker;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import io.github.hexstr.UnityFPSUnlocker.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private UnityGameScanner scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable save_runnable = () -> commitGlobal(false);
    private boolean binding_values;

    private final Prefs.ActivationListener activation_listener = this::renderStatus;

    private final UnityGameScanner.Listener scan_listener =
            (state, games) -> {
                if (binding != null) {
                    renderScanState(state, games.size());
                }
            };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindValues();
        bindListeners();

        Prefs.addActivationListener(activation_listener);
        renderStatus(Prefs.isXposedActive());

        UnityGameScanner scanner = UnityGameScanner.get(requireContext());
        this.scanner = scanner;
        scanner.addListener(scan_listener);
        scanner.startScan();
        renderScanState(scanner.getState(), scanner.getGames().size());
    }

    private void bindValues() {
        binding_values = true;
        binding.fpsInput.setText(String.valueOf(ModuleSettings.getGlobalFps(requireContext())));
        binding.resolutionInput.setText(
                String.valueOf(ModuleSettings.getGlobalScale(requireContext())));
        binding.delayInput.setText(String.valueOf(ModuleSettings.getGlobalDelay(requireContext())));
        binding.opcodeSwitch.setChecked(ModuleSettings.getGlobalModOpcode(requireContext()));
        binding_values = false;
    }

    private void bindListeners() {
        binding.fpsInput.addTextChangedListener(new InputFields.AfterTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!binding_values) {
                    scheduleSave();
                }
            }
        });
        binding.resolutionInput.addTextChangedListener(new InputFields.AfterTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!binding_values) {
                    scheduleSave();
                }
            }
        });
        binding.delayInput.addTextChangedListener(new InputFields.AfterTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!binding_values) {
                    scheduleSave();
                }
            }
        });

        View.OnFocusChangeListener focus_listener =
                (field, has_focus) -> {
                    if (!has_focus) {
                        commitGlobal(true);
                    }
                };
        binding.fpsInput.setOnFocusChangeListener(focus_listener);
        binding.resolutionInput.setOnFocusChangeListener(focus_listener);
        binding.delayInput.setOnFocusChangeListener(focus_listener);

        binding.delayInput.setOnEditorActionListener((field, action_id, event) -> {
            if (action_id == EditorInfo.IME_ACTION_DONE) {
                commitGlobal(true);
                field.clearFocus();
                return true;
            }
            return false;
        });

        binding.opcodeSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!binding_values) {
                ModuleSettings.setGlobalModOpcode(requireContext(), checked);
            }
        });
    }

    private void scheduleSave() {
        handler.removeCallbacks(save_runnable);
        handler.postDelayed(save_runnable, 450);
    }

    private void commitGlobal(boolean show_errors) {
        if (binding == null || binding_values) {
            return;
        }
        handler.removeCallbacks(save_runnable);

        Integer fps = InputFields.parsePositiveInt(InputFields.textOf(binding.fpsInput));
        if (fps == null) {
            if (show_errors) {
                binding.fpsLayout.setError(getString(R.string.error_fps));
            }
        } else {
            binding.fpsLayout.setError(null);
            ModuleSettings.setGlobalFps(requireContext(), fps);
        }

        Float scale = InputFields.parseScale(InputFields.textOf(binding.resolutionInput));
        if (scale == null) {
            if (show_errors) {
                binding.resolutionLayout.setError(getString(R.string.error_resolution));
            }
        } else {
            binding.resolutionLayout.setError(null);
            ModuleSettings.setGlobalScale(requireContext(), scale);
        }

        Integer delay = InputFields.parseNonNegativeInt(InputFields.textOf(binding.delayInput));
        if (delay == null) {
            if (show_errors) {
                binding.delayLayout.setError(getString(R.string.error_delay));
            }
        } else {
            binding.delayLayout.setError(null);
            ModuleSettings.setGlobalDelay(requireContext(), delay);
        }
    }

    private void renderStatus(boolean active) {
        if (binding == null) {
            return;
        }

        binding.statusTitle.setText(active ? R.string.status_active : R.string.status_inactive);
        binding.statusDescription.setText(
                active ? R.string.status_active_desc : R.string.status_inactive_desc);
        binding.statusIcon.setImageResource(
                active ? R.drawable.ic_check_circle : R.drawable.ic_warning);
        binding.statusIcon.setContentDescription(
                getString(active ? R.string.status_active : R.string.status_inactive));

        int foreground_attribute = active
                ? com.google.android.material.R.attr.colorOnPrimaryContainer
                : com.google.android.material.R.attr.colorOnErrorContainer;
        int background_attribute = active
                ? com.google.android.material.R.attr.colorPrimaryContainer
                : com.google.android.material.R.attr.colorErrorContainer;
        int foreground = MaterialColors.getColor(binding.statusIcon, foreground_attribute);
        int background = MaterialColors.getColor(binding.statusIconContainer, background_attribute);

        ImageViewCompat.setImageTintList(
                binding.statusIcon,
                ColorStateList.valueOf(foreground));
        binding.statusIconContainer.setCardBackgroundColor(background);
    }

    private void renderScanState(UnityGameScanner.State state, int game_count) {
        if (binding == null) {
            return;
        }
        if (state == UnityGameScanner.State.READY) {
            binding.scanProgress.setVisibility(View.GONE);
            binding.gameCount.setText(String.valueOf(game_count));
            binding.gameCountUnit.setText(R.string.game_count_unit);
        } else if (state == UnityGameScanner.State.INACTIVE) {
            binding.scanProgress.setVisibility(View.GONE);
            binding.gameCount.setText(R.string.value_placeholder);
            binding.gameCountUnit.setText(R.string.scope_inactive_short);
        } else if (state == UnityGameScanner.State.EMPTY_SCOPE) {
            binding.scanProgress.setVisibility(View.GONE);
            binding.gameCount.setText(R.string.value_placeholder);
            binding.gameCountUnit.setText(R.string.scope_empty_short);
        } else if (state == UnityGameScanner.State.ERROR) {
            binding.scanProgress.setVisibility(View.GONE);
            binding.gameCount.setText(R.string.value_placeholder);
            binding.gameCountUnit.setText(R.string.scan_failed_short);
        } else {
            binding.scanProgress.setVisibility(View.VISIBLE);
            binding.gameCount.setText(R.string.value_placeholder);
            binding.gameCountUnit.setText(R.string.scanning_short);
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            commitGlobal(true);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(save_runnable);
        Prefs.removeActivationListener(activation_listener);
        if (scanner != null) {
            scanner.removeListener(scan_listener);
        }
        binding = null;
        super.onDestroyView();
    }
}
