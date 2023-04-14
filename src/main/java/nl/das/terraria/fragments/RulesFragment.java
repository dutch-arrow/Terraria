package nl.das.terraria.fragments;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.json.Device;

public class RulesFragment extends Fragment {

    private int tcunr;

    private LinearLayout rulesLayout;
    private Button btnCurrent;

    public RulesFragment() {
        // Required empty public constructor
    }

    public static RulesFragment newInstance(int tcunr) {
        Utils.log('i', "RulesFragment: newInstance() start");
        RulesFragment fragment = new RulesFragment();
        Bundle args = new Bundle();
        args.putInt("tcunr", tcunr);
        fragment.setArguments(args);
        Utils.log('i', "RulesFragment: newInstance() end");
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.log('i', "RulesFragment: onCreate() start");
        if (getArguments() != null) {
            tcunr = getArguments().getInt("tcunr");
        }
        Utils.log('i', "RulesFragment: onCreate() end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "RulesFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.fragment_rules, container, false);
        int backcolorSel = getResources().getColor(R.color.colorPrimaryDark, null);
        int backcolor = getResources().getColor(R.color.notActive, null);
        int nrrs = 5;
        rulesLayout = view.findViewById(R.id.rulesButtons);
        for (int rs = 0; rs < nrrs; rs++) {
            int rsnr = rs;
            View v = inflater.inflate(R.layout.dynamic_button, container, false);
            Button btnRs = v.findViewById(R.id.dyn_button_id);
            btnRs.setOnClickListener(bv1 -> {
                Button btn = (Button) bv1;
                btn.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolorSel, PorterDuff.Mode.SRC));
                btn.setTextColor(getResources().getColor(R.color.white, null));
                if (btnCurrent != null) {
                    btnCurrent.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolor, PorterDuff.Mode.SRC));
                    btnCurrent.setTextColor(getResources().getColor(R.color.black, null));
                }
                btnCurrent = btn;
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.replace(R.id.rs_fcvRules, TemperatureRuleFragment.newInstance(tcunr, rsnr + 1));
                ft.commit();
            });
            rulesLayout.addView(v);
        }

        if (hasSprayer()) {
            View v = inflater.inflate(R.layout.dynamic_button, container, false);
            Button btnRs = v.findViewById(R.id.dyn_button_id);
            btnRs.setOnClickListener(dv -> {
                Button btn = (Button) dv;
                btn.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolorSel, PorterDuff.Mode.SRC));
                btn.setTextColor(getResources().getColor(R.color.white, null));
                if (btnCurrent != null) {
                    btnCurrent.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(backcolor, PorterDuff.Mode.SRC));
                    btnCurrent.setTextColor(getResources().getColor(R.color.black, null));
                }
                btnCurrent = btn;
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.replace(R.id.rs_fcvRules, SprayerRuleFragment.newInstance(tcunr));
                ft.commit();
            });
            rulesLayout.addView(v);
        }
        Utils.log('i', "RulesFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Utils.log('i', "RulesFragment: onViewCreated() start");
        int nrrs = 5;
        int vix = 0;
        int r = getResources().getIdentifier("lblTemperatureRule", "string", "nl.das.terraria");
        for (int rs = 0; rs < nrrs; rs++) {
            Button btn = (Button) rulesLayout.getChildAt(vix);
            btn.setText(getResources().getString(r) + " " + (rs + 1));
            vix++;
        }
        if (hasSprayer()) {
            r = getResources().getIdentifier("lblSprayingRules", "string", "nl.das.terraria");
            Button btn = (Button) rulesLayout.getChildAt(vix);
            btn.setText(getResources().getString(r));
        }
        View v = rulesLayout.getChildAt(0);
        Button btnRs = v.findViewById(R.id.dyn_button_id);
        btnRs.performClick();
        Utils.log('i', "RulesFragment: onViewCreated() end");
    }

    private boolean hasSprayer() {
        for (Device d : TerrariaApp.configs.get(tcunr).getDevices()) {
            if (d.getDevice().equalsIgnoreCase("sprayer")) {
                return true;
            }
        }
        return false;
    }
}