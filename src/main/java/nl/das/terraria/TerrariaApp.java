package nl.das.terraria;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.fragments.ConfigurationFragment;
import nl.das.terraria.fragments.HelpFragment;
import nl.das.terraria.fragments.HistoryFragment;
import nl.das.terraria.fragments.RulesetsFragment;
import nl.das.terraria.fragments.StateFragment;
import nl.das.terraria.fragments.TerrariaFragment;
import nl.das.terraria.fragments.TimersFragment;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Properties;
import nl.das.terraria.services.TcuService;

public class TerrariaApp extends AppCompatActivity {

    public static final boolean LOGGING = false;

    public TerrariaApp() {

    }

    private Boolean bluetoothEnabled;

    public static int nrOfTerraria;
    public static Map<Integer,Properties> configs;
    public static TerrariaApp instance;

    public View appView;
    public static Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.log('i', "TerrariaApp - onCreate start");
        setContentView(R.layout.activity_terrarium);
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        appView = findViewById(android.R.id.content).getRootView();
        instance = this;
        loadConfig();
        if (nrOfTerraria == 0) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_layout, ConfigurationFragment.newInstance(nrOfTerraria), "config")
                    .commit();
        } else {
            // Initialize Bluetooth adapter
            initBluetooth();

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
            for (int i = 1; i <= nrOfTerraria; i++) {
                hosts.add(configs.get(i).getDeviceName());
                uuids.add(configs.get(i).getUuid());
                ips.add(configs.get(i).getIp());
            }
            data.putStringArrayList("hosts", hosts);
            data.putStringArrayList("uuids", uuids);
            data.putStringArrayList("ips", ips);
            intent.putExtras(data);

            startService(intent);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_layout, TerrariaFragment.newInstance(), "config")
                    .commit();
        }
        Utils.log('i', "TerrariaApp - onCreate end");
    }

    private void loadConfig() {
        Utils.log('i', "TerrariaApp - loadConfig() start");
        configs = new HashMap<>();
        // Fetching the stored data from the SharedPreference
        SharedPreferences sh = getSharedPreferences("TerrariaConfig", MODE_PRIVATE);
        nrOfTerraria = sh.getInt("nrOfTerraria", 0);
        Utils.log('i', "TerrariaApp - loadConfig() nrOfTerraria=" + nrOfTerraria);
        if (nrOfTerraria > 0) {
            for (int t = 1; t <= nrOfTerraria; t++) {
                Properties props = new Properties();
                props.setTcuName(sh.getString("t" + t + ".name", ""));
                props.setDeviceName(sh.getString("t" + t + ".host", ""));
                props.setUuid(sh.getString("t" + t + ".uuid", ""));
                props.setIp(sh.getString("t" + t + ".ip", ""));
                configs.put(t, props);
            }
        }
        Utils.log('i', "TerrariaApp - loadConfig() end");
    }
    public void saveConfig() {
        Utils.log('i', "TerrariaApp - saveConfig() start");
        nrOfTerraria = configs.keySet().size();
        SharedPreferences sh = getSharedPreferences("TerrariaConfig", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sh.edit();
        myEdit.putInt("nrOfTerraria", nrOfTerraria);
        for (int t = 1; t <= configs.keySet().size(); t++) {
            Properties props = configs.get(t);
            myEdit.putString("t" + t + ".host", props.getDeviceName());
            myEdit.putString("t" + t + ".name", props.getTcuName());
            myEdit.putString("t" + t + ".uuid", props.getUuid());
            myEdit.putString("t" + t + ".ip", props.getIp());
        }
        myEdit.commit();
        Utils.log('i', "TerrariaApp - saveConfig() end");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.log('i',"TerrariaApp - onResume start");
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.app_layout, StateFragment.newInstance(TerrariaFragment.curTabNr), "state")
                    .commit();
            return true;
        }
        if (id == R.id.menu_timers_item) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.app_layout, TimersFragment.newInstance(TerrariaFragment.curTabNr), "timers")
                    .commit();
            return true;
        }
        if (id == R.id.menu_program_item) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.app_layout, RulesetsFragment.newInstance(TerrariaFragment.curTabNr), "rulesets")
                    .commit();
            return true;
        }
        if (id == R.id.menu_history_item) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.app_layout, HistoryFragment.newInstance(TerrariaFragment.curTabNr),"history")
                    .commit();
            return true;
        }
        if (id == R.id.menu_config_item) {
            ConfigurationFragment frag = ConfigurationFragment.newInstance(nrOfTerraria);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_layout, frag, "config")
                    .commit();
            return true;
        }
        if (id == R.id.menu_terr_item) {
            TerrariaFragment frag = TerrariaFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_layout, frag, "app")
                    .commit();
            return true;
        }
        if (id == R.id.menu_help_item) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_layout, new HelpFragment(), "help")
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setBluetoothIcon() {
        ((ImageView)appView.findViewById(R.id.WifiOrBT)).setImageResource(R.drawable.bluetooth);
    }

    public void setWifiIcon() {
        ((ImageView)appView.findViewById(R.id.WifiOrBT)).setImageResource(R.drawable.wifi);
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
            } else {
                Utils.log('i', "TerrariaApp - initBluetooth(): Bluetooth adapter ready.");
            }
        }
    }
}