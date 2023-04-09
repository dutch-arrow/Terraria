package nl.das.terraria.fragments;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;

public class ConfigurationFragment extends Fragment {

    private InputMethodManager imm;
    private LinearLayout terrariaLayout;
    private Button btnCurrent;
    private int nrOfTcus;

    private ConfigurationFragment() {
    }

    public static ConfigurationFragment newInstance(int nrOfTcus) {
        ConfigurationFragment fragment = new ConfigurationFragment();
        Bundle args = new Bundle();
        args.putInt("nrOfTcus", nrOfTcus);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.log('i', "ConfigurationFragment: onCreate() start");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nrOfTcus = getArguments().getInt("nrOfTcus");
        }
        Utils.log('i', "ConfigurationFragment: onCreate() end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "ConfigurationFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.fragment_configuration, container, false);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        int backcolorSel = getResources().getColor(R.color.colorPrimaryDark, null);
        int backcolor = getResources().getColor(R.color.notActive, null);
        terrariaLayout = view.findViewById(R.id.terrariaButtons);
        for (int t = 1; t <= 10; t++) {
            View v = inflater.inflate(R.layout.dynamic_button, container, false);
            Button b = v.findViewById(R.id.dyn_button_id);
            b.setText("Terrarium " + t);
            b.setVisibility(View.INVISIBLE);
            int finalT = t;
            b.setOnClickListener(bv -> {
                Button btn = (Button) bv;
                btn.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolorSel, PorterDuff.Mode.SRC));
                btn.setTextColor(getResources().getColor(R.color.white, null));
                if( btnCurrent != null) {
                    btnCurrent.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolor, PorterDuff.Mode.SRC));
                    btnCurrent.setTextColor(getResources().getColor(R.color.black, null));
                }
                btnCurrent = btn;
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.replace(R.id.cfg_terrarium, TerrariumConfigFragment.newInstance(finalT));
                ft.commit();
            });
            terrariaLayout.addView(v);
        }
        Utils.log('i', "ConfigurationFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "ConfigurationFragment: onViewCreated() start");
        Button btnDone = view.findViewById(R.id.cfg_btnDone);
        btnDone.setEnabled(true);
        btnDone.setOnClickListener(v -> {
            btnDone.requestFocusFromTouch();
            imm.hideSoftInputFromWindow(btnDone.getWindowToken(), 0);
            TerrariaApp.menu.performIdentifierAction(R.id.menu_terr_item, 0);
//            getParentFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.main_layout, TerrariaFragment.newInstance(), "app")
//                    .commitNow();
        });
        EditText nrOfTerraria = view.findViewById(R.id.edtNrOfTerraria);
        nrOfTerraria.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(nrOfTerraria.getWindowToken(), 0);
            }
            return false;
        });
        nrOfTerraria.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
                    int nr = Integer.parseInt(s.toString());
                    for (int i = 1; i <= nr; i++) {
                        terrariaLayout.getChildAt(i - 1).setVisibility(View.VISIBLE);
                    }
                    for (int i = nr + 1; i <= 10; i++) {
                        terrariaLayout.getChildAt(i - 1).setVisibility(View.INVISIBLE);
                    }
                }
                terrariaLayout.getChildAt(0).callOnClick();
            }
        });
        if (nrOfTcus > 0) {
            nrOfTerraria.setText(nrOfTcus + "");
            for (int i = 1; i <= nrOfTcus; i++) {
                terrariaLayout.getChildAt(i - 1).setVisibility(View.VISIBLE);
            }
            for (int i = nrOfTcus + 1; i <= 10; i++) {
                terrariaLayout.getChildAt(i - 1).setVisibility(View.INVISIBLE);
            }
            terrariaLayout.getChildAt(0).callOnClick();
        }
        Utils.log('i', "ConfigurationFragment: onViewCreated() end");
    }
}