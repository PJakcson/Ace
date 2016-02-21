/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aceft.PlayerActivity;
import com.aceft.R;
import com.aceft.data.CircleTransform;
import com.aceft.data.LayoutTasks;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchNetworkTasks;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Random;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        String channel = data.getString("channel");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);


        sendNotification(message, channel);

//        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
//        int silentHourStart = 24;
//        int silentHourEnd = 8;
//
//        if (silentHourEnd > silentHourStart) {
//            if (hour <= silentHourStart || hour >= silentHourEnd)
//
//        } else {
//            if (hour <= silentHourStart && hour >= silentHourEnd)
//                sendNotification(message, channel);
//        }

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message, String channel) {
        Random rand = new Random();

        int randomNum = rand.nextInt(10000);

        Context context = getApplicationContext();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        if (!pref.getBoolean(Preferences.BOOL_NOTIFICATIONS_ACTIVE, true))
            return;

        String fromString = pref.getString(Preferences.STRING_SILENT_FROM, "23:00");
        String untilString = pref.getString(Preferences.STRING_SILENT_UNTIL, "09:00");

        int[] from = LayoutTasks.stringTimeToInt(fromString);
        int[] until = LayoutTasks.stringTimeToInt(untilString);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);

        if (from == null || until == null)
            return;

        if (pref.getBoolean(Preferences.IS_PRO_USER, false) && pref.getBoolean(Preferences.BOOL_SILENT_ACTIVE, true) && LayoutTasks.timeBetween(from[0], from[1], until[0], until[1], hour, min))
            return;

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel", channel);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, randomNum /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        JSONObject j = TwitchNetworkTasks.downloadJSONData("https://api.twitch.tv/kraken/streams/" + channel);
        String logo = "";
        try {
            if (j != null)
                logo = j.getJSONObject("stream").getJSONObject("channel").getString("logo");
        } catch (Exception ignored) {
        }

        Bitmap b = TwitchNetworkTasks.downloadBitmap(logo);
        if (b == null)
            b = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_placeholder);

        Resources r = getResources();
        int size = r.getDimensionPixelSize(R.dimen.notification_large_icon_height);
        b = Bitmap.createScaledBitmap(b, size, size, false);

        if (Build.VERSION.SDK_INT >= 21)
            b = new CircleTransform().transform(b);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setLargeIcon(b)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(channel)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(randomNum, notificationBuilder.getNotification());
    }
}
