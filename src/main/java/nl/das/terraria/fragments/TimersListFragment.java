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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Timer;
import nl.das.terraria.json.Timers;
import nl.das.terraria.services.TcuService;

public class TimersListFragment extends Fragment {

    private String deviceID;
    private int tcunr;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();
    private Messenger tcuservice;
    private boolean bound;
    
    private Button btnSave;
    private Button btnRefresh;
    private final EditText[] edtTimeOn = new EditText[5];
    private final EditText[] edtTimeOff = new EditText[5];
    private final EditText[] edtRepeat = new EditText[5];
    private final EditText[] edtPeriod = new EditText[5];


    public static final Map<String, Timer[]> timers = new HashMap<>();
    private WaitSpinner wait;
    private InputMethodManager imm;

    public TimersListFragment() {
        supportedMessages.add(TcuService.CMD_GET_TIMERS);
        supportedMessages.add(TcuService.CMD_SET_TIMERS);
    }

    public static TimersListFragment newInstance(int tcunr, String device) {
        Utils.log('i', "TimersListFragment: newInstance() start");
        TimersListFragment fragment = new TimersListFragment();
        Bundle args = new Bundle();
        args.putString("deviceID", device);
        args.putInt("tcunr", tcunr);
        fragment.setArguments(args);
        Utils.log('i', "TimersListFragment: newInstance() start");
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
                getTimers();
            } catch (RemoteException e) {
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
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"TimersListFragment: handleMessage() for message " + msg.what );
            switch (msg.what) {
                case TcuService.CMD_GET_TIMERS:
                    Utils.log('i', "TimersListFragment: " + msg.obj.toString());
                    Timers devTimers = new Gson().fromJson(msg.obj.toString(), Timers.class);
                    timers.put(deviceID, devTimers.getTimers());
                    updateTimers();
                    wait.dismiss();
                    break;
                case TcuService.CMD_SET_TIMERS:
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
                        Utils.log('i', "TimersListFragment: No response");
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
        Utils.log('i', "TimersListFragment: onCreate() start");
        deviceID = requireArguments().getString("deviceID");
        tcunr = getArguments().getInt("tcunr");
        Utils.log('i', "TimersListFragment: onCreate() end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "TimersListFragment: onCreateView() start");
        View view = inflater.inflate(R.layout.five_timers_frg, container, false);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Utils.log('i', "TimersListFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "TimersListFragment: onViewCreated() start");
        wait = new WaitSpinner(requireContext());
        // Bind to TcuService
        Intent intent = new Intent(requireContext(), TcuService.class);
        if(!requireContext().bindService(intent, connection, 0)) {
            Utils.log('e',"TimersListFragment: Could not bind to TcuService");
        }
        btnSave = view.findViewById(R.id.ti_btnSave);
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> {
            btnSave.requestFocusFromTouch();
            saveTimers();
            imm.hideSoftInputFromWindow(btnSave.getWindowToken(), 0);
            btnSave.setEnabled(false);
        });
        btnRefresh = view.findViewById(R.id.ti_btnRefresh);
        btnRefresh.setEnabled(true);
        btnRefresh.setOnClickListener(v -> {
            wait.start();
            getTimers();
            imm.hideSoftInputFromWindow(btnRefresh.getWindowToken(), 0);
            btnSave.setEnabled(false);
        });
        int resId;
        for (int i = 0; i < 5; i++) {
            int nr = i;
            resId = getResources().getIdentifier("it_edtTimeOn_" + (i + 1), "id", requireContext().getPackageName());
            edtTimeOn[i] = view.findViewById(resId);
            edtTimeOn[i].setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(edtTimeOn[nr].getWindowToken(), 0);
                    if (checkTime(edtTimeOn[nr])) {
                        String value = String.valueOf(edtTimeOn[nr].getText()).trim();
                        int hr = Utils.getH(value);
                        int min = Utils.getM(value);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setHourOn(hr);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOn(min);
                        edtTimeOff[nr].requestFocus();
                        btnSave.setEnabled(true);
                    }
                }
                return false;
            });
            edtTimeOn[i].setOnFocusChangeListener(((v, hasFocus) -> {
                if (!hasFocus) {
                    if (checkTime(edtTimeOn[nr])) {
                        String value = String.valueOf(edtTimeOn[nr].getText()).trim();
                        int hr = Utils.getH(value);
                        int min = Utils.getM(value);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setHourOn(hr);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOn(min);
                        btnSave.setEnabled(true);
                    }
                }
            }));
            resId = getResources().getIdentifier("it_edtTimeOff_" + (i + 1), "id", requireContext().getPackageName());
            edtTimeOff[i] = view.findViewById(resId);
            edtTimeOff[i].setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(edtTimeOff[nr].getWindowToken(), 0);
                    if (checkTime(edtTimeOff[nr])) {
                        String value = String.valueOf(edtTimeOff[nr].getText()).trim();
                        int hr = Utils.getH(value);
                        int min = Utils.getM(value);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setHourOff(hr);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOff(min);
                        if (value.length() > 0) {
                            edtPeriod[nr].setText("0");
                            Objects.requireNonNull(TimersListFragment.timers.get(deviceID))[nr].setPeriod(0);
                        }
                        edtRepeat[nr].requestFocus();
                        btnSave.setEnabled(true);
                    }
                }
                return false;
            });
            edtTimeOff[i].setOnFocusChangeListener(((v, hasFocus) -> {
                if (!hasFocus) {
                    if (checkTime(edtTimeOff[nr])) {
                        String value = String.valueOf(edtTimeOff[nr].getText()).trim();
                        int hr = Utils.getH(value);
                        int min = Utils.getM(value);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setHourOff(hr);
                        Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOff(min);
                        if (value.length() > 0) {
                            edtPeriod[nr].setText("0");
                            Objects.requireNonNull(TimersListFragment.timers.get(deviceID))[nr].setPeriod(0);
                        }
                        btnSave.setEnabled(true);
                    }
                }
            }));
            resId = getResources().getIdentifier("it_edtRepeat_" + (i + 1), "id", requireContext().getPackageName());
            edtRepeat[i] = view.findViewById(resId);
            edtRepeat[i].setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(edtRepeat[nr].getWindowToken(), 0);
                    String value = String.valueOf(edtRepeat[nr].getText()).trim();
                    try {
                        int rv = Integer.parseInt(value);
                        if (rv < 0 || rv > 1) {
                            edtRepeat[nr].setError("Herhaling moet 0 of 1 zijn. 0 betekent niet aktief, 1 dagelijks");
                        }
                    }
                    catch (NumberFormatException e) {
                        edtRepeat[nr].setError("Herhaling moet 0 of 1 zijn.");
                    }
                    if (edtRepeat[nr].getError() == null) {
                        Objects.requireNonNull(timers.get(deviceID))[nr].setRepeat(Integer.parseInt(value));
                        edtPeriod[nr].requestFocus();
                        btnSave.setEnabled(true);
                    }
                }
                return false;
            });
            edtRepeat[i].setOnFocusChangeListener(((v, hasFocus) -> {
                if (!hasFocus) {
                    String value = String.valueOf(edtRepeat[nr].getText()).trim();
                    try {
                        int rv = Integer.parseInt(value);
                        if (rv < 0 || rv > 1) {
                            edtRepeat[nr].setError("Herhaling moet 0 of 1 zijn. 0 betekent niet aktief, 1 dagelijks");
                        }
                    }
                    catch (NumberFormatException e) {
                        edtRepeat[nr].setError("Herhaling moet 0 of 1 zijn.");
                    }
                    if (edtRepeat[nr].getError() == null) {
                        Objects.requireNonNull(timers.get(deviceID))[nr].setRepeat(Integer.parseInt(value));
                        btnSave.setEnabled(true);
                    }
                }
            }));
            resId = getResources().getIdentifier("it_edtPeriod_" + (i + 1), "id", requireContext().getPackageName());
            edtPeriod[i] = view.findViewById(resId);
            edtPeriod[i].setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(edtPeriod[nr].getWindowToken(), 0);
                    String value = String.valueOf(edtPeriod[nr].getText()).trim();
                    try {
                        int val = Integer.parseInt(value);
                        if (val < 0 || val > 3600) {
                            edtPeriod[nr].setError("Periode moet een getal tussen 0 en 3600 zijn.");
                        }
                    }
                    catch (NumberFormatException e) {
                        edtPeriod[nr].setError("Periode moet een getal tussen 0 en 3600 zijn.");
                    }
                    if (edtPeriod[nr].getError() == null) {
                        Objects.requireNonNull(TimersListFragment.timers.get(deviceID))[nr].setPeriod(Integer.parseInt(value));
                        if (!value.equalsIgnoreCase("0")) {
                            edtTimeOff[nr].setText("");
                            Objects.requireNonNull(timers.get(deviceID))[nr].setHourOff(0);
                            Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOff(0);
                        }
                        btnSave.setEnabled(true);
                    }
                }
                btnSave.requestFocusFromTouch();
                return false;
            });
            edtPeriod[i].setOnFocusChangeListener(((v, hasFocus) -> {
                if (!hasFocus) {
                    String value = String.valueOf(edtPeriod[nr].getText()).trim();
                    try {
                        int val = Integer.parseInt(value);
                        if (val < 0 || val > 3600) {
                            edtPeriod[nr].setError("Periode moet een getal tussen 0 en 3600 zijn.");
                        }
                    }
                    catch (NumberFormatException e) {
                        edtPeriod[nr].setError("Periode moet een getal tussen 0 en 3600 zijn.");
                    }
                    if (edtPeriod[nr].getError() == null) {
                        Objects.requireNonNull(TimersListFragment.timers.get(deviceID))[nr].setPeriod(Integer.parseInt(value));
                        if (!value.equalsIgnoreCase("0")) {
                            edtTimeOff[nr].setText("");
                            Objects.requireNonNull(timers.get(deviceID))[nr].setHourOff(0);
                            Objects.requireNonNull(timers.get(deviceID))[nr].setMinuteOff(0);
                        }
                        btnSave.setEnabled(true);
                    }
                }
            }));
        }
        Utils.log('i', "TimersListFragment: onViewCreated() end");
     }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.log('i', "TimersListFragment: onDestroy() start");
        if (tcuservice != null) {
            super.onDestroy();
            try {
                Message msg = Message.obtain(null, TcuService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = messenger;
                tcuservice.send(msg);
            } catch (RemoteException ignored) {
            }
            requireContext().unbindService(connection);
            Utils.log('i', "TimersListFragment: onDestroy() end");
        }
    }

    private void updateTimers() {
        Utils.log('i', "TimersListFragment: updateTimers() start");
        int[] ids = {R.id.it_layTimer_1, R.id.it_layTimer_2, R.id.it_layTimer_3, R.id.it_layTimer_4, R.id.it_layTimer_5};
        for (int i = 0; i < Objects.requireNonNull(timers.get(deviceID)).length; i++) {
            ConstraintLayout fcv = requireView().findViewById(ids[i]);
            fcv.setVisibility(View.VISIBLE);
            edtTimeOn[i].setText(Utils.cvthm2string(Objects.requireNonNull(timers.get(deviceID))[i].getHourOn(), Objects.requireNonNull(timers.get(deviceID))[i].getMinuteOn()));
            edtTimeOff[i].setText(Utils.cvthm2string(Objects.requireNonNull(timers.get(deviceID))[i].getHourOff(), Objects.requireNonNull(timers.get(deviceID))[i].getMinuteOff()));
            edtRepeat[i].setText(String.valueOf(Objects.requireNonNull(timers.get(deviceID))[i].getRepeat()));
            edtPeriod[i].setText(String.valueOf(Objects.requireNonNull(timers.get(deviceID))[i].getPeriod()));
        }
        Utils.log('i', "TimersListFragment: updateTimers() end");
    }

    private void getTimers() {
        Utils.log('i', "TimersListFragment: getTimers() start");
        if (TerrariaApp.MOCK[tcunr]) {
            try {
                Gson gson = new Gson();
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("timers_" + deviceID + "_" + TerrariaApp.configs[tcunr].getMockPostfix() + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                Timer[] devTimers = gson.fromJson(response, new TypeToken<Timer[]>() {}.getType());
                timers.put(deviceID, devTimers);
                updateTimers();
                wait.dismiss();
            } catch (IOException e) {
                wait.dismiss();
            }
        } else {
            if (tcuservice != null) {
                Utils.log('i', "TimersListFragment: getTimers() from server");
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_TIMERS);
                    msg.replyTo = messenger;
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    JsonObject d = new JsonObject();
                    d.addProperty("device", deviceID);
                    data.putString("json", new Gson().toJson(d));
                    msg.setData(data);
                    tcuservice.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            } else {
                Utils.log('i', "TimersListFragment: TcuService is not ready yet");
            }
        }
        Utils.log('i', "TimersListFragment: getTimers() end");
    }

    private void saveTimers() {
        Utils.log('i', "TimersListFragment: saveTimers() start");
        wait.start();
        if (tcuservice != null) {
            try {
                Utils.log('i', "TimersListFragment: saveTimers()");
                Message msg = Message.obtain(null, TcuService.CMD_SET_TIMERS);
                msg.replyTo = messenger;
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new JsonObject();
                d.addProperty("device", deviceID);
                JsonArray jsArray = new Gson().toJsonTree(timers.get(deviceID)).getAsJsonArray();
                d.add("timers", jsArray);
                data.putString("json", new Gson().toJson(d));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "TimersListFragment: TcuService is not ready yet");
        }
        Utils.log('i', "TimersListFragment: saveTimers() end");
    }

    public boolean checkTime(EditText field) {
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
}