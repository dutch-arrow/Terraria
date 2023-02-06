package nl.das.terraria.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

import nl.das.terraria.R;

public class ResetHoursDialogFragment extends DialogFragment  implements TextView.OnEditorActionListener {
    private EditText mEditText;

    public ResetHoursDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static ResetHoursDialogFragment newInstance(String device) {
        ResetHoursDialogFragment frag = new ResetHoursDialogFragment();
        Bundle args = new Bundle();
        args.putString("device", device);
        frag.setArguments(args);
        return frag;
    }

    // Fires whenever the textfield has an action performed
    // In this case, when the "Done" button is pressed
    // REQUIRES a 'soft keyboard' (virtual keyboard)
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            // Return input text back to activity through the implemented listener
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            String nr = mEditText.getText().toString();
            Bundle res = new Bundle();
            if (nr.isEmpty()) {
                res.putInt("hours", 0);
            } else {
                res.putInt("hours",  Integer.parseInt(nr));
            }
            fm.setFragmentResult("reset", res);
            // Close the dialog and return back to the parent activity
            dismiss();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.reset_hours_dlg, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditText = view.findViewById(R.id.edtResetHours);
        // Fetch arguments from bundle and set title
        String device = requireArguments().getString("device");
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();
        Objects.requireNonNull(getDialog()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);
    }
}
