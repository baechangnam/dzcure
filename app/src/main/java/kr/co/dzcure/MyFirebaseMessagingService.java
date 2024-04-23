/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.co.dzcure;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import kr.co.dzcure.MainActivity;
import kr.co.dzcure.utils.CommonUtils;
import kr.co.dzcure.R;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 *
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 *
 * <intent-filter>
 *   <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // TODO(developer): Handle FCM messages here.
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Message data payload: " + remoteMessage.getData());
               // handleNow();
        }

        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            //sendNotification( remoteMessage.getNotification().getBody());
        }

        int badge = 0;
        if(!CommonUtils.isEmpty(remoteMessage.getData().get("badge")+"")){
            badge = Integer.parseInt(remoteMessage.getData().get("badge")+"");
        }
        Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        badgeIntent.putExtra("badge_count", badge);
        badgeIntent.putExtra("badge_count_package_name", getPackageName());
        badgeIntent.putExtra("badge_count_class_name", getLauncherClassName());
        sendBroadcast(badgeIntent);
        sendNotification( remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());
    }

    private String getLauncherClassName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (pkgName.equalsIgnoreCase(getPackageName())) {
                return resolveInfo.activityInfo.name;
            }
        }
        return null;
    }
    @Override
    public void onNewToken(String token) {
        Log.e(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void handleNow() {
        Log.e(TAG, "Short lived task is done.");
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    private void sendNotification(String title,String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //intent.putExtra("url",data.get("url")+"");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT) ;

        String channelId = getString(R.string.default_notification_channel_id);
        //Uri soundUri = RingtoneMana

        // ger.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
               .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
               .setUsage(AudioAttributes.USAGE_NOTIFICATION)
               .build();
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(3 /* ID of notification */, notificationBuilder.build());
    }
}