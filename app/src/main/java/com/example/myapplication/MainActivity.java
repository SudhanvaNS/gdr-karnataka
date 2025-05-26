package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
//import android.Manifest;
import android.content.pm.PackageManager;
import android.os.*;
//import android.telephony.SmsManager;
//import android.view.View;
import android.widget.*;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
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
    Map<String, Pair<String, String>> numberMap = new HashMap<>();
    Map<String, Runnable> runnableMap = new HashMap<>();
    Map<String, Handler> handlerMap = new HashMap<>();
    Button executeCommandButton;
    TextView statusText;
    Set<String> activeKeys = new HashSet<>();
    EditText commandInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numberMap.put("a", new Pair<>("8368929116", "8368929116"));
        numberMap.put("b", new Pair<>("09418099998", "9418099998"));
        numberMap.put("j", new Pair<>("07021265165", "7021265165"));
        numberMap.put("v", new Pair<>("54051", "VI-CELLOC"));
        registerReceiver(smsListener, new IntentFilter("com.example.message.NEW_SMS"));
        messageInput = findViewById(R.id.messageInput);
        exportButton = findViewById(R.id.exportButton);
        messageList = findViewById(R.id.messageList);
        commandInput = findViewById(R.id.commandInput);
        executeCommandButton = findViewById(R.id.executeCommandButton);
        statusText = findViewById(R.id.statusText); // Add a TextView in XML for this

        executeCommandButton.setOnClickListener(v -> {
            String command = commandInput.getText().toString().trim().toLowerCase();
            handleCommand(command);
        });

        intervalInput = findViewById(R.id.intervalInput);
        messages = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(adapter);

        requestPermissions();
//        choiceInput.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                String choice = s.toString().trim().toLowerCase();
//                if (numberMap.containsKey(choice)) {
//                    Pair<String, String> pair = numberMap.get(choice);
//                    senderNumber=pair.second;  // sets target number
//                    targetNumber=pair.first;   // sets expected sender (responder)
//                    Log.d("sendernumber", "Sending to targetNumber: " + senderNumber);
//                    Log.d("targetNumber", "Sending to targetNumber: " + targetNumber);
//                    Toast.makeText(MainActivity.this, "Set recipient/responder for: " + choice, Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//        sendButton.setOnClickListener(v -> {
//            String intervalStr = intervalInput.getText().toString();
//                    if (intervalStr.isEmpty()) {
//                        Toast.makeText(MainActivity.this, "Enter interval!", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    long intervalMillis = Long.parseLong(intervalStr) * 1000 * 60; // convert seconds to m
//                    startSending(); // your existing function
//                });
//        stopButton.setOnClickListener(v -> stopSending());
        exportButton.setOnClickListener(v -> MessageExporter.exportToExcel(MainActivity.this, messages));
    }
    void handleCommand(String command) {
        String intervalStr = intervalInput.getText().toString();
        String msg = messageInput.getText().toString();

            if (command.startsWith("start ")) {
                String key = command.substring(6).trim();
                if (numberMap.containsKey(key)) {
                    startSendingForKey(key);

                    updateStatusView();
                } else {
                    Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
                }
            } else if (command.startsWith("stop ")) {
                String key = command.substring(5).trim();
                stopSendingForKey(key);

                updateStatusView();
            } else {
                Toast.makeText(this, "Unknown command", Toast.LENGTH_SHORT).show();
            }

    }
    private void updateStatusView() {
        String text = "Running: " + (activeKeys.isEmpty() ? "None" : String.join(", ", activeKeys));
        statusText.setText(text);
    }

    void startSendingForKey(String key) {
        if (runnableMap.containsKey(key)) {
            Toast.makeText(this, "Already sending for " + key, Toast.LENGTH_SHORT).show();
            return;
        }

        Pair<String, String> pair = numberMap.get(key);
        String target = pair.first;
        String msg = messageInput.getText().toString().trim();
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
            Handler handler = handlerMap.get(key);
            Runnable runnable = runnableMap.get(key);
            handler.removeCallbacks(runnable);
            runnableMap.remove(key);
            handlerMap.remove(key);
            activeKeys.remove(key);
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
