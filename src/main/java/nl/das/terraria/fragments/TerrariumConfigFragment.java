package nl.das.terraria.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.json.Properties;

public class TerrariumConfigFragment extends Fragment {

    private static final int TERRARIUM_NR = 1;

    private int terrNr;
    private Properties props;
    private InputMethodManager imm;

    private EditText edtName;
    private EditText edtHost;
    private EditText edtUuid;
    private EditText edtIp;
    private Button btnSave;

    private TerrariumConfigFragment() { }

    public static TerrariumConfigFragment newInstance(int tnr) {
        TerrariumConfigFragment fragment = new TerrariumConfigFragment();
        Bundle args = new Bundle();
        args.putInt("TerrariumNr", tnr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            terrNr = getArguments().getInt("TerrariumNr");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "TerrariumConfigFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.fragment_terrarium_config, container, false);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Utils.log('i', "TerrariumConfigFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "TerrariumConfigFragment: onViewCreated() start");
        btnSave = view.findViewById(R.id.cfg_btnSave);
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> {
            btnSave.requestFocusFromTouch();
            if (propsCheck()) {
                TerrariaApp.configs.put(terrNr, props);
                TerrariaApp.instance.saveConfig();
            }
            imm.hideSoftInputFromWindow(btnSave.getWindowToken(), 0);
            btnSave.setEnabled(false);
        });
        edtName = view.findViewById(R.id.cfg_edtTerrName);
        edtName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtName.getWindowToken(), 0);
                edtHost.requestFocus();
                btnSave.setEnabled(true);
            }
            return false;
        });
        edtHost = view.findViewById(R.id.cfg_edtHostName);
        edtHost.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtHost.getWindowToken(), 0);
                edtUuid.requestFocus();
                btnSave.setEnabled(true);
            }
            return false;
        });
        edtUuid = view.findViewById(R.id.cfg_edtUUID);
        edtUuid.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtUuid.getWindowToken(), 0);
                edtIp.requestFocus();
                btnSave.setEnabled(true);
            }
            return false;
        });
        Pattern pattern = Pattern.compile("([0-9A-F]|-)");
        InputFilter inputFilter_uuid = (source, start, end, dest, dstart, dend) -> {
            // 2D266186-01FB-47C2-8D9F-10B8EC891363
            Utils.log('i', source.toString() + " " + start + ".." + end + " " + dest.toString() + " " + dstart + ".." + dend);
            if (dest.length()>=36) return ""; // max 32 hex digits + 4 '-'-chars
            if (end > 0) {
                Matcher matcher = pattern.matcher(String.valueOf(source.charAt(end - 1)));
                if (!matcher.matches()) {
                    if ((end - start) > 1) {
                        return dest;
                    } else {
                        return "";
                    }
                }
                if (dest.length() == 8 || dest.length() == 13 || dest.length() == 18 || dest.length() == 23) {
                    if ((end - start) > 1) {
                        return dest + "-" + source.charAt(end - 1);
                    } else {
                        return "-" + source.charAt(end - 1);
                    }
                }
            }
            return null;
        };
        edtUuid.setFilters(new InputFilter[] { inputFilter_uuid });
        edtUuid.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        edtIp = view.findViewById(R.id.cfg_edtIPAddress);
        edtIp.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtIp.getWindowToken(), 0);
                btnSave.requestFocus();
                btnSave.setEnabled(true);
            }
            return false;
        });
        props = TerrariaApp.configs.get(terrNr);
        if (props != null) {
            edtName.setText(props.getTcuName());
            edtHost.setText(props.getDeviceName());
            edtUuid.setText(props.getUuid());
            edtIp.setText(props.getIp());
        }
        Utils.log('i', "TerrariumConfigFragment: onViewCreated() end");
    }

    private boolean propsCheck() {
        boolean res = true;
        props = new Properties();
        String name = edtName.getText().toString();
        props.setTcuName(name);
        String host = edtHost.getText().toString();
        props.setDeviceName(host);
        String uuid = edtUuid.getText().toString();
        props.setUuid(uuid);
        String ip = edtIp.getText().toString();
        props.setIp(ip);
        return res;
    }


}