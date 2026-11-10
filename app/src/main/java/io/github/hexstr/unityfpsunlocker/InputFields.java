package io.github.hexstr.UnityFPSUnlocker;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Shared text handling for the auto-saving configuration fields. Global and
 * per-app settings must validate input identically, so the rules live here.
 */
final class InputFields {
    private InputFields() {
    }

    static String textOf(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    static Integer parsePositiveInt(String text) {
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Integer parseNonNegativeInt(String text) {
        try {
            int value = Integer.parseInt(text);
            return value >= 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Float parseScale(String text) {
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

    abstract static class AfterTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }
    }
}
