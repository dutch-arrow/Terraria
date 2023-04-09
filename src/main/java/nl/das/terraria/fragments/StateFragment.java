package nl.das.terraria.fragments;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import nl.das.terraria.TerrariaApp;
import nl.das.terraria.services.TcuService;
import nl.das.terraria.R;
import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Device;
import nl.das.terraria.json.Sensor;
import nl.das.terraria.json.Sensors;
import nl.das.terraria.json.State;
import nl.das.terraria.json.States;

public class StateFragment extends Fragment {

    private int tcunr;
    private Sensors sensors;
    private WaitSpinner wait;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();
    private Messenger tcuservice;
    private boolean bound;

    private LinearLayout deviceLayout;
    private TextView tvwDateTime;
    private TextView tvwTTemp;
    private TextView tvwRHum;
    private TextView tvwRTemp;
    private TextView tvwCpu;

    public StateFragment() {
        supportedMessages.add(TcuService.CMD_GET_SENSORS);
        supportedMessages.add(TcuService.CMD_GET_STATE);
        supportedMessages.add(TcuService.CMD_SET_DEVICE_ON);
        supportedMessages.add(TcuService.CMD_SET_DEVICE_ON_FOR);
        supportedMessages.add(TcuService.CMD_SET_DEVICE_OFF);
        supportedMessages.add(TcuService.CMD_SET_DEVICE_MANUAL_ON);
        supportedMessages.add(TcuService.CMD_SET_DEVICE_MANUAL_OFF);
        supportedMessages.add(TcuService.CMD_SET_LIFECYCLE_COUNTER);
    }

    public static StateFragment newInstance(int tabnr) {
        Utils.log('i', "StateFragment.newInstance() start");
        StateFragment fragment = new StateFragment();
        Bundle args = new Bundle();
        args.putInt("tcunr", tabnr);
        fragment.setArguments(args);
        Utils.log('i', "StateFragment.newInstance() end");
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
                getSensors();
                getState();
            } catch (RemoteException e) { }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            tcuservice = null;
        }
    };

    /**
     * Handler of incoming messages from service.
     */
    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"StateFragment: handleMessage() for message " + msg.what );
            switch (msg.what) {
                case TcuService.CMD_GET_SENSORS:
                    Utils.log('i', "StateFragment: " + msg.obj.toString());
                    sensors = new Gson().fromJson(msg.obj.toString(), Sensors.class);
                    updateSensors();
                    break;
                case TcuService.CMD_GET_STATE:
                    Utils.log('i', "StateFragment: " + msg.obj.toString());
                    States states = new Gson().fromJson(msg.obj.toString(), States.class);
                    updateState(states.getStates());
                    wait.dismiss();
                    break;
                case TcuService.CMD_SET_DEVICE_ON:
                case TcuService.CMD_SET_DEVICE_ON_FOR:
                case TcuService.CMD_SET_DEVICE_OFF:
                case TcuService.CMD_SET_DEVICE_MANUAL_ON:
                case TcuService.CMD_SET_DEVICE_MANUAL_OFF:
                case TcuService.CMD_SET_LIFECYCLE_COUNTER:
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
                        Utils.log('i', "StateFragment: No response");
                    }
                    wait.dismiss();
                    break;
                default:
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.log('i', "StateFragment: onCreate() start");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tcunr = getArguments().getInt("tcunr");
        }
        Utils.log('i', "StateFragment: onCreate() end. TCUnr=" + tcunr);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "StateFragment: onCreateView() start");
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_state, container, false);
        deviceLayout = view.findViewById(R.id.trm_lay_device_state);
        for (Device d :  TerrariaApp.configs.get(tcunr).getDevices()) {
            View v = inflater.inflate(R.layout.fragment_device_state, container, false);
            SwitchCompat sw = v.findViewById(R.id.trm_switchDevice);
            String devname = d.getDevice();
            int r = getResources().getIdentifier(devname, "string", "nl.das.terraria");
            sw.setText(getResources().getString(r));
            if (devname.equalsIgnoreCase("uvlight")) {
                v.findViewById(R.id.trm_lay_uv).setVisibility(View.VISIBLE);
            } else {
                v.findViewById(R.id.trm_lay_uv).setVisibility(View.GONE);
            }
            deviceLayout.addView(v);
        }
        Utils.log('i', "StateFragment: onCreateView() end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "StateFragment: onViewCreated() start");
        super.onViewCreated(view, savedInstanceState);
        wait = new WaitSpinner(requireActivity());
        // Bind to TcuService
        Intent intent = new Intent(getContext(), TcuService.class);
        if(!requireContext().bindService(intent, connection, 0)) {
            Log.e("TerrariaBT","StateFragment: Could not bind to TcuService");
        }

        Button btn = view.findViewById(R.id.trm_refreshButton);
        btn.setOnClickListener(v -> {
            Utils.log('i', "StateFragment: refresh State");
            wait.start();
            getSensors();
            getState();
        });

        // walk through all device views
        int vix = 0;
        for (Device d :  TerrariaApp.configs.get(tcunr).getDevices()) {
            View v = deviceLayout.getChildAt(vix);
            SwitchCompat swManual = v.findViewById(R.id.trm_swManualDevice);
            TextView state = v.findViewById(R.id.trm_tvwDeviceState);
            swManual.setOnClickListener(cv -> {
                SwitchCompat sc = (SwitchCompat) cv;
                if (sc.isChecked()) {
                    switchManual(d.getDevice(), true);
                } else {
                    switchManual(d.getDevice(), false);
                    state.setText("");
                }
            });
            SwitchCompat swDevice = v.findViewById(R.id.trm_switchDevice);
            swDevice.setOnClickListener(cv -> {
                SwitchCompat sc = (SwitchCompat) cv;
                if (sc.isChecked()) {
                    // Create an instance of the dialog fragment and show it
                    OnPeriodDialogFragment dlgPeriod = OnPeriodDialogFragment.newInstance(d.getDevice());
                    FragmentManager fm = requireActivity().getSupportFragmentManager();
                    // SETS the target fragment for use later when sending results
                    fm.setFragmentResultListener("period", this, (requestKey, result) -> {
                        int period = result.getInt("period");
                        switchDevice(d.getDevice(), true, period);
                        if (period == 0) {
                            state.setText("geen eindtijd");
                        } else if (period < 0) {
                            state.setText("tot ideale temperatuur bereikt is");
                        } else if (period > 0) {
                            Date now = Calendar.getInstance().getTime();
                            String prd = (String) DateFormat.format("HH:mm:ss", (now.getTime() + (period * 1000)));
                            state.setText(prd);
                        }
                    });
                    dlgPeriod.show(fm, "OnPeriodDialogFragment");
                } else {
                    switchDevice(d.getDevice(), false, 0);
                    state.setText("");
                }
            });
            if (d.isLifecycle()) {
                TextView tvwHours = v.findViewById(R.id.trm_tvwHours_lcc);
                Button btnReset = v.findViewById(R.id.trm_btnReset);
                btnReset.setOnClickListener(cv -> {
                    Utils.log('i', "StateFragment: Reset lifecycle counter for device '" + d.getDevice() + "'");
                    // Create an instance of the dialog fragment and show it
                    ResetHoursDialogFragment dlgReset = ResetHoursDialogFragment.newInstance(d.getDevice());
                    FragmentManager fm = requireActivity().getSupportFragmentManager();
                    // SETS the target fragment for use later when sending results
                    fm.setFragmentResultListener("reset", this, (requestKey, result) -> {
                        tvwHours.setText(String.valueOf(result.getInt("hours")));
                        onResetHoursSave(d.getDevice(), result.getInt("hours"));
                    });
                    dlgReset.show(fm, "ResetHoursDialogFragment");
                });
            }
            vix++;
        }

        tvwDateTime = view.findViewById(R.id.trm_st_tvwDateTime);
        tvwTTemp = view.findViewById(R.id.trm_st_tvwTtemp);
        tvwRHum = view.findViewById(R.id.trm_st_tvwRhum);
        tvwRTemp = view.findViewById(R.id.trm_st_tvwRtemp);
        tvwCpu = view.findViewById(R.id.trm_st_tvwCpu);

        Utils.log('i', "StateFragment: onViewCreated() end");
    }

    @Override
    public void onDestroy() {
        Utils.log('i', "StateFragment: onDestroy() start");
        if (tcuservice != null) {
            try {
                Message msg = Message.obtain(null, TcuService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = messenger;
                tcuservice.send(msg);
            } catch (RemoteException ignored) {
            }
            requireContext().unbindService(connection);
        }
        Utils.log('i', "StateFragment: onDestroy() end");
        super.onDestroy();
    }

    private void switchDevice(String device, boolean yes, int period) {
        wait.start();
        if (tcuservice != null) {
            try {
                Message msg;
                if (yes) {
                    if (period > 0) {
                        Utils.log('i', "StateFragment: switchDevice() on");
                        msg = Message.obtain(null, TcuService.CMD_SET_DEVICE_ON_FOR);
                    } else {
                        Utils.log('i', "StateFragment: switchDevice() on");
                        msg = Message.obtain(null, TcuService.CMD_SET_DEVICE_ON);
                    }
                } else {
                    Utils.log('i', "StateFragment: switchDevice() off");
                    msg = Message.obtain(null, TcuService.CMD_SET_DEVICE_OFF);
                }
                msg.replyTo = messenger;
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new JsonObject();
                d.addProperty("device", device);
                if (period > 0) {
                    d.addProperty("period", period);
                }
                data.putString("json", new Gson().toJson(d));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "StateFragment: TcuService is not ready yet");
        }
    }

    private void switchManual(String device, boolean yes) {
        wait.start();
        if (tcuservice != null) {
            try {
                Message msg;
                if (yes) {
                    Utils.log('i', "StateFragment: switchManual() on");
                    msg = Message.obtain(null, TcuService.CMD_SET_DEVICE_MANUAL_ON);
                } else {
                    Utils.log('i', "StateFragment: switchManual() off");
                    msg = Message.obtain(null, TcuService.CMD_SET_DEVICE_MANUAL_OFF);
                }
                msg.replyTo = messenger;
                // data = {"tcunr": tcunr, "json": {"device": device}}
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new JsonObject();
                d.addProperty("device", device);
                data.putString("json", new Gson().toJson(d));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "StateFragment: TcuService is not ready yet");
        }
    }

    private void getSensors() {
        if (TerrariaFragment.MOCK[tcunr]) {
            Utils.log('i', "StateFragment: Retrieved mocked sensor readings");
            Gson gson = new Gson();
            try {
                String response = new BufferedReader(
                    new InputStreamReader(getResources().getAssets().open("sensors_" + TerrariaApp.configs.get(tcunr).getMockPostfix() + ".json")))
                    .lines().collect(Collectors.joining("\n"));
                sensors = gson.fromJson(response, Sensors.class);
                updateSensors();
            } catch (JsonSyntaxException | IOException e) {
                Utils.showMessage(requireContext(), getView(), "StateFragment: Sensors response contains errors:\n" + e.getMessage());
            }

        } else {
            if (tcuservice != null) {
                Utils.log('i', "StateFragment: getSensors() from server");
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_SENSORS);
                    msg.replyTo = messenger;
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    msg.setData(data);
                    tcuservice.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            } else {
                Utils.log('i', "StateFragment: TcuService is not ready yet");
            }
        }
    }

    private void updateSensors() {
        Utils.log('i',"StateFragment: updateSensors() start");
        tvwDateTime.setText(sensors.getClock());
        for (Sensor sensor: sensors.getSensors()) {
            if (sensor.getLocation().equalsIgnoreCase("room")) {
                tvwRTemp.setText(sensor.getTemperature().toString());
                tvwRHum.setText(sensor.getHumidity().toString());
            } else if (sensor.getLocation().equalsIgnoreCase("terrarium")) {
                tvwTTemp.setText(sensor.getTemperature().toString());
            } else if (sensor.getLocation().equalsIgnoreCase("cpu")) {
                tvwCpu.setText(sensor.getTemperature().toString());
            }
        }
        Utils.log('i',"StateFragment: updateSensors() end");
    }

    private void getState() {
        // Request state.
        if (TerrariaFragment.MOCK[tcunr]) {
            Utils.log('i', "StateFragment: Retrieved mocked state readings");
            Gson gson = new Gson();
            try {
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("state_" + TerrariaApp.configs.get(tcunr).getMockPostfix() + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                List<State> states = gson.fromJson(response, new TypeToken<List<State>>() {}.getType());
                updateState(states);
                wait.dismiss();
            } catch (JsonSyntaxException | IOException e) {
                Utils.showMessage(requireContext(), getView(), "StateFragment: State response contains errors:\n" + e.getMessage());
            }
        } else {
            if (tcuservice != null) {
                Utils.log('i', "StateFragment: getState() from server");
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_STATE);
                    msg.replyTo = messenger;
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    msg.setData(data);
                    tcuservice.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            } else {
                Utils.log('i', "StateFragment: TcuService is not ready yet");
            }
        }
    }

    private void updateState(List<State> states) {
        Utils.log('i',"StateFragment: updateState() start");
        int vix = 0;
        for (Device d :  TerrariaApp.configs.get(tcunr).getDevices()) {
            for (State s : states) {
                if (d.getDevice().equalsIgnoreCase(s.getDevice())) {
                    View v = deviceLayout.getChildAt(vix);
                    SwitchCompat swManual = v.findViewById(R.id.trm_swManualDevice);
                    SwitchCompat swDevice = v.findViewById(R.id.trm_switchDevice);
                    TextView state = v.findViewById(R.id.trm_tvwDeviceState);
                    swManual.setChecked(s.getManual().equalsIgnoreCase("yes"));
                    swDevice.setChecked(s.getState().equalsIgnoreCase("on"));
                    state.setText(translateEndTime(s.getEndTime()));
                    if (d.isLifecycle()) {
                        TextView h = v.findViewById(R.id.trm_tvwHours_lcc);
                        h.setText(s.getHoursOn().toString());
                    }
                }
            }
            vix++;
        }
        Utils.log('i',"StateFragment: updateState() end");
    }

    public void onResetHoursSave(String device, int hoursOn) {
        wait.start();
        if (tcuservice != null) {
            try {
                Utils.log('i', "StateFragment: onResetHoursSave()");
                Message msg = Message.obtain(null, TcuService.CMD_SET_LIFECYCLE_COUNTER);
                msg.replyTo = messenger;
                Bundle data = new Bundle();
                data.putInt("tcunr", tcunr);
                JsonObject d = new JsonObject();
                d.addProperty("device", device);
                d.addProperty("hoursOn", hoursOn);
                data.putString("json", new Gson().toJson(d));
                msg.setData(data);
                tcuservice.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        } else {
            Utils.log('i', "StateFragment: TcuService is not ready yet");
        }
    }

    private String translateEndTime(String endTime) {
        if (endTime == null) {
            return "";
        } else {
            if (endTime.equalsIgnoreCase("no endtime")) {
                return "geen eindtijd";
            } else if (endTime.equalsIgnoreCase("until ideal temperature is reached")) {
                return "tot ideale temperatuur bereikt is";
            } else if (endTime.equalsIgnoreCase("until ideal humidity is reached")) {
                return "tot ideale vochtigheidsgraad bereikt is";
            } else {
                return endTime;
            }
        }
    }
}