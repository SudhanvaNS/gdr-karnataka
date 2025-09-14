package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
//import android.Manifest;
//import android.telephony.SmsManager;
//import android.view.View;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.*;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    EditText messageInput;
    Button  exportButton;
    ListView messageList;
    ArrayAdapter<String> adapter;
    ArrayList<String> messages;
    EditText intervalInput;
    Handler handler = new Handler();
    Runnable messageRunnable;
    static String senderNumber;
    String targetNumber;
    int interval = 60000;// milliseconds default
    //8105583426
    static Map<String, Pair<String, String>> numberMap = new HashMap<>();
    Map<String, Runnable> runnableMap = new HashMap<>();
    Map<String, Handler> handlerMap = new HashMap<>();
    Map<String, String> messageMap = new HashMap<>();

    Button executeCommandButton;
    TextView statusText;
    Map<String, Long> startTimeMap = new HashMap<>();

    static Set<String> activeKeys = new HashSet<>();
    EditText commandInput;
    private final String[] keys = {"a", "b", "j", "v"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadNumberMap();
        Button openSettings = findViewById(R.id.btn_open_settings);
        openSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NumberPairActivity.class))
        );
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }
//08800112112  AX-LEALOC
//        numberMap.put("a", new Pair<>("8368929116", "8368929116"));
//        numberMap.put("b", new Pair<>("09418099998", "9418099998"));
//        numberMap.put("j", new Pair<>("07021265165", "7021265165"));
//        numberMap.put("v", new Pair<>("54051", "VI-CELLOC"));
        registerReceiver(smsListener, new IntentFilter("com.example.message.NEW_SMS"));
        messageInput = findViewById(R.id.messageInput);
        exportButton = findViewById(R.id.exportButton);
        messageList = findViewById(R.id.messageList);
        commandInput = findViewById(R.id.commandInput);
        executeCommandButton = findViewById(R.id.executeCommandButton);
        statusText = findViewById(R.id.statusText); // Add a TextView in XML for this
        intervalInput = findViewById(R.id.intervalInput);
        messages = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(adapter);

        requestPermissions();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS}, 1);
        }
        executeCommandButton.setOnClickListener(v -> {
            String command = commandInput.getText().toString().trim().toLowerCase();
            handleCommand(command);
        });
      }
    public static Set<String> getActiveSenderNumbers() {
        Set<String> activeSenders = new HashSet<>();
        for (String key : activeKeys) {
            Pair<String, String> pair = numberMap.get(key);
            if (pair != null) {
                activeSenders.add(pair.second); // or pair.first depending on who replies
            }
        }
        return activeSenders;
    }
    private void loadNumberMap() {
        SharedPreferences prefs = getSharedPreferences("NumberPairs", MODE_PRIVATE);
        for (String k : keys) {
            String target = prefs.getString(k + "_target", "");
            String sender = prefs.getString(k + "_sender", "");
            if (target == null || target.isEmpty()) {
                Log.e("NumberMap", "Missing target number for key: " + k);
            }
            if (sender == null || sender.isEmpty()) {
                Log.e("NumberMap", "Missing sender number for key: " + k);
            }

//            numberMap.put(k, new Pair<>(target, sender));
            numberMap.put(k, new Pair<>(target, sender));
        }
    }

    public static boolean isActiveSender(String sender) {
        return getActiveSenderNumbers().contains(sender);
    }
    void handleCommand(String command) {
        String intervalStr = intervalInput.getText().toString();
        String msg = messageInput.getText().toString();

            if (command.startsWith("start ")) {
                String key = command.substring(6).trim();
                senderNumber=numberMap.get(key).second;
                targetNumber=numberMap.get(key).first;
                if (numberMap.containsKey(key)) {
                    startTimeMap.put(key, System.currentTimeMillis());
                    startSendingForKey(key);
                    updateStatusView();
                } else {
                    Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
                }
            } else if (command.startsWith("stop ")) {
                String key = command.substring(5).trim();
                stopSendingForKey(key);
            } else {
                Toast.makeText(this, "Unknown command", Toast.LENGTH_SHORT).show();
            }

    }
    private void updateStatusView() {
        if (activeKeys.isEmpty()) {
            statusText.setText("Running: None");
            return;
        }

        StringBuilder text = new StringBuilder("Running:\n");
        for (String key : activeKeys) {
            String msg = messageMap.getOrDefault(key, "N/A");
            long time = startTimeMap.getOrDefault(key, 0L);
            String timeStr = (time == 0L) ? "N/A" : new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(time));
            text.append(key).append(" → ").append(msg).append(" [").append(timeStr).append("]\n");
        }
        statusText.setText(text.toString().trim());
    }

    void startSendingForKey(String key) {
        if (runnableMap.containsKey(key)) {
            Toast.makeText(this, "Already sending for " + key, Toast.LENGTH_SHORT).show();
            return;
        }

        Pair<String, String> pair = numberMap.get(key);
        String target = pair.first;
        String msg = messageInput.getText().toString().trim();
        messageMap.put(key, msg);
        String intervalStr = intervalInput.getText().toString().trim();
        if (msg.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(this, "Enter message and interval first", Toast.LENGTH_SHORT).show();
            return;
        }

        activeKeys.add(key);
        int intervalMillis = Integer.parseInt(intervalStr) * 60000;

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(target, null, msg, null, null);
                Log.d("SendSMS", "Sent to: " + target);

                handler.postDelayed(this, intervalMillis);
            }
        };

        handler.post(runnable);
        runnableMap.put(key, runnable);
        handlerMap.put(key, handler);

        Toast.makeText(this, "Started sending for " + key, Toast.LENGTH_SHORT).show();
    }
    void stopSendingForKey(String key) {
        if (runnableMap.containsKey(key)) {
            MessageExporter.exportSmsFromNumber(this, numberMap.get(key).second , startTimeMap.get(key));
            Handler handler = handlerMap.get(key);
            Runnable runnable = runnableMap.get(key);
            handler.removeCallbacks(runnable);
            runnableMap.remove(key);
            handlerMap.remove(key);
            activeKeys.remove(key);
            startTimeMap.remove(key);
            messageMap.remove(key);
            updateStatusView();
            Toast.makeText(this, "Stopped sending for " + key, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No active sending for " + key, Toast.LENGTH_SHORT).show();
        }
    }

    void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 1);
    }

//    void startSending() {
//        String msg = messageInput.getText().toString();
//        String intervalStr = intervalInput.getText().toString().trim();
//        if (intervalStr.isEmpty()) {
//            Toast.makeText(MainActivity.this, "Enter interval!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            interval = Integer.parseInt(intervalStr) * 60000; // convert minutes to ms
////            startSending();
//        } catch (NumberFormatException e) {
//            Toast.makeText(MainActivity.this, "Invalid interval input", Toast.LENGTH_SHORT).show();
//        }
//        if (msg.isEmpty()) {
//            Toast.makeText(this, "Enter a message first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        Log.d("SendSMS", "Sending to targetNumber: " + targetNumber);
//        messageRunnable = new Runnable() {
//            @Override
//            public void run() {
//                SmsManager sms = SmsManager.getDefault();
//                sms.sendTextMessage(targetNumber, null, msg, null, null);
//                handler.postDelayed(this, interval);
//            }
//        };
//        handler.post(messageRunnable);
//        Toast.makeText(this, "Started sending...", Toast.LENGTH_SHORT).show();
//    }

//    void stopSending() {
//        if (messageRunnable != null) {
//            handler.removeCallbacks(messageRunnable);
//            Toast.makeText(this, "Stopped sending", Toast.LENGTH_SHORT).show();
//        }
//    }

    public void addReceivedMessage(String msg) {
        runOnUiThread(() -> {
            messages.add(msg);
            adapter.notifyDataSetChanged();
        });
    }
    BroadcastReceiver smsListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            addReceivedMessage(msg);
        }
    };

}
