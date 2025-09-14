package com.example.myapplication;
//package com.example.sms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MessageExporter {

    public static void exportSmsFromNumber(Context context, String targetNumber, long startTimeMillis) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri smsUri = Uri.parse("content://sms/inbox");

        String[] projection = new String[]{"address", "body", "date"};
        String selection = "address LIKE ? AND date >= ?";
        String[] selectionArgs = new String[]{"%" + targetNumber, String.valueOf(startTimeMillis)};

        Cursor cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, "date ASC");

        if (cursor == null) {
            Toast.makeText(context, "Failed to read messages.", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(context.getExternalFilesDir(null), "Messages.csv");
        boolean fileExists = file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (!fileExists) {
                writer.append("MOB,L. Act ,CGI,VLR,IMEI,IMSI,LAT,LONG,HTTP,LBS Type,Location\n");
            }
            String latStr="" ;
            String lonStr="" ;
            final String[] locationHolder = {""};

            while (cursor.moveToNext()) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                List<String> values = parseMessageLines(body);
                Log.d("SendSMS1", "message1123321232323  " + values);
                latStr=values.get(6);
                lonStr=values.get(7);
                Log.d("location", "location in double :" + latStr + ", " + lonStr);
                if (!latStr.isEmpty() && !lonStr.isEmpty()) {
                    try {
                        Log.d("lat", "message  " + latStr);
                        Log.d("long", "message  " + lonStr);
                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);
                        Log.d("location", "location in double :" + lat + ", " + lon);
                        new Thread(() -> {
                            String location = getAreaCity(lat, lon);  // Network call safely in background

                            Log.d("Location", "Fetched: " + location);



                        }).start();
                        // Call your reverse geocoding function
                    } catch (NumberFormatException e) {
                        locationHolder[0] = "Unknown location";
                        Log.d("Location", locationHolder[0]);
                    }
                }
                values.add(locationHolder[0]);
                for (String val : values) {
                    writer.append(escapeCsv(val)).append(",");
                }


                writer.append(locationHolder[0]);
                writer.append("\n");
            }

            Toast.makeText(context, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("Export", "Exported to " + file.getAbsolutePath());

        } catch (IOException e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Export", "Export failed", e);
        } finally {
            cursor.close();
        }
    }
    private static List<String> parseMessageLines(String body) {
        List<String> values = new ArrayList<>();
        String[] lines = body.split("\n");

        for (String line : lines) {
            String[] parts = line.split(" ", 2);  // Split at first :
            if (parts.length > 1) {
                values.add(parts[1].trim());      // Take value after :
            } else {
                values.add(parts[0].trim());      // If no ":", take whole line (e.g., 'x')
            }
        }
        return values;
    }
//    String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon;
    public static String getAreaCity(double lat, double lon) {
        String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // Nominatim requires User-Agent

            Log.d("hello check", "Before API call");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            Log.d("namma check", "after API call");

            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

//            Log.d("hello check", "API call response: " + content.toString());

            JSONObject obj = new JSONObject(content.toString());
            JSONObject address = obj.getJSONObject("address");

            String suburb = address.optString("suburb", "");
            String city = address.optString("city", "");
            String state = address.optString("state", "");
            String country = address.optString("country", "");

            String result = suburb + ", " + city + ", " + state + ", " + country;
            Log.d("Location", "Parsed: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown location";
        }
    }


    private static String escapeCsv(String input) {
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            input = input.replace("\"", "\"\""); // escape inner quotes
            return "\"" + input + "\"";
        }
        return input;
    }


}
