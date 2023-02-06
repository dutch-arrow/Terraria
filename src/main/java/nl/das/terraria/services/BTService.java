package nl.das.terraria.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import nl.das.terraria.Utils;
import nl.das.terraria.json.Command;
import nl.das.terraria.json.Response;

public class BTService {

    public BTService() { }

    public String sendRequest(BluetoothDevice device, String uuid, Command request) {
        try {
            if (device != null) {
                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // the app's UUID string, also used in the server code.
                    @SuppressLint("MissingPermission")
                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
                    String json = new Gson().toJson(request);
                    Future<String> completableFuture = send(socket, json);
                    Utils.log('i', "BTService - sendRequest: " + request.getCmd());
                    String res = completableFuture.get();
                    return res;
                } catch (IOException e) {
                    return ("ERROR: " + e.getMessage());
                }
            }
            return ("ERROR: No Bluetooth device");
        } catch (InterruptedException | ExecutionException e) {
            return("ERROR: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private Future<String> send(BluetoothSocket socket, String msg) throws InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable,"BTService");
                return t;
            }
        };
        Executors.newSingleThreadExecutor(tf).submit(() -> {
            Utils.log('i', "BTService - send start");
            InputStream mmInStream = null;
            OutputStream mmOutStream = null;
            try {
                socket.connect();
            } catch (IOException e) {
                completableFuture.complete(null);
                return null;
            }
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
                mmOutStream.write(msg.getBytes());
                mmOutStream.write(0x03);
                // Keep listening to the InputStream until an exception occurs.
                // Read from the InputStream.
                int b;
                StringBuilder sb = new StringBuilder();
                while ((b = mmInStream.read()) != -1) {
                    if (b == 0x03) { //ETX character
                        String message = sb.toString();
                        Utils.log('i',"BTService: Received a message of size  " + message.length());
//                        Utils.log('i', message);
                        Response res = new Gson().fromJson(message, Response.class);
                        completableFuture.complete(new Gson().toJson(res.getResponse()));
                        Utils.log('i', "BTService - send end");
                        break;
                    } else {
                        sb.append((char) b);
                    }
                }
                mmInStream.close();
                mmOutStream.close();
                socket.close();
            } catch (IOException e) {
                Utils.log('e', "BTService: Input stream IO error: " + e.getMessage());
                completableFuture.complete(null);
                if (socket.isConnected()) {
                    if (mmInStream != null) {
                        mmInStream.close();
                    }
                    if (mmOutStream != null) {
                        mmOutStream.close();
                    }
                    socket.close();
                }
            }
            return null;
        });
        return completableFuture;
    }
}
