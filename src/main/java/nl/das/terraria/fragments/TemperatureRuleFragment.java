package nl.das.terraria.fragments;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import nl.das.terraria.TerrariaApp;
import nl.das.terraria.json.Device;
import nl.das.terraria.json.TemperatureRule;
import nl.das.terraria.services.TcuService;
import nl.das.terraria.R;
import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Action;

public class TemperatureRuleFragment extends Fragment {

    private static final int NR_OF_ACTIONS = 4;
    private int tcunr;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();
    private Messenger tcuservice;
    private boolean bound;
    
    private InputMethodManager imm;
    private TemperatureRule rule;
    private int currentRlNr;

    private Button btnSave;
    private Button btnRefresh;
    private SwitchCompat swActive;
    private EditText edtFrom;
    private EditText edtTo;
    private EditText edtIdeal;
    private EditText edtThreshold;
    private EditText edtDelay;
    private Spinner spnAboveBelow;
    private List<String> spn_items;
    private List<String> spn_devices;
    private LinearLayout actionsLayout;
    private View action;
    private WaitSpinner wait;
    private final boolean[] userSelect = new boolean[5];

    public TemperatureRuleFragment() {
        supportedMessages.add(TcuService.CMD_GET_RULE);
        supportedMessages.add(TcuService.CMD_SET_RULE);
    }

    public static TemperatureRuleFragment newInstance(int tcunr, int rulenr) {
        Utils.log('i', "TemperatureRuleFragment: newInstance() start");
        TemperatureRuleFragment fragment = new TemperatureRuleFragment();
        Bundle args = new Bundle();
        args.putInt("tcunr", tcunr);
        args.putInt("rulenr", rulenr);
        fragment.setArguments(args);
        Utils.log('i', "TemperatureRuleFragment: newInstance() end. TCUnr=" + tcunr + " rulenr=" + rulenr);
        return fragment;
    }
    /**
     * Service connection that connects to the TcuService.
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            tcuservice = new Messenger(service);
            try {
                Message msg = Message.obtain(null, TcuService.MSG_REGISTER_CLIENT);
                Bundle bdl = new Bundle();
                bdl.putIntegerArrayList("commands", supportedMessages);
                msg.setData(bdl);
                msg.replyTo = messenger;
                tcuservice.send(msg);
                bound = true;
                wait.start();
                getRule();
            } catch (RemoteException e) {
                bound = false;
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            tcuservice = null;
            bound = false;
        }
    };

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger messenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from service.
     */
    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"TemperatureRuleFragment: handleMessage() for message " + msg.what );
            switch (msg.what) {
                case TcuService.CMD_GET_RULE:
                    Utils.log('i', "TemperatureRuleFragment: " + msg.obj.toString());
                    rule = new Gson().fromJson(msg.obj.toString(), TemperatureRule.class);
                    updateRule();
                    wait.dismiss();
                    break;
                case TcuService.CMD_SET_RULE:
                    if (msg.obj != null && msg.obj.toString().length() > 2) {
                        String errmsg;
                        Error err = new Gson().fromJson(msg.obj.toString(), Error.class);
                        if (err != null) {
                            errmsg = err.getError();
                        } else {
                            errmsg = msg.obj.toString();
                        }
                        Utils.showMessage(requireContext(), requireView(), errmsg);
                    } else {
                        Utils.log('i', "TemperatureRuleFragment: No response");
                    }
                    wait.dismiss();
                    break;
                default:
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.log('i', "TemperatureRuleFragment: onCreate() start");
        if (getArguments() != null) {
            tcunr = getArguments().getInt("tcunr");
            currentRlNr = getArguments().getInt("rulenr");
        }
        Utils.log('i', "TemperatureRuleFragment: onCreate() end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "TemperatureRuleFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.temperaturerule_frg, container, false);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        btnSave = view.findViewById(R.id.tr_btnSave);
        btnSave.setOnClickListener(v -> {
            btnSave.requestFocusFromTouch();
            imm.hideSoftInputFromWindow(btnSave.getWindowToken(), 0);
            wait.start();
            saveRule();
            btnSave.setEnabled(false);
        });
        btnRefresh = view.findViewById(R.id.tr_btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            imm.hideSoftInputFromWindow(btnRefresh.getWindowToken(), 0);
            wait.start();
            getRule();
            btnSave.setEnabled(false);
        });
        swActive = view.findViewById(R.id.tr_swActive);
        swActive.setOnClickListener(v -> {
            if (swActive.isChecked()) {
                rule.setActive("yes");
            } else {
                rule.setActive("no");
            }
            btnSave.setEnabled(true);
        });
        edtFrom = view.findViewById(R.id.tr_edtFrom);
        edtFrom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtFrom.getWindowToken(), 0);
                if (checkTime(edtFrom)) {
                    String value = String.valueOf(edtFrom.getText()).trim();
                    rule.setFrom(value);
                    edtTo.requestFocus();
                    btnSave.setEnabled(true);
                }
            }
            return false;
        });
        edtFrom.setOnFocusChangeListener(((v, hasFocus) -> {
            if (!hasFocus) {
                if (checkTime(edtFrom)) {
                    String value = String.valueOf(edtFrom.getText()).trim();
                    rule.setFrom(value);
                    btnSave.setEnabled(true);
                }
            }
        }));
        edtTo = view.findViewById(R.id.tr_edtTo);
        edtTo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtTo.getWindowToken(), 0);
                if (checkTime(edtTo)) {
                    String value = String.valueOf(edtTo.getText()).trim();
                    rule.setTo(value);
                    edtIdeal.requestFocus();
                    btnSave.setEnabled(true);
                }
            }
            return false;
        });
        edtTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (checkTime(edtTo)) {
                    String value = String.valueOf(edtTo.getText()).trim();
                    rule.setTo(value);
                    btnSave.setEnabled(true);
                }
            }
        });
        edtIdeal = view.findViewById(R.id.tr_edtIdeal);
        edtIdeal.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtIdeal.getWindowToken(), 0);
                String value = String.valueOf(edtIdeal.getText()).trim();
                if (checkInteger(edtIdeal, value, 15, 40)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setTempIdeal(intval);
                    btnSave.setEnabled(true);
                }
            }
            return false;
        });
        edtIdeal.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtIdeal.getText()).trim();
                if (checkInteger(edtIdeal, value, 15, 40)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setTempIdeal(intval);
                    btnSave.setEnabled(true);
                }
            }
        });
        edtDelay = view.findViewById(R.id.tr_edtDelay);
        edtDelay.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtDelay.getWindowToken(), 0);
                String value = String.valueOf(edtDelay.getText()).trim();
                if (checkInteger(edtDelay, value, 1, 59)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setDelay(intval);
                    btnSave.setEnabled(true);
                }
            }
            return false;
        });
        edtDelay.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtDelay.getText()).trim();
                if (checkInteger(edtDelay, value, 1, 59)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setDelay(intval);
                    btnSave.setEnabled(true);
                }
            }
        });

        spnAboveBelow = view.findViewById(R.id.tr_spnAboveBelow);
        ArrayAdapter<CharSequence> abAdap = ArrayAdapter.createFromResource(
                requireContext(), R.array.tr_ab, R.layout.spinner_list);
        abAdap.setDropDownViewResource(R.layout.spinner_list);
        spnAboveBelow.setAdapter(abAdap);

        edtThreshold = view.findViewById(R.id.tr_edtThreshold);
        edtThreshold.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imm.hideSoftInputFromWindow(edtThreshold.getWindowToken(), 0);
                String value = String.valueOf(edtThreshold.getText()).trim();
                if (checkInteger(edtThreshold, value, 15, 40)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setTempThreshold(-intval);
                    btnSave.setEnabled(true);
                }
            }
            return false;
        });
        edtThreshold.setOnFocusChangeListener(((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtThreshold.getText()).trim();
                if (checkInteger(edtThreshold, value, 15, 40)) {
                    int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                    rule.setTempThreshold(-intval);
                    btnSave.setEnabled(true);
                }
            }
        }));

        spn_items = new ArrayList<>();
        spn_devices = new ArrayList<>();
        spn_items.add("");
        for (Device d :  Objects.requireNonNull(TerrariaApp.configs.get(tcunr)).getDevices()) {
            if (!d.getDevice().startsWith("light") && !d.getDevice().equalsIgnoreCase("uvlight")) {
                spn_devices.add(d.getDevice());
                int r = getResources().getIdentifier(d.getDevice(), "string", "nl.das.terraria");
                spn_items.add(getResources().getString(r));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_list, R.id.tr_spnItem, spn_items.toArray(new String[0]));
        actionsLayout = view.findViewById(R.id.tr_layActions);
        final Integer[] lastPos = {-1, -1, -1, -1, -1};
        for (int a = 0; a < NR_OF_ACTIONS; a++) {
            action = inflater.inflate(R.layout.rule_action_frg, container, false);
            actionsLayout.addView(action, a);
            RadioGroup rbgPeriod = action.findViewById((R.id.tr_rbgPeriod));
            RadioButton rbnActionIdeal = action.findViewById((R.id.tr_rbIdeal));
            RadioButton rbnActionPeriod = action.findViewById((R.id.tr_rbPeriod));
            EditText edtActionPeriod = action.findViewById((R.id.tr_edtPeriod));
            TextView lblSeconds = action.findViewById((R.id.tr_lblSeconds));
            Spinner spnDevice = action.findViewById(R.id.tr_spnDevice);

            int anr = a;
            spnDevice.setAdapter(adapter);
            spnDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (lastPos[anr] != -1) { // init
                        if (lastPos[anr] != position) {
                            if (position == 0) { // "no device"
                                rbgPeriod.setEnabled(false);
                                rbnActionIdeal.setEnabled(false);
                                rbnActionIdeal.setChecked(false);
                                edtActionPeriod.setText("");
                                rbnActionPeriod.setEnabled(false);
                                rbnActionPeriod.setChecked(false);
                                edtActionPeriod.setEnabled(false);
                                rule.getActions().get(anr).setDevice("no device");
                                rule.getActions().get(anr).setOnPeriod(0);
                                lblSeconds.setTextColor(getResources().getColor(R.color.disabled, null));
                                if (userSelect[anr]) {
                                    btnSave.setEnabled(true);
                                } else {
                                    userSelect[anr] = true;
                                 }
                                lastPos[anr] = position;
                            } else if (position > 0) {
                                rbgPeriod.setEnabled(true);
                                rbnActionIdeal.setEnabled(true);
                                rbnActionPeriod.setEnabled(true);
                                if (rbnActionPeriod.isChecked()) {
                                    edtActionPeriod.setEnabled(true);
                                }
                                rule.getActions().get(anr).setDevice(spn_devices.get(position - 1));
                                lblSeconds.setTextColor(getResources().getColor(R.color.black, null));
                                if (userSelect[anr]) {
                                    btnSave.setEnabled(true);
                                } else {
                                    userSelect[anr] = true;
                                }
                                lastPos[anr] = position;
                            }
                        }
                    } else {
                        lastPos[anr] = position;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            rbnActionIdeal.setOnClickListener(v -> {
                rule.getActions().get(anr).setOnPeriod(-2);
                edtActionPeriod.setText("");
                edtActionPeriod.setEnabled(false);
                rbnActionPeriod.setChecked(false);
                rbnActionIdeal.setChecked(true);
                btnSave.setEnabled(true);
            });

            rbnActionPeriod.setOnClickListener(v -> {
                edtActionPeriod.setEnabled(true);
                rbnActionIdeal.setChecked(false);
                btnSave.setEnabled(true);
            });

            edtActionPeriod.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(edtActionPeriod.getWindowToken(), 0);
                    String value = String.valueOf(edtActionPeriod.getText()).trim();
                    if (checkInteger(edtActionPeriod, value, 0, 3600)) {
                        if (value.trim().length() > 0) {
                            int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                            rule.getActions().get(anr).setOnPeriod(intval);
                        }
                        spnDevice.requestFocus();
                        btnSave.setEnabled(true);
                    }
                }
                return false;
            });
            edtActionPeriod.setOnFocusChangeListener(((v, hasFocus) -> {
                if (!hasFocus) {
                    String value = String.valueOf(edtActionPeriod.getText()).trim();
                    if (checkInteger(edtActionPeriod, value, 0, 3600)) {
                        if (value.trim().length() > 0) {
                            int intval = value.trim().length() == 0 ? 0 : Integer.parseInt(value);
                            rule.getActions().get(anr).setOnPeriod(intval);
                        }
                        btnSave.setEnabled(true);
                    }
                }
            }));
        }
        Utils.log('i', "TemperatureRuleFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Utils.log('i', "TemperatureRuleFragment: onViewCreated() start");
        wait = new WaitSpinner(requireActivity());
        // Bind to TcuService
        Intent intent = new Intent(requireContext(), TcuService.class);
        if(!requireContext().bindService(intent, connection, 0)) {
            Utils.log('e',"TemperatureRuleFragment: Could not bind to TcuService");
        }
//        getRule();
        Utils.log('i', "TemperatureRuleFragment: onViewCreated() end");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.log('i', "TemperatureRuleFragment: onDestroy() start");
        if (tcuservice != null) {
            super.onDestroy();
            try {
                Message msg = Message.obtain(null, TcuService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = messenger;
                tcuservice.send(msg);
            } catch (RemoteException ignored) {
            }
            requireContext().unbindService(connection);
            Utils.log('i', "TemperatureRuleFragment: onDestroy() end");
        } else {
            Utils.log('i', "TemperatureRuleFragment: why is onDestroy() called?");
        }
    }

    private void getRule() {
        Utils.log('i', "TemperatureRuleFragment: getRule() start");
        if (TerrariaFragment.MOCK[tcunr]) {
            Utils.log('i', "TemperatureRuleFragment: getRule() from file (mock)");
            try {
                Gson gson = new Gson();
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("rules" + currentRlNr + "_" + Objects.requireNonNull(TerrariaApp.configs.get(tcunr)).getMockPostfix() + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                rule = gson.fromJson(response, TemperatureRule.class);
                wait.dismiss();
            } catch (IOException e) {
                wait.dismiss();
            }
        } else {
            if (tcuservice != null) {
                Utils.log('i', "TemperatureRuleFragment: getRule() from server");
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_RULE);
                    msg.replyTo = messenger;
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    JsonObject d = new JsonObject();
                    d.addProperty("rulenr", currentRlNr);
                    data.putString("json", new Gson().toJson(d));
                    msg.setData(data);
                    tcuservice.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            } else {
                Utils.log('i', "TemperatureRuleFragment: TcuService is not ready yet");
            }
        }
        Utils.log('i', "TemperatureRuleFragment: getRule() end");
    }

    private void updateRule() {
        Utils.log('i', "TemperatureRuleFragment: updateRule() start");
        swActive.setChecked(rule.getActive().equalsIgnoreCase("yes"));
        edtFrom.setText(rule.getFrom());
        edtTo.setText(rule.getTo());
        edtIdeal.setText(rule.getTempIdeal() + "");
        edtDelay.setText(rule.getDelay() / 60 + "");
        int value = rule.getTempThreshold();
        if (value < 0) {
            edtThreshold.setText(String.valueOf(-value));
            spnAboveBelow.setSelection(1);
        } else if (value > 0) {
            edtThreshold.setText(String.valueOf(value));
            spnAboveBelow.setSelection(0);
        }

        userSelect[0] = userSelect[1] = userSelect[2]= userSelect[3] = userSelect[4] = false;
        for (int i = 0; i < NR_OF_ACTIONS; i++) {
            action = actionsLayout.getChildAt(i);
            Action a = rule.getActions().get(i);
            int ix = 0;
            Spinner spnDevice = action.findViewById(R.id.tr_spnDevice);
            if (!a.getDevice().equalsIgnoreCase("no device")) {
                int r = getResources().getIdentifier(a.getDevice(), "string", "nl.das.terraria");
                ix = spn_items.indexOf(getResources().getString(r));
            }
            spnDevice.setSelection(ix);
            RadioButton rbnActionIdeal = action.findViewById((R.id.tr_rbIdeal));
            RadioButton rbnActionPeriod = action.findViewById((R.id.tr_rbPeriod));
            EditText edtActionPeriod = action.findViewById((R.id.tr_edtPeriod));
            TextView lblSeconds = action.findViewById((R.id.tr_lblSeconds));
            if (a.getOnPeriod() < 0) {
                rbnActionIdeal.setChecked(true);
                rbnActionPeriod.setChecked(false);
                edtActionPeriod.setEnabled(false);
                lblSeconds.setTextColor(getResources().getColor(R.color.disabled, null));
            } else if (a.getOnPeriod() > 0) {
                rbnActionPeriod.setChecked(true);
                rbnActionIdeal.setChecked(false);
                edtActionPeriod.setText(String.valueOf(a.getOnPeriod()));
                edtActionPeriod.setEnabled(true);
                lblSeconds.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                rbnActionIdeal.setChecked(false);
                rbnActionIdeal.setEnabled(false);
                rbnActionPeriod.setChecked(false);
                rbnActionPeriod.setEnabled(false);
                edtActionPeriod.setText("");
                edtActionPeriod.setEnabled(false);
                lblSeconds.setTextColor(getResources().getColor(R.color.disabled, null));
            }
        }
        btnSave.setEnabled(false);
        Utils.log('i', "TemperatureRuleFragment: updateRule() end");
    }

    private void saveRule() {
        Utils.log('i', "TemperatureRuleFragment: saveRuleset() start");
        if (tcuservice != null) {
            try {
                Utils.log('i', "TemperatureRuleFragment: saveRuleset()");
                Message msg = Message.obtain(null, TcuService.CMD_SET_RULE);
                msg.replyTo = messenger;
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new JsonObject();
                d.addProperty("rulenr", currentRlNr);
                rule.setFrom(rule.getFrom().replace(".", ":"));
                rule.setTo(rule.getTo().replace(".", ":"));
                JsonObject jsObj = new Gson().toJsonTree(rule).getAsJsonObject();
                d.add("rule", jsObj);
                data.putString("json", new Gson().toJson(d));
                Utils.log('i', data.getString("json"));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "TemperatureRuleFragment: TcuService is not ready yet");
        }
        Utils.log('i', "TemperatureRuleFragment: saveRuleset() end");
    }

    private boolean checkTime(EditText field) {
        String value = String.valueOf(field.getText()).trim();
        if (value.length() != 0) {
            String[] parts = value.split("\\.");
            if (parts.length == 2) {
                try {
                    int hr = Integer.parseInt(parts[0].trim());
                    if (hr < 0 || hr > 23) {
                        field.setError("Uuropgave moet tussen 0 en 23 zijn");
                    }
                } catch (NumberFormatException e) {
                    field.setError("Uuropgave is geen getal");
                }
                try {
                    int min = Integer.parseInt(parts[1].trim());
                    if (min < 0 || min > 59) {
                        field.setError("Minutenopgave moet tussen 0 en 59 zijn");
                    }
                } catch (NumberFormatException e) {
                    field.setError("Minutenopgave is geen getal");
                }
            } else {
                field.setError("Tijdopgave is niet juist. Formaat: hh.mm");
            }
        }
        return field.getError() == null;
    }

    private boolean checkInteger(EditText field, String value, int minValue, int maxValue) {
        if (value.trim().length() > 0) {
            try {
                int rv = Integer.parseInt(value);
                if (rv < minValue || rv > maxValue) {
                    field.setError("Waarde moet tussen " + minValue + " en " + maxValue + " zijn.");
                }
            } catch (NumberFormatException e) {
                field.setError("Waarde moet tussen " + minValue + " en " + maxValue + " zijn.");
            }
        }
        return field.getError() == null;
    }

}