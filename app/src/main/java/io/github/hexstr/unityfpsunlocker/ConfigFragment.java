package io.github.hexstr.UnityFPSUnlocker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Collections;
import java.util.List;

import io.github.hexstr.UnityFPSUnlocker.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {
    private FragmentConfigBinding binding;
    private GameAdapter adapter;
    private UnityGameScanner scanner;

    private final UnityGameScanner.Listener scan_listener =
            (state, games) -> {
                if (binding != null) {
                    render(state, games);
                }
            };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new GameAdapter();
        binding.gameList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.gameList.setAdapter(adapter);

        UnityGameScanner scanner = UnityGameScanner.get(requireContext());
        this.scanner = scanner;
        scanner.addListener(scan_listener);
        scanner.startScan();
        render(scanner.getState(), scanner.getGames());
    }

    private void render(UnityGameScanner.State state, List<UnityGameScanner.Game> games) {
        if (binding == null) {
            return;
        }
        if (state == UnityGameScanner.State.READY && !games.isEmpty()) {
            binding.gameList.setVisibility(View.VISIBLE);
            binding.stateContainer.setVisibility(View.GONE);
            adapter.submitList(games);
            return;
        }

        binding.gameList.setVisibility(View.GONE);
        binding.stateContainer.setVisibility(View.VISIBLE);
        adapter.submitList(Collections.emptyList());
        binding.stateDescription.setVisibility(View.VISIBLE);

        if (state == UnityGameScanner.State.ERROR) {
            binding.stateProgress.setVisibility(View.GONE);
            binding.stateIcon.setImageResource(R.drawable.ic_warning);
            binding.stateIcon.setVisibility(View.VISIBLE);
            binding.stateTitle.setText(R.string.config_error);
            binding.stateDescription.setText(R.string.config_error_desc);
        } else if (state == UnityGameScanner.State.INACTIVE) {
            binding.stateProgress.setVisibility(View.GONE);
            binding.stateIcon.setImageResource(R.drawable.ic_warning);
            binding.stateIcon.setVisibility(View.VISIBLE);
            binding.stateTitle.setText(R.string.config_inactive);
            binding.stateDescription.setText(R.string.config_inactive_desc);
        } else if (state == UnityGameScanner.State.EMPTY_SCOPE) {
            binding.stateProgress.setVisibility(View.GONE);
            binding.stateIcon.setImageResource(R.drawable.ic_gamepad);
            binding.stateIcon.setVisibility(View.VISIBLE);
            binding.stateTitle.setText(R.string.config_scope_empty);
            binding.stateDescription.setText(R.string.config_scope_empty_desc);
        } else if (state == UnityGameScanner.State.READY) {
            binding.stateProgress.setVisibility(View.GONE);
            binding.stateIcon.setImageResource(R.drawable.ic_gamepad);
            binding.stateIcon.setVisibility(View.VISIBLE);
            binding.stateTitle.setText(R.string.config_empty);
            binding.stateDescription.setText(R.string.config_empty_desc);
        } else {
            binding.stateProgress.setVisibility(View.VISIBLE);
            binding.stateIcon.setVisibility(View.GONE);
            binding.stateTitle.setText(R.string.config_scanning);
            binding.stateDescription.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            View focused = binding.gameList.findFocus();
            if (focused != null) {
                focused.clearFocus();
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (scanner != null) {
            scanner.removeListener(scan_listener);
        }
        adapter = null;
        binding = null;
        super.onDestroyView();
    }
}
