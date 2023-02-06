package nl.das.terraria.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import nl.das.terraria.Utils;
import nl.das.terraria.dialogs.WaitSpinner;

public class HttpService {
    public HttpService() { }

    public String sendGetRequest(String request) {
        try {
            Future<String> completableFuture = send("GET", request, null);
            Utils.log('i', "HttpService - sendGetRequest: " + request);
            String res = completableFuture.get(5, TimeUnit.SECONDS);
//            Utils.log('i', "HttpService - Response - " + res);
            return res;
        } catch (InterruptedException e) {
            return("ERROR: Interrupted");
        } catch (ExecutionException e) {
            return("ERROR:" + e.getMessage());
        } catch (TimeoutException e) {
            return("ERROR: Timeout");
        }
    }

    public String sendPostRequest(String request, String json) {
        try {
            Future<String> completableFuture = send("POST", request, json);
            Utils.log('i', "HttpService - sendPostRequest: " + request);
            String res = completableFuture.get(5, TimeUnit.SECONDS);
            return res;
        } catch (InterruptedException e) {
            return("ERROR: Interrupted");
        } catch (ExecutionException e) {
            return("ERROR:" + e.getMessage());
        } catch (TimeoutException e) {
            return("ERROR: Timeout");
        }
    }
/*================================================================================================*/

    private Future<String> send(String method, String request, String json) throws InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        ThreadFactory tf = runnable -> {
            Thread t = new Thread(runnable,"HttpService");
            return t;
        };
        Executors.newSingleThreadExecutor(tf).submit(() -> {
            Utils.log('i', "HttpService - send start");
            if (method.equalsIgnoreCase("GET")) {
                String resjson = handleHttpGetRequest(request);
                completableFuture.complete(resjson);
            } else {
                String resjson = handleHttpPostRequest(request, json);
                completableFuture.complete(resjson);
            }
            Utils.log('i', "HttpService - send end");
        });
        return completableFuture;
    }

    private String handleHttpGetRequest(String request) {
        HttpURLConnection urlConnection = null;
        String response = null;
        try {
            URL url = new URL(request);
            urlConnection = (HttpURLConnection) url.openConnection();
            int code = urlConnection.getResponseCode();
            if (code !=  200) {
                throw new IOException("Invalid response from server: " + code);
            }
            response = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n")
                    );
        } catch (Exception e) {
            response = "ERROR: " + e.getMessage();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return response;
    }

    private String handleHttpPostRequest(String request, String json) {
        HttpURLConnection urlConnection = null;
        String response = null;
        try {
           URL url = new URL(request);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);
            // Send data
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(json);
            writer.flush();
            int code = urlConnection.getResponseCode();
            if (code !=  200) {
                throw new IOException("Invalid response from server: " + code);
            }
            response = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n")
                    );
        } catch(IOException e) {
            response = "ERROR: " + e.getMessage();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return response;
     }
}


