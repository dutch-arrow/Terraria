package nl.das.terraria.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.json.Command;

public class TcuService extends Service {

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int CMD_GET_PROPERTIES = 3;
    public static final int CMD_GET_SENSORS = 4;
    public static final int CMD_GET_STATE = 5;
    public static final int CMD_SET_DEVICE_ON = 6;
    public static final int CMD_SET_DEVICE_ON_FOR = 7;
    public static final int CMD_SET_DEVICE_OFF = 8;
    public static final int CMD_SET_DEVICE_MANUAL_ON = 9;
    public static final int CMD_SET_DEVICE_MANUAL_OFF = 10;
    public static final int CMD_SET_LIFECYCLE_COUNTER = 11;
    public static final int CMD_GET_TIMERS = 12;
    public static final int CMD_SET_TIMERS = 13;
    public static final int CMD_GET_RULESET = 14;
    public static final int CMD_SET_RULESET = 15;
    public static final int CMD_GET_SPRAYERRULE = 16;
    public static final int CMD_SET_SPRAYERRULE = 17;
    public static final int CMD_GET_TEMP_FILES = 18;
    public static final int CMD_GET_STATE_FILES = 19;
    public static final int CMD_GET_TEMP_FILE = 20;
    public static final int CMD_GET_STATE_FILE = 21;
    public static final int MSG_DISCONNECTED = 22;

    public static String[] btCommands = new String[22];
    static {
        btCommands[MSG_REGISTER_CLIENT]       = "registerClient";
        btCommands[MSG_UNREGISTER_CLIENT]     = "unregisterClient";
        btCommands[CMD_GET_PROPERTIES]        = "getProperties";
        btCommands[CMD_GET_SENSORS]           = "getSensors";
        btCommands[CMD_GET_STATE]             = "getState";
        btCommands[CMD_SET_DEVICE_ON]         = "setDeviceOn";
        btCommands[CMD_SET_DEVICE_OFF]        = "setDeviceOff";
        btCommands[CMD_SET_DEVICE_ON_FOR]     = "setDeviceOnFor";
        btCommands[CMD_SET_DEVICE_MANUAL_ON]  = "setDeviceManualOn";
        btCommands[CMD_SET_DEVICE_MANUAL_OFF] = "setDeviceManualOff";
        btCommands[CMD_SET_LIFECYCLE_COUNTER] = "setLifecycleCounter";
        btCommands[CMD_GET_TIMERS]            = "getTimersForDevice";
        btCommands[CMD_SET_TIMERS]            = "replaceTimers";
        btCommands[CMD_GET_RULESET]           = "getRuleset";
        btCommands[CMD_SET_RULESET]           = "saveRuleset";
        btCommands[CMD_GET_SPRAYERRULE]       = "getSprayerRule";
        btCommands[CMD_SET_SPRAYERRULE]       = "setSprayerRule";
        btCommands[CMD_GET_TEMP_FILES]        = "getTempTracefiles";
        btCommands[CMD_GET_STATE_FILES]       = "getStateTracefiles";
        btCommands[CMD_GET_TEMP_FILE]         = "getTemperatureFile";
        btCommands[CMD_GET_STATE_FILE]        = "getStateFile";
    }
    
    public static String[] httpPaths = new String[22];
    static {
        httpPaths[MSG_REGISTER_CLIENT]       = "registerClient";
        httpPaths[MSG_UNREGISTER_CLIENT]     = "unregisterClient";
        httpPaths[CMD_GET_PROPERTIES]        = "properties";
        httpPaths[CMD_GET_SENSORS]           = "sensors";
        httpPaths[CMD_GET_STATE]             = "state";
        httpPaths[CMD_SET_DEVICE_ON]         = "device/{device}/on";
        httpPaths[CMD_SET_DEVICE_OFF]        = "device/{device}/off";
        httpPaths[CMD_SET_DEVICE_ON_FOR]     = "device/{device}/on/{period}";
        httpPaths[CMD_SET_DEVICE_MANUAL_ON]  = "device/{device}/manual";
        httpPaths[CMD_SET_DEVICE_MANUAL_OFF] = "device/{device}/auto";
        httpPaths[CMD_SET_LIFECYCLE_COUNTER] = "counter/{device}/{value}";
        httpPaths[CMD_GET_TIMERS]            = "timers/{device}";
        httpPaths[CMD_SET_TIMERS]            = "timers";
        httpPaths[CMD_GET_RULESET]           = "ruleset/{nr}";
        httpPaths[CMD_SET_RULESET]           = "ruleset/{nr}";
        httpPaths[CMD_GET_SPRAYERRULE]       = "sprayerrule";
        httpPaths[CMD_SET_SPRAYERRULE]       = "sprayerrule";
        httpPaths[CMD_GET_TEMP_FILES]        = "history/temperature";
        httpPaths[CMD_GET_STATE_FILES]       = "history/state";
        httpPaths[CMD_GET_TEMP_FILE]         = "history/temperature/{fname}";
        httpPaths[CMD_GET_STATE_FILE]        = "history/state/{fname}";
    }

    private ArrayList<String> hosts;
    private ArrayList<String> uuids;
    private ArrayList<String> ips;
    private Map<String, BluetoothDevice> devices = new HashMap<>();

    private HttpService httpService = new HttpService();
    private BTService btService = new BTService();

    /*
     * Keeps track of all current registered clients.
     * Map<command, Messenger of client>
     */
    private static final Map<Integer, Set<Messenger>> clients = new HashMap<>();

/*
 * Messenger and response handler for communication with Clients.
 * The Messenger defined here is returned to the Client when it binds to the TcuService.
 */
    final Messenger messenger = new Messenger(new IncomingMessageHandler());

/*=================================================================================================*/

    public TcuService() { }

    /*
     * Called when a Client executes the startService-command.
     * The intent used as argument should contain a Bundle with
     * - a StringArray (hosts) containing all the connected TCU names
     * - a StringArray (uuids) containing all the unique IDs of the Bluetooth services
     * - a StringArray (ips) containing all de ip-addresses of the TCUs
     * NOTE: the arrays must be synced !
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Bundle data = intent.getExtras();
        hosts = data.getStringArrayList("hosts");
        uuids = data.getStringArrayList("uuids");
        ips = data.getStringArrayList("ips");
        // Create a socket for all hosts
        for (int tcunr = 0; tcunr < hosts.size(); tcunr++) {
            BluetoothDevice s = getBluetoothDevice(tcunr);
            devices.put(hosts.get(tcunr), s);
         }
        Utils.log('i', "TcuService: started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Utils.log('i', "TcuService: onBind()");
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Utils.log('i', "TcuService: onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

/*=================================================================================================*/

    @SuppressLint("HandlerLeak")
    class IncomingMessageHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            int tcunr = -1;
            if (msg.what != MSG_REGISTER_CLIENT && msg.what != MSG_UNREGISTER_CLIENT) {
                tcunr = msg.getData().getInt("tcunr");
            }
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Set<Integer> cmds = new HashSet<>(msg.getData().getIntegerArrayList("commands"));
                    Utils.log('i',"TcuService: Register client for commands " + cmds);
                    for (Integer cmd : cmds) {
                        clients.computeIfAbsent(cmd, k -> new HashSet<>());
                        clients.get(cmd).add(msg.replyTo);
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Utils.log('i',"TcuService: Unregister client");
                    for (int c : clients.keySet()) {
                        Set<Messenger> msgr = clients.get(c);
                        if (msgr != null) {
                            clients.get(c).remove(msg.replyTo);
                        }
                    }
                    break;
                default:
                    handleCommand(tcunr, msg.what, msg);
                    break;
            }
        }
    }

    private void handleCommand(int tcunr, int command, Message msg) {
        Utils.log('i',"TcuService - handleCommand(): " + btCommands[msg.what] + " from device '" + hosts.get(tcunr) + "'");
//        Utils.log('i', "TcuService - handleCommand(): json=" + msg.getData().getString("json"));
        // Get the json data from the message
        JsonObject jsonobj = null;
        String jsondata = msg.getData().getString("json");
        if (jsondata != null) {
            JsonElement jsonel = JsonParser.parseString(jsondata);
            if (jsonel != null) {
                jsonobj = jsonel.getAsJsonObject();
            }
        }
        // Check if Bluetooth is enabled and if so, if the TCU is reachable
        String json = null;
        BluetoothDevice s = devices.get(hosts.get(tcunr));
        if (s != null) {
            TerrariaApp.instance.setBluetoothIcon();
            json = btService.sendRequest(s, uuids.get(tcunr), new Command(btCommands[command], jsonobj));
            if (json == null || json.startsWith("ERROR")) {
                TerrariaApp.instance.setWifiIcon();
                json = sendHttpRequest(tcunr, command, jsonobj);
            }
        } else {
            TerrariaApp.instance.setWifiIcon();
            json = sendHttpRequest(tcunr, command, jsonobj);
        }
        if (json != null) {
            if (!json.startsWith("ERROR")) {
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                sendResponse(command, jsonObject, tcunr);
            } else {
                sendResponse(command, JsonParser.parseString("{\"error\":\"TCU '" + hosts.get(tcunr) + "' not reachable\"}").getAsJsonObject(), tcunr);
            }
        } else {
            sendResponse(command, null, tcunr);
        }
    }

    private String sendHttpRequest(int tcunr, int command, JsonObject jsonobj) {
        String json;
        String url = httpPaths[command];
        switch (command) {
            case CMD_SET_DEVICE_ON:
            case CMD_SET_DEVICE_OFF:
            case CMD_SET_DEVICE_MANUAL_ON:
            case CMD_SET_DEVICE_MANUAL_OFF:
            case CMD_GET_TIMERS:
            case CMD_SET_TIMERS:
                url = url.replace("{device}", jsonobj.get("device").getAsString());
                break;
            case CMD_GET_RULESET:
            case CMD_SET_RULESET:
                url = url.replace("{nr}", jsonobj.get("rulesetnr").getAsString());
                break;
            case CMD_SET_DEVICE_ON_FOR:
                url = url.replace("{device}", jsonobj.get("device").getAsString());
                url = url.replace("{period}", jsonobj.get("period").getAsString());
                break;
            case CMD_SET_LIFECYCLE_COUNTER:
                url = url.replace("{device}", jsonobj.get("device").getAsString());
                url = url.replace("{hoursOn}", jsonobj.get("hoursOn").getAsString());
                break;
            case CMD_GET_TEMP_FILE:
            case CMD_GET_STATE_FILE:
                url = url.replace("{fname}", jsonobj.get("fname").getAsString());
                break;
        }
        switch(command) {
            case CMD_SET_TIMERS:
            case CMD_SET_RULESET:
            case CMD_SET_SPRAYERRULE:
                json = httpService.sendPostRequest("http://" + ips.get(tcunr) + "/" + url, new Gson().toJson(jsonobj));
                break;
            default:
                json = httpService.sendGetRequest("http://" + ips.get(tcunr) + "/" + url);
                break;
        }
        return json;
    }

    private void sendResponse(int cmd, JsonObject obj, int tcunr) {
        Utils.log('i', "TcuService: sendResponse() of command '" + btCommands[cmd] + "' to TCU '" + hosts.get(tcunr) + "'");
        // Go through all Messengers that are registered for the given command
        // and when found sent it the message.
        if (clients.get(cmd) != null) {
            for (Messenger m : clients.get(cmd)) {
                try {
                    Message res = Message.obtain(null, cmd);
                    if (obj != null) {
                        res.obj = new Gson().toJson(obj);
                    }
                    m.send(res);
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    for (int c : clients.keySet()) {
                        clients.get(c).remove(m);
                    }
                }
            }
        }
    }

    private BluetoothDevice getBluetoothDevice(int tcunr) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = null;
        if (ba.isEnabled()) {
            @SuppressLint("MissingPermission")
            Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            Utils.log('i', "TerrariaApp: found " + pairedDevices.size() + " paired devices.");
            if (pairedDevices.size() > 0) {
                // There are paired devices.
                for (BluetoothDevice d : pairedDevices) {
                    @SuppressLint("MissingPermission")
                    String deviceName = d.getName();
                    if (deviceName.equalsIgnoreCase(hosts.get(tcunr))) {
                        device = d;
                        Utils.log('i', "TerrariaApp: found Bluetooth device '" + hosts.get(tcunr) + "'");
                        break;
                    }
                }
            }
        }
        return device;
    }
}