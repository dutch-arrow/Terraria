package nl.das.terraria;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.fragments.HelpFragment;
import nl.das.terraria.fragments.HistoryFragment;
import nl.das.terraria.fragments.RulesetsFragment;
import nl.das.terraria.fragments.StateFragment;
import nl.das.terraria.fragments.TimersFragment;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Properties;
import nl.das.terraria.services.TcuService;

public class TerrariaApp extends AppCompatActivity {

    public static final boolean LOGGING = true;
    public static final boolean[] MOCK = {false, false, false};

    public TerrariaApp() {
        supportedMessages.add(TcuService.CMD_GET_PROPERTIES);
    }

    private Messenger tcuService;
    private boolean bound;
    private Boolean bluetoothEnabled;

    /*
     * Handler of incoming messages from TcuService.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"TerrariaApp: handle Response for command '" + TcuService.btCommands[msg.what] + "'" );
            if (msg.what == TcuService.CMD_GET_PROPERTIES) {
                Utils.log('i', "TerrariaApp: " + msg.obj.toString());
                if (msg.obj.toString().startsWith("{\"error")) {
                    Error err = new Gson().fromJson(msg.obj.toString(), Error.class);
                    Utils.showMessage(getBaseContext(), appView, err.getError());
                } else {
                    Properties tmp = new Gson().fromJson(msg.obj.toString(), Properties.class);
                    configs[curTabNr - 1].setDevices(tmp.getDevices());
                    configs[curTabNr - 1].setTcu(tmp.getTcu());
                    configs[curTabNr - 1].setNrOfTimers(tmp.getNrOfTimers());
                    configs[curTabNr - 1].setNrOfPrograms(tmp.getNrOfPrograms());
                    mTabbar.setVisibility(View.VISIBLE);
                    menu.findItem(R.id.menu_history_item).setVisible(true);
                    for (int i = 0; i < nrOfTerraria; i++) {
                        mTabTitles[i].setVisibility(View.VISIBLE);
                        mTabTitles[i].setText(getString(R.string.tabName, configs[i].getTcuName(), (MOCK[i] ? " (Test)" : "")));
                    }
                    menu.performIdentifierAction(R.id.menu_state_item, 0); // select state fragment
                }
                wait.dismiss();
            }
        }
    }

    /*
     * Messenger that is published to TcuService to recieve messages.
     */
    private Messenger messenger = new Messenger(new IncomingHandler());

    /*
     * Service connection that connects to the TcuService.
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            tcuService = new Messenger(service);
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                // Create message to register this client
                Message msg = Message.obtain(null, TcuService.MSG_REGISTER_CLIENT);
                // Add arguments
                Bundle bdl = new Bundle();
                bdl.putIntegerArrayList("commands", supportedMessages);
                msg.setData(bdl);
                // Add the client messenger to reply to
                msg.replyTo = messenger;
                tcuService.send(msg);
                bound = true;
                // and select the current tab
                mTabTitles[curTabNr - 1].performClick();
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            messenger = null;
            bound = false;
        }
    };

    public static int nrOfTerraria;
    public static Properties[] configs;
    public static int curTabNr;
    public static TerrariaApp instance;
    private BluetoothAdapter btAdapter;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();

    public View appView;
    private View mTabbar;
    private TextView[] mTabTitles;
    private WaitSpinner wait;
    public Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.log('i', "TerrariaApp - onCreate start");
        setContentView(R.layout.activity_main);
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        appView = findViewById(android.R.id.content).getRootView();
        instance = this;
        wait = new WaitSpinner(this);
        curTabNr = 3;

        // Initialize Bluetooth adapter
        initBluetooth();
        // Get the configuration from the properties file
        java.util.Properties config = readConfig();
        mTabTitles = new TextView[nrOfTerraria];
        configs = new Properties[nrOfTerraria];
        mTabbar = findViewById(R.id.tabbar);
        mTabbar.setVisibility(View.GONE);
        // Get the properties from the TCU's
        for (int i = 0; i < nrOfTerraria; i++) {
            int tabnr = i + 1;
            int r = getResources().getIdentifier("tab" + tabnr, "id", "nl.das.terraria");
            mTabTitles[i] = findViewById(r);
            configs[i] = new Properties();
            configs[i].setTcuName(config.getProperty("t" + tabnr +".title"));
            configs[i].setDeviceName(config.getProperty("t" + tabnr +".hostname"));
            configs[i].setUuid(config.getProperty("t" + tabnr +".uuid"));
            configs[i].setIp(config.getProperty("t" + tabnr +".ip"));
            configs[i].setMockPostfix(config.getProperty("t" + tabnr +".mock_postfix"));
            mTabTitles[i].setText(getString(R.string.tabName, configs[i].getTcuName(), (MOCK[i] ? " (Test)" : "")));
            mTabTitles[i].setVisibility(View.VISIBLE);
        }

        // Create the main toolbar
        Toolbar mTopToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mTopToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Now start the TcuService
        Intent intent = new Intent(getApplicationContext(), TcuService.class);
        Bundle data = new Bundle();
        ArrayList<String> hosts = new ArrayList<>();
        ArrayList<String> uuids = new ArrayList<>();
        ArrayList<String> ips = new ArrayList<>();
        for (int i = 0; i < nrOfTerraria; i++) {
            hosts.add(configs[i].getDeviceName());
            uuids.add(configs[i].getUuid());
            ips.add(configs[i].getIp());
        }
        data.putStringArrayList("hosts", hosts);
        data.putStringArrayList("uuids", uuids);
        data.putStringArrayList("ips", ips);
        intent.putExtras(data);

        startService(intent);

        Utils.log('i', "TerrariaApp - onCreate end");
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Utils.log('i',"TerrariaApp - onStart()");
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.log('i',"TerrariaApp - onResume start");
//        mTabbar.setVisibility(View.VISIBLE);
//        mTabTitles[curTabNr - 1].setTextColor(Color.WHITE);
        // Bind to the TcuService
        Intent intent = new Intent(getApplicationContext(), TcuService.class);
        if (!getApplicationContext().bindService(intent, connection, 0)) {
            Utils.log('e', "TerrariaApp: Could not bind to BTService");
        }
        Utils.log('i',"TerrariaApp - onResume end");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.log('i',"TerrariaApp - onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.log('i',"TerrariaApp - onStop start");
        if (bound) {
            Message msg = Message.obtain(null, TcuService.MSG_UNREGISTER_CLIENT);
            try {
                tcuService.send(msg);
            } catch (RemoteException e) {
            }
            getApplicationContext().unbindService(connection);
        }
        Utils.log('i',"TerrariaApp - onStop end");
        // Unbind from service
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.log('i',"TerrariaApp - onDestroy start");
        Utils.log('i',"TerrariaApp - onDestroy end");
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_state_item) {
            mTabbar.setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layout, StateFragment.newInstance(curTabNr), "state")
                    .commit();
            return true;
        }
        if (id == R.id.menu_timers_item) {
            mTabbar.setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layout, TimersFragment.newInstance(curTabNr), "timers")
                    .commit();
            return true;
        }
        if (id == R.id.menu_program_item) {
            mTabbar.setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layout, RulesetsFragment.newInstance(curTabNr), "rulesets")
                    .commit();
            return true;
        }
        if (id == R.id.menu_history_item) {
            mTabbar.setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layout, HistoryFragment.newInstance(curTabNr),"history")
                    .commit();
            return true;
        }
        if (id == R.id.menu_help_item) {
            mTabbar.setVisibility(View.GONE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layout, new HelpFragment(), "help")
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onTabSelect(View v){
        Utils.log('i',"TerrariaApp: onTabSelect()");
        int tcunr = curTabNr - 1;
        List<Fragment> frags = getSupportFragmentManager().getFragments();
        for (Fragment f : frags) {
            Utils.log('i',"TerrariaApp: Active fragment '" + f.getTag() + "' removed");
            getSupportFragmentManager().beginTransaction().remove(f).commit();
        }
        mTabTitles[tcunr].setTextColor(Color.BLACK);
        curTabNr = Integer.parseInt((String)v.getTag());
        tcunr = curTabNr - 1;
        mTabbar.setVisibility(View.VISIBLE);
        mTabTitles[tcunr].setTextColor(Color.WHITE);
        // Now thet the properties of the TCU linked to the current tab number
        getProperties(tcunr);
    }

    private java.util.Properties readConfig() {
        java.util.Properties config = new java.util.Properties();
        AssetManager assetManager = getAssets();
        try {
            config.load(assetManager.open("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        nrOfTerraria = Integer.parseInt(config.getProperty("nrOfTerraria"));
        return config;
    }

    public void getProperties(int tcunr) {
        String pfx = configs[tcunr].getMockPostfix();
        if (MOCK[tcunr]) {
            Utils.log('i',"TerrariaApp: getProperties() from file (mock)");
            try {
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("properties_" + pfx + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                Properties tmp = new Gson().fromJson(response, Properties.class);
                configs[tcunr].setDevices(tmp.getDevices());
                configs[tcunr].setTcu(tmp.getTcu());
                configs[tcunr].setNrOfTimers(tmp.getNrOfTimers());
                configs[tcunr].setNrOfPrograms(tmp.getNrOfPrograms());
            } catch (IOException ignored) {
            }
        } else {
            if (tcuService != null) {
                Utils.log('i', "TerrariaApp: getProperties() from '" + configs[tcunr].getDeviceName() + "'");
                wait.start();
                try {
                    Message msg = Message.obtain(null, TcuService.CMD_GET_PROPERTIES);
                    Bundle data = new Bundle();
                    data.putInt("tcunr", tcunr);
                    msg.setData(data);
                    msg.replyTo = messenger;
                    tcuService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                    Utils.log('i', "TerrariaApp: TcuService has crashed");
                }
            } else {
                Utils.log('i', "TerrariaApp: TcuService is not ready yet");
            }
        }
    }

    public void setBluetoothIcon() {
        ((ImageView)appView.findViewById(R.id.WifiOrBT)).setImageResource(R.drawable.bluetooth);
    }

    public void setWifiIcon() {
        ((ImageView)appView.findViewById(R.id.WifiOrBT)).setImageResource(R.drawable.wifi);
    }
    /*
     * Wifi methods
     */
    private boolean checkWifiConnection(int tcunr) {
        boolean reachable = false;
        // Executed in separate thread
        Runtime runtime = Runtime.getRuntime();
         try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 -w 1 " + configs[tcunr].getIp());
            int exitValue = ipProcess.waitFor();
             reachable = (exitValue == 0);
            ipProcess.destroy();
        } catch (IOException | InterruptedException e) {
        }
        return reachable;
    }

    ActivityResultLauncher<String> reqPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            result -> {
                Utils.log('i', "TerrariaApp - initBluetooth() reqPermission=" + result);
            }
    );

    ActivityResultLauncher<Intent> enableBt = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Utils.log('i', "TerrariaApp - initBluetooth(): enableBt=" + result.getResultCode());
                bluetoothEnabled = (result.getResultCode() == RESULT_OK);
            }
    );
    /*
     * Bluetooth methods
     */
    private void initBluetooth() {
        BluetoothDevice curBTDevice = null;
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Utils.log('e', "TerrariaApp - initBluetooth : android.permission.BLUETOOTH not given. ");
            reqPermission.launch(Manifest.permission.BLUETOOTH);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Utils.log('e', "TerrariaApp initBluetooth(): android.permission.BLUETOOTH_ADMIN not given. ");
            reqPermission.launch(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Utils.log('e', "TerrariaApp - initBluetooth: android.permission.BLUETOOTH_SCAN not given. ");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reqPermission.launch(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Utils.log('e', "TerrariaApp: initBluetooth(): android.permission.BLUETOOTH_CONNECT not given.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reqPermission.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        // Get the list of paired devices from the local Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.i("TerrariaBT", "TerrariaApp - initBluetooth(): No bluetooth adapter found.");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Utils.log('i', "TerrariaApp - initBluetooth(): Bluetooth is not enabled.");
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                try {
//                    enableBt.launch(enableBtIntent);
//                } catch (ActivityNotFoundException e) {
//                    Log.e("TerrariaBT", "TerrariaApp - initBluetooth() " + e.getMessage());
//                }
            } else {
                Utils.log('i', "TerrariaApp - initBluetooth(): Bluetooth adapter ready.");
            }
        }
    }
}