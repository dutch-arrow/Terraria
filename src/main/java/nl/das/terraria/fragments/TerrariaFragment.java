package nl.das.terraria.fragments;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nl.das.terraria.R;
import nl.das.terraria.TerrariaApp;
import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;
import nl.das.terraria.json.Error;
import nl.das.terraria.json.Properties;
import nl.das.terraria.services.TcuService;

public class TerrariaFragment extends Fragment {

    /*
     * Handler of incoming messages from TcuService.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Utils.log('i',"TerrariaFragment: handle Response for command '" + TcuService.btCommands[msg.what] + "'" );
            if (msg.what == TcuService.CMD_GET_PROPERTIES) {
                Utils.log('i', "TerrariaFragment: " + msg.obj.toString());
                if (msg.obj.toString().startsWith("{\"error")) {
                    Error err = new Gson().fromJson(msg.obj.toString(), Error.class);
                    Utils.showMessage(TerrariaApp.instance.getApplicationContext(), getView(), err.getError());
                } else {
                    Properties tmp = new Gson().fromJson(msg.obj.toString(), Properties.class);
                    TerrariaApp.configs.get(curTabNr).setDevices(tmp.getDevices());
                    TerrariaApp.configs.get(curTabNr).setNrOfTimers(tmp.getNrOfTimers());
                    TerrariaApp.configs.get(curTabNr).setNrOfPrograms(tmp.getNrOfPrograms());
                    mTabbar.setVisibility(View.VISIBLE);
                    for (int i = 0; i < TerrariaApp.nrOfTerraria; i++) {
                        mTabTitles[i].setVisibility(View.VISIBLE);
                        mTabTitles[i].setText(TerrariaApp.configs.get(i + 1).getTcuName() +(MOCK[i] ? " (Test)" : ""));
                    }
                    TerrariaApp.menu.performIdentifierAction(R.id.menu_state_item, 0); // select state fragment
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

    public static final boolean[] MOCK = {false, false, false};

    private View mTabbar;
    private TextView[] mTabTitles;
    public static int curTabNr;
    private WaitSpinner wait;
    private Messenger tcuService;
    private boolean bound;
    private final ArrayList<Integer> supportedMessages = new ArrayList<>();

    public TerrariaFragment() {
        supportedMessages.add(TcuService.CMD_GET_PROPERTIES);
    }

    public static TerrariaFragment newInstance() {
        TerrariaFragment fragment = new TerrariaFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.log('i', "TerrariaFragment - onCreate start");
        super.onCreate(savedInstanceState);
        wait = new WaitSpinner(getContext());
        curTabNr = 1;
        Utils.log('i', "TerrariaFragment - onCreate end");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.log('i', "TerrariaFragment - onCreateView start");
        View view =  inflater.inflate(R.layout.fragment_terraria, container, false);
        Utils.log('i', "TerrariaFragment - onCreateView end");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.log('i', "TerrariaFragment - onViewCreated start");
        super.onViewCreated(view, savedInstanceState);
        mTabbar = view.findViewById(R.id.app_tabbar);
        mTabbar.setVisibility(View.VISIBLE);
        mTabTitles = new TextView[10];
        Utils.log('i', "TerrariaFragment - onViewCreated nrOfTerraria=" + TerrariaApp.nrOfTerraria);
        for (int i = 1; i <= TerrariaApp.nrOfTerraria; i++) {
            int r = getResources().getIdentifier("tab" + i, "id", "nl.das.terraria");
            mTabTitles[i - 1] = view.findViewById(r);
            mTabTitles[i - 1].setText(getString(R.string.tabName, TerrariaApp.configs.get(i).getTcuName(), (MOCK[i] ? " (Test)" : "")));
            mTabTitles[i - 1].setVisibility(View.VISIBLE);
            mTabTitles[i - 1].setOnClickListener(v -> onTabSelect(v));
            // Bind to the TcuService
            Intent intent = new Intent(getContext(), TcuService.class);
            if (!getContext().bindService(intent, connection, 0)) {
                Utils.log('e', "TerrariaFragment: Could not bind to TcuService");
            }
        }
        Utils.log('i', "TerrariaFragment - onViewCreated end");
    }

    public void onTabSelect(View v){
        Utils.log('i',"TerrariaFragment: onTabSelect()");
        int tcunr = curTabNr;
//        List<Fragment> frags = getParentFragmentManager().getFragments();
//        for (Fragment f : frags) {
//            Utils.log('i',"TerrariaFragment: Active fragment '" + f.getTag() + "' removed");
//            getParentFragmentManager().beginTransaction().remove(f).commit();
//        }
        mTabTitles[curTabNr - 1].setTextColor(Color.BLACK);
        curTabNr = Integer.parseInt((String)v.getTag());
        mTabbar.setVisibility(View.VISIBLE);
        mTabTitles[curTabNr - 1].setTextColor(Color.WHITE);
        // Now thet the properties of the TCU linked to the current tab number
        getProperties(curTabNr);
    }

    public void getProperties(int tcunr) {
        String pfx = TerrariaApp.configs.get(tcunr).getMockPostfix();
        if (MOCK[tcunr]) {
            Utils.log('i',"TerrariaFragment: getProperties() from file (mock)");
            try {
                String response = new BufferedReader(
                        new InputStreamReader(getResources().getAssets().open("properties_" + pfx + ".json")))
                        .lines().collect(Collectors.joining("\n"));
                Properties tmp = new Gson().fromJson(response, Properties.class);
                TerrariaApp.configs.get(tcunr).setDevices(tmp.getDevices());
                TerrariaApp.configs.get(tcunr).setNrOfTimers(tmp.getNrOfTimers());
                TerrariaApp.configs.get(tcunr).setNrOfPrograms(tmp.getNrOfPrograms());
            } catch (IOException ignored) {
            }
        } else {
            if (tcuService != null) {
                Utils.log('i', "TerrariaFragment: getProperties() from '" + TerrariaApp.configs.get(tcunr).getDeviceName() + "'");
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
                    Utils.log('i', "TerrariaFragment: TcuService has crashed");
                }
            } else {
                Utils.log('i', "TerrariaFragment: TcuService is not ready yet");
            }
        }
    }
}