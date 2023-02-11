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

import nl.das.terraria.R;

public class OnPeriodDialogFragment extends DialogFragment  implements TextView.OnEditorActionListener {
    private String device;
    private EditText mEditText;

    public OnPeriodDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static OnPeriodDialogFragment newInstance(String device) {
        OnPeriodDialogFragment frag = new OnPeriodDialogFragment();
        Bundle args = new Bundle();
        args.putString("device", device);
        frag.setArguments(args);
        return frag;
    }

    // Interface that must be implemented by the StateFragment class
    // So that the result can be communicated back.
    public interface OnPeriodDialogListener {
        public void onSave(String device, int onPeriod);
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
                res.putInt("period", 0);
            } else {
                res.putInt("period",  Integer.parseInt(nr));
            }
            fm.setFragmentResult("period", res);
            // Close the dialog and return back to the parent activity
            dismiss();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.onperiod_dlg, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditText = (EditText) view.findViewById(R.id.edtTextOnPeriod);
        // Fetch arguments from bundle and set title
        device= getArguments().getString("device");
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);
    }
}
