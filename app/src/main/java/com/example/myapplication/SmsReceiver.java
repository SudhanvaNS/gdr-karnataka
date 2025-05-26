package com.example.myapplication;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    // 54051
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                String sender = sms.getOriginatingAddress();
                String msg = sms.getMessageBody();
                if (sender != null && sender.contains(MainActivity.senderNumber)) {
                    Intent i = new Intent("com.example.message.NEW_SMS");
                    i.putExtra("msg", msg);
                    context.sendBroadcast(i);
                }
            }
        }
    }
}

