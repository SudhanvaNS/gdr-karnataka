package com.example.myapplication;
//package com.example.sms;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import android.util.Log;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MessageExporter {

    public static void exportToExcel(Context context, List<String> messages) {
        File file = new File(context.getExternalFilesDir(null), "Messages.csv");
//        Log.d("Export", "Total messages: " + messages.size());
        try (FileWriter writer = new FileWriter(file)) {
            for (String msg : messages) {
                // Split message into 10-char chunks
                for (int i = 0; i < msg.length(); i += 55) {
                    writer.append(msg.substring(i, Math.min(i + 10, msg.length())));
                    writer.append(',');
                }
                writer.append('\n');
            }
            Toast.makeText(context, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.e("Export", "Export done: " + file.getAbsolutePath());

        } catch (IOException e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Export", "Export failed: " + e.getMessage());
            Log.e("Export", "Export failed: " + e.getMessage());
            Log.e("Export", "Export failed: " + e.getMessage());

        }
    }
}
