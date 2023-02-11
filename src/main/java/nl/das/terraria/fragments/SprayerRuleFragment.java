package nl.das.terraria.fragments;

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
import android.util.Log;
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

import nl.das.terraria.services.TcuService;
import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Action;
import nl.das.terraria.json.SprayerRule;

public class SprayerRuleFragment extends Fragment {
    // Drying rule
    private Button btnSaveDR;
    private EditText edtDelay;
    private EditText edtFanInPeriod;
    private EditText edtFanOutPeriod;

    private InputMethodManager imm;
    private WaitSpinner wait;

    private SprayerRule dryingRule;
    private int tcunr;
    private boolean bound;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();
    private Messenger tcuservice;

    public SprayerRuleFragment() {
        supportedMessages.add(TcuService.CMD_GET_SPRAYERRULE);
        supportedMessages.add(TcuService.CMD_SET_SPRAYERRULE);
    }

    public static SprayerRuleFragment newInstance(int tabnr) {
        Utils.log('i', "SprayerRuleFragment: newInstance() start");
        SprayerRuleFragment fragment = new SprayerRuleFragment();
        Bundle args = new Bundle();
        args.putInt("tcunr", tabnr - 1);
        fragment.setArguments(args);
        Utils.log('i', "SprayerRuleFragment: newInstance() end");
        return fragment;
    }
    /**
     * Service connection that connects to the TcuService.
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            tcuservice = new Messenger(service);
            bound = true;
            try {
                Message msg = Message.obtain(null, TcuService.MSG_REGISTER_CLIENT);
                Bundle bdl = new Bundle();
                bdl.putIntegerArrayList("commands", supportedMessages);
                msg.setData(bdl);
                msg.replyTo = mMessenger;
                tcuservice.send(msg);
                wait.start();
                getDryingRule();
            } catch (RemoteException e) {
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            tcuservice = null;
            bound = false;
        }
    };

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"SprayerRuleFragment: handleMessage() for message " + msg.what );
            switch (msg.what) {
                case TcuService.CMD_GET_SPRAYERRULE:
                    Utils.log('i', "SprayerRuleFragment: " + msg.obj.toString());
                    dryingRule = new Gson().fromJson(msg.obj.toString(), SprayerRule.class);
                    updateDryingRule();
                    wait.dismiss();
                    break;
                case TcuService.CMD_SET_SPRAYERRULE:
                    if (msg.obj != null && msg.obj.toString().length() > 2) {
                        String errmsg = "";
                        Error err = new Gson().fromJson(msg.obj.toString(), Error.class);
                        if (err != null) {
                            errmsg = err.getError();
                        } else {
                            errmsg = msg.obj.toString();
                        }
                        Utils.showMessage(getContext(), requireView(), errmsg);
                    } else {
                        Utils.log('i', "SprayerRuleFragment: No response");
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
        Utils.log('i', "SprayerRuleFragment: onCreate() start");
        if (getArguments() != null) {
            tcunr = getArguments().getInt("tcunr");
        }
        Utils.log('i', "SprayerRuleFragment: onCreate() end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Utils.log('i', "SprayerRuleFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.drying_rule_frg, parent, false).getRootView();
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        btnSaveDR = view.findViewById(R.id.dr_btnSave);
        btnSaveDR.setEnabled(false);
        btnSaveDR.setOnClickListener(v -> {
            btnSaveDR.requestFocusFromTouch();
            wait.start();
            saveDryingRule();
            btnSaveDR.setEnabled(false);
        });
        Button btnRefreshDR = view.findViewById(R.id.dr_btnRefresh);
        btnRefreshDR.setEnabled(true);
        btnRefreshDR.setOnClickListener(v -> {
            wait.start();
            getDryingRule();
            btnSaveDR.setEnabled(false);
        });

        edtDelay = view.findViewById(R.id.dr_edtDelay);
        edtDelay.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = String.valueOf(edtDelay.getText()).trim();
                if (checkInteger(edtDelay, value)) {
                    dryingRule.setDelay(Integer.parseInt(value));
                    edtFanInPeriod.requestFocus();
                    btnSaveDR.setEnabled(true);
                    imm.hideSoftInputFromWindow(edtDelay.getWindowToken(), 0);
                }
            }
            return false;
        });
        edtDelay.setOnFocusChangeListener(((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtDelay.getText()).trim();
                if (checkInteger(edtDelay, value)) {
                    dryingRule.setDelay(Integer.parseInt(value));
                    btnSaveDR.setEnabled(true);
                }
            }
        }));

        edtFanInPeriod = view.findViewById(R.id.dr_edtOnPeriodIn);
        edtFanInPeriod.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = String.valueOf(edtFanInPeriod.getText()).trim();
                if (checkInteger(edtFanInPeriod, value)) {
                    dryingRule.getActions().get(0).setDevice("fan_in");
                    dryingRule.getActions().get(0).setOnPeriod(Integer.parseInt(value) * 60);
                    edtFanOutPeriod.requestFocus();
                    btnSaveDR.setEnabled(true);
                    imm.hideSoftInputFromWindow(edtFanInPeriod.getWindowToken(), 0);
                }
            }
            return false;
        });
        edtFanInPeriod.setOnFocusChangeListener(((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtFanInPeriod.getText()).trim();
                if (checkInteger(edtFanInPeriod, value)) {
                    dryingRule.getActions().get(0).setDevice("fan_in");
                    dryingRule.getActions().get(0).setOnPeriod(Integer.parseInt(value) * 60);
                    btnSaveDR.setEnabled(true);
                }
            }
        }));


        edtFanOutPeriod = view.findViewById(R.id.dr_edtOnPeriodOut);
        edtFanOutPeriod.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = String.valueOf(edtFanOutPeriod.getText()).trim();
                if (checkInteger(edtFanOutPeriod, value)) {
                    dryingRule.getActions().get(1).setDevice("fan_out");
                    dryingRule.getActions().get(1).setOnPeriod(Integer.parseInt(value) * 60);
                    btnSaveDR.setEnabled(true);
                    imm.hideSoftInputFromWindow(edtFanOutPeriod.getWindowToken(), 0);
                }
            }
            return false;
        });
        edtFanOutPeriod.setOnFocusChangeListener(((v, hasFocus) -> {
            if (!hasFocus) {
                String value = String.valueOf(edtFanOutPeriod.getText()).trim();
                if (checkInteger(edtFanOutPeriod, value)) {
                    dryingRule.getActions().get(1).setDevice("fan_out");
                    dryingRule.getActions().get(1).setOnPeriod(Integer.parseInt(value) * 60);
                    btnSaveDR.setEnabled(true);
                }
            }
        }));
        Utils.log('i', "SprayerRuleFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "SprayerRuleFragment: onViewCreated() start");
        super.onViewCreated(view, savedInstanceState);
        wait = new WaitSpinner(requireActivity());
        // Bind to TcuService
        bound = false;
        Intent intent = new Intent(getContext(), TcuService.class);
        if(!getContext().bindService(intent, connection, 0)) {
            Utils.log('i',"SprayerRuleFragment: Could not bind to TcuService");
        }
        Utils.log('i', "SprayerRuleFragment: onViewCreated() end");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.log('i', "SprayerRuleFragment: onDestroy() start");
        if (tcuservice != null) {
            super.onDestroy();
            try {
                Message msg = Message.obtain(null, TcuService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                tcuservice.send(msg);
            } catch (RemoteException e) {
            }
            getContext().unbindService(connection);
            bound = false;
        }
        Utils.log('i', "SprayerRuleFragment: onDestroy() end");
    }

    private void getDryingRule() {
        Utils.log('i', "SprayerRuleFragment: getDryingRule() start");
        if (TerrariaApp.MOCK[tcunr]) {
            Utils.log('i', "SprayerRuleFragment: getDryingRule() from file (mock)");
            try {
                Gson gson = new Gson();
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("sprayer_rule_" + TerrariaApp.configs[tcunr].getMockPostfix() + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                dryingRule = gson.fromJson(response.toString(), SprayerRule.class);
                updateDryingRule();
                wait.dismiss();
            } catch (IOException e) {
                wait.dismiss();
            }
        } else {
            if (tcuservice != null) {
                Utils.log('i', "SprayerRuleFragment: getDryingRule() from server");
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_SPRAYERRULE);
                    msg.replyTo = mMessenger;
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    msg.setData(data);
                    tcuservice.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            } else {
                Utils.log('i', "SprayerRuleFragment: TcuService is not ready yet");
            }
        }
        Utils.log('i', "SprayerRuleFragment: getDryingRule() end");
    }

    private void updateDryingRule() {
        Utils.log('i', "SprayerRuleFragment: updateDryingRule() start");
        int value = dryingRule.getDelay();
        edtDelay.setText(value + "");
        for (int i = 0; i < 4; i++) {
            Action a = dryingRule.getActions().get(i);
            if (a.getDevice().equalsIgnoreCase("fan_in")) {
                edtFanInPeriod.setText(a.getOnPeriod() / 60 + "");
            } else if (a.getDevice().equalsIgnoreCase("fan_out")) {
                edtFanOutPeriod.setText(a.getOnPeriod() / 60 + "");
            }
        }
        Utils.log('i', "SprayerRuleFragment: updateDryingRule() end");
    }

    private void saveDryingRule() {
        Utils.log('i', "SprayerRuleFragment: saveDryingRule() start");
        dryingRule.getActions().get(2).setDevice("no device");
        dryingRule.getActions().get(2).setOnPeriod(0);
        dryingRule.getActions().get(3).setDevice("no device");
        dryingRule.getActions().get(3).setOnPeriod(0);
        if (tcuservice != null) {
            try {
                Utils.log('i', "SprayerRuleFragment: saveDryingRule()");
                Message msg = Message.obtain(null, TcuService.CMD_SET_SPRAYERRULE);
                msg.replyTo = mMessenger;
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new Gson().toJsonTree(dryingRule).getAsJsonObject();
                data.putString("json", new Gson().toJson(d));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "SprayerRuleFragment: TcuService is not ready yet");
        }
        Utils.log('i', "SprayerRuleFragment: saveDryingRule() end");
    }

    private boolean checkInteger(EditText field, String value) {
        if (value.trim().length() > 0) {
            try {
                int rv = Integer.parseInt(value);
                if (rv < 0 || rv > 60) {
                    field.setError("Waarde moet tussen " + 0 + " en " + 60 + " zijn.");
                }
            } catch (NumberFormatException e) {
                field.setError("Waarde moet tussen " + 0 + " en " + 60 + " zijn.");
            }
        }
        return field.getError() == null;
    }
}
