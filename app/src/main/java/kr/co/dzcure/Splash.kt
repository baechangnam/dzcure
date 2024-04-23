package kr.co.dzcure;

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat

import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase


class Splash : Activity() {
   lateinit var context : Context
   var deepLink =""


   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.splash)
      context = this

      var channelId = getString(R.string.default_notification_channel_id)
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      // Since android Oreo notification channel is needed.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val channel = NotificationChannel(
            channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT
         )
         val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
         notificationManager.createNotificationChannel(channel)
      }


      Firebase.dynamicLinks
         .getDynamicLink(intent)
         .addOnSuccessListener(this) { pendingDynamicLinkData ->
            // Get deep link from result (may be null if no link is found)
            var deepLink: Uri? = null
            if (pendingDynamicLinkData != null) {
               deepLink = pendingDynamicLinkData.link
            }
            deepLink?.let {
               this.deepLink = it.toString()
               Log.e("deepLink", this.deepLink)
            }

            // Handle the deep link. For example, open the linked
            // content, or apply promotional credit to the user's
            // account.
            // ...

            // ...
         }
         .addOnFailureListener(this) { e -> Log.w("ERROR", "getDynamicLink:onFailure", e) }

      Handler(mainLooper).postDelayed({
         val intent = Intent(context, MainActivity::class.java)
         intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION)
         if (deepLink!="") intent.putExtra("url",deepLink)
         startActivity(intent)
         finish()
      },1500)
   }
   fun getVersionInfo() : String {
      return context.packageManager.getPackageInfo(context.packageName, 0).versionName
   }


   fun update(url:String){
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      startActivity(browserIntent)
   }
   fun update(url:String,isFlowRun:Boolean){
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      startActivityForResult(browserIntent,900)
   }
   fun permissionCheck(): Boolean {
      return if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) !== PackageManager.PERMISSION_GRANTED ||
         ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED||
         ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) !== PackageManager.PERMISSION_GRANTED
      ) {
         ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA),
            112
         )
         false
      } else {
         true
      }
   }



   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      if (requestCode==900) {
         val intent = Intent(context, MainActivity::class.java)
          intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION)
         if (deepLink!="") intent.putExtra("url",deepLink)
         startActivityForResult(intent,900)
         finish()
      }
   }
}