package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.hexstr.UnityFPSUnlocker.databinding.ItemGameBinding;

final class GameAdapter extends ListAdapter<UnityGameScanner.Game, GameAdapter.GameViewHolder> {
    private static final DiffUtil.ItemCallback<UnityGameScanner.Game> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull UnityGameScanner.Game old_item,
                        @NonNull UnityGameScanner.Game new_item) {
                    return old_item.packageName.equals(new_item.packageName);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull UnityGameScanner.Game old_item,
                        @NonNull UnityGameScanner.Game new_item) {
                    return old_item.label.equals(new_item.label);
                }
            };

    GameAdapter() {
        super(DIFF);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).packageName.hashCode();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGameBinding binding = ItemGameBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new GameViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public void onViewRecycled(@NonNull GameViewHolder holder) {
        holder.recycle();
        super.onViewRecycled(holder);
    }

    void refreshValues() {
        if (getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    static final class GameViewHolder extends RecyclerView.ViewHolder {
        private final ItemGameBinding binding;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable save_runnable = () -> commit(false);

        private UnityGameScanner.Game game;
        private boolean binding_values;

        GameViewHolder(ItemGameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.gameFpsInput.addTextChangedListener(new AfterTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    if (!binding_values) {
                        scheduleSave();
                    }
                }
            });
            binding.gameResolutionInput.addTextChangedListener(new AfterTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    if (!binding_values) {
                        scheduleSave();
                    }
                }
            });
            binding.gameDelayInput.addTextChangedListener(new AfterTextWatcher() {
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
                            commit(true);
                        }
                    };
            binding.gameFpsInput.setOnFocusChangeListener(focus_listener);
            binding.gameResolutionInput.setOnFocusChangeListener(focus_listener);
            binding.gameDelayInput.setOnFocusChangeListener(focus_listener);

            binding.gameDelayInput.setOnEditorActionListener((field, action_id, event) -> {
                if (action_id == EditorInfo.IME_ACTION_DONE) {
                    commit(true);
                    field.clearFocus();
                    hideKeyboard();
                    return true;
                }
                return false;
            });

            binding.gameOpcodeSwitch.setOnCheckedChangeListener((button, checked) -> {
                if (!binding_values && game != null) {
                    ModuleSettings.setPerAppModOpcode(
                            binding.getRoot().getContext(),
                            game.packageName,
                            checked);
                }
            });

            binding.gameCustomSwitch.setOnCheckedChangeListener((button, checked) -> {
                if (binding_values || game == null) {
                    return;
                }

                Context context = binding.getRoot().getContext();
                handler.removeCallbacks(save_runnable);
                if (checked) {
                    ModuleSettings.enablePerAppConfig(context, game.packageName);
                    binding_values = true;
                    loadConfigValues(context, game.packageName);
                    binding.gameCustomSwitch.setChecked(true);
                    binding_values = false;
                } else {
                    ModuleSettings.disablePerAppConfig(context, game.packageName);
                    binding_values = true;
                    binding.gameCustomSwitch.setChecked(false);
                    binding_values = false;
                }
                renderCustomConfig(checked);
            });
        }

        void bind(UnityGameScanner.Game game) {
            handler.removeCallbacks(save_runnable);
            this.game = game;

            Context context = binding.getRoot().getContext();
            boolean custom_enabled = ModuleSettings.hasPerAppConfig(context, game.packageName);

            binding_values = true;
            binding.gameName.setText(game.label);
            binding.gamePackage.setText(game.packageName);
            binding.gameIcon.setImageDrawable(game.icon);
            binding.gameCustomSwitch.setChecked(custom_enabled);
            if (custom_enabled) {
                loadConfigValues(context, game.packageName);
            }
            binding_values = false;

            binding.gameFpsLayout.setError(null);
            binding.gameResolutionLayout.setError(null);
            binding.gameDelayLayout.setError(null);
            renderCustomConfig(custom_enabled);
        }

        private void loadConfigValues(Context context, String package_name) {
            binding.gameFpsInput.setText(
                    String.valueOf(ModuleSettings.getPerAppFps(context, package_name)));
            binding.gameResolutionInput.setText(
                    String.valueOf(ModuleSettings.getPerAppScale(context, package_name)));
            binding.gameDelayInput.setText(
                    String.valueOf(ModuleSettings.getPerAppDelay(context, package_name)));
            binding.gameOpcodeSwitch.setChecked(
                    ModuleSettings.getPerAppModOpcode(context, package_name));
        }

        private void renderCustomConfig(boolean enabled) {
            binding.gameConfigContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
            binding.gameCustomDesc.setText(
                    enabled ? R.string.per_app_enabled_desc : R.string.per_app_disabled_desc);
        }

        void recycle() {
            commit(false);
            handler.removeCallbacks(save_runnable);
            game = null;
        }

        private void scheduleSave() {
            handler.removeCallbacks(save_runnable);
            handler.postDelayed(save_runnable, 450);
        }

        private void commit(boolean show_errors) {
            if (game == null || binding_values) {
                return;
            }
            handler.removeCallbacks(save_runnable);

            Context context = binding.getRoot().getContext();
            String package_name = game.packageName;
            if (!ModuleSettings.hasPerAppConfig(context, package_name)) {
                return;
            }

            Integer fps = parsePositiveInt(textOf(binding.gameFpsInput));
            if (fps == null) {
                if (show_errors) {
                    binding.gameFpsLayout.setError(
                            context.getString(R.string.error_fps));
                }
            } else {
                binding.gameFpsLayout.setError(null);
                ModuleSettings.setPerAppFps(context, package_name, fps);
            }

            Float scale = parseScale(textOf(binding.gameResolutionInput));
            if (scale == null) {
                if (show_errors) {
                    binding.gameResolutionLayout.setError(
                            context.getString(R.string.error_resolution));
                }
            } else {
                binding.gameResolutionLayout.setError(null);
                ModuleSettings.setPerAppScale(context, package_name, scale);
            }

            Integer delay = parseNonNegativeInt(textOf(binding.gameDelayInput));
            if (delay == null) {
                if (show_errors) {
                    binding.gameDelayLayout.setError(
                            context.getString(R.string.error_delay));
                }
            } else {
                binding.gameDelayLayout.setError(null);
                ModuleSettings.setPerAppDelay(context, package_name, delay);
            }

            ModuleSettings.setPerAppModOpcode(
                    context,
                    package_name,
                    binding.gameOpcodeSwitch.isChecked());
        }

        private void hideKeyboard() {
            InputMethodManager input_method_manager =
                    (InputMethodManager) binding.getRoot().getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (input_method_manager != null) {
                input_method_manager.hideSoftInputFromWindow(
                        binding.getRoot().getWindowToken(),
                        0);
            }
        }

        private static String textOf(android.widget.EditText field) {
            return field.getText() == null ? "" : field.getText().toString().trim();
        }

        private static Integer parsePositiveInt(String text) {
            try {
                int value = Integer.parseInt(text);
                return value > 0 ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static Integer parseNonNegativeInt(String text) {
            try {
                int value = Integer.parseInt(text);
                return value >= 0 ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static Float parseScale(String text) {
            try {
                float value = Float.parseFloat(text);
                if (!Float.isFinite(value)) {
                    return null;
                }
                return value == -1.0f || value > 0.0f ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private abstract static class AfterTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }
    }
}
