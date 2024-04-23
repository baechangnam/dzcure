package kr.co.dzcure;

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView.WebViewTransport
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewbinding.ViewBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kr.co.dzcure.databinding.ActivityMainBinding
import kr.co.dzcure.utils.CommonUtils
import kr.co.dzcure.utils.Constant
import kr.co.dzcure.utils.FullscreenableChromeClient
import kr.co.dzcure.utils.RealPathUtil
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity() {
   lateinit var context: Context
   var webView: WebView? = null
   var token: String? = null
   var dId: String? = null
   var currentPhotoPath:String? = null
   private lateinit var binding: ActivityMainBinding


   inner class AndroidBridge(val webView_:WebView) {
      @JavascriptInterface
      fun logout() {
         Handler(Looper.getMainLooper()).postDelayed( {
            webView_?.clearCache(true);
            webView_?.clearHistory();
            clearCookies()
         },500)
      }

      @JavascriptInterface
      fun login() {
         Handler(Looper.getMainLooper()).postDelayed( {
            webView_?.settings?.setUserAgentString(CommonUtils.getUserAgentFake())
         },500)
      }

      fun clearCookies() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
         } else {
            val cookieSyncMngr = CookieSyncManager.createInstance(context)
            cookieSyncMngr.startSync()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMngr.stopSync()
            cookieSyncMngr.sync()
         }
         webView_.reload()
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      binding = ActivityMainBinding.inflate(layoutInflater);

      context = this
      webView = findViewById<View>(R.id.webview) as WebView
      val set = webView!!.settings
      set.javaScriptEnabled = true
      set.allowFileAccess = true
      set.pluginState = WebSettings.PluginState.ON_DEMAND
      set.setSupportMultipleWindows(true)
      set.javaScriptCanOpenWindowsAutomatically = true
      set.setUserAgentString(CommonUtils.getUserAgentFakeLogin())

      if (Build.VERSION.SDK_INT >= 21) {
         webView!!.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
         val cookieManager = CookieManager.getInstance()
         cookieManager.setAcceptCookie(true)
         cookieManager.setAcceptThirdPartyCookies(webView, true)
      }
      //set.setAppCacheEnabled(true)
      set.domStorageEnabled = true

      webView!!.settings.apply {
         this.setSupportMultipleWindows(false) // 새창 띄우기 허용
         this.setSupportZoom(false) // 화면 확대 허용
         this.javaScriptEnabled = true // 자바스크립트 허용
         this.javaScriptCanOpenWindowsAutomatically = false // 자바스크립트 새창 띄우기 허용
         this.loadWithOverviewMode = true // html의 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정
         this.useWideViewPort = true // html의 viewport 메타 태그 지원
         this.builtInZoomControls = false // 화면 확대/축소 허용
         this.displayZoomControls = false
         this.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN // 컨텐츠 사이즈 맞추기
         this.cacheMode = WebSettings.LOAD_NO_CACHE // 브라우저 캐쉬 허용
         this.domStorageEnabled = true // 로컬 저장 허용
         this.databaseEnabled = true

         /**
          * This request has been blocked; the content must be served over HTTPS
          * https 에서 이미지가 표시 안되는 오류를 해결하기 위한 처리
          */
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
         }
      }

     // webView!!.webViewClient = WebViewClient()

      webView!!.webChromeClient = CustomChrome(this)
      webView!!.webViewClient = CustomWebClient()
      webView!!.addJavascriptInterface(AndroidBridge(webView!!), "AndroidBridge")

      webView!!.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
         var contentDisposition = contentDisposition
         try {
            if (ContextCompat.checkSelfPermission(
                  this@MainActivity,
                  Manifest.permission.WRITE_EXTERNAL_STORAGE
               )
               !== PackageManager.PERMISSION_GRANTED
            ) {
               // Should we show an explanation?
               if (ActivityCompat.shouldShowRequestPermissionRationale(
                     this@MainActivity,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE
                  )
               ) {
                  ActivityCompat.requestPermissions(
                     this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                     111
                  )
               } else {
                  ActivityCompat.requestPermissions(
                     this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                     111
                  )
               }
               return@DownloadListener
            }
            val request = DownloadManager.Request(Uri.parse(url))
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8")
            // 파일명 잘라내기
            var fileName = contentDisposition
            if (fileName != null && fileName.length > 0) {
               val idxFileName = fileName.indexOf("filename=")
               if (idxFileName > -1) {
                  fileName = fileName.substring(idxFileName + 9).trim { it <= ' ' }
               }
               if (fileName.endsWith(";")) {
                  fileName = fileName.substring(0, fileName.length - 1)
               }
               if (fileName.startsWith("\" ") && fileName.startsWith("\" ")) {
                  fileName = fileName.substring(1, fileName.length - 1)
               }
            }

            // 세션 유지를 위해 쿠키 세팅하기
            val cookie = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("Cookie", cookie)
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading File")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setTitle(fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
               request.setRequiresCharging(false)
            }
            request.allowScanningByMediaScanner()
            request.setAllowedOverMetered(true)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            dm.enqueue(request)
            Toast.makeText(applicationContext, "파일을 다운로드 합니다.", Toast.LENGTH_LONG).show()
         } catch (e: Exception) {
            e.printStackTrace()
         }
      })
      init(null)
   }

   override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      if (intent.hasExtra("url")) {
         init(intent.getStringExtra("url"))
      } else {
      }
   }

   fun init(_url: String?) {
      if (intent.hasExtra("url")) {
         Log.e("INTENT",intent.getStringExtra("url").toString())
         webView!!.loadUrl(intent.getStringExtra("url").toString())
         intent.removeExtra("url")
         return
      }
      FirebaseInstanceId.getInstance().instanceId
         .addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
               return@OnCompleteListener
            }
            // Get new Instance ID token
            token = task.result?.let { it.token }
            Log.e("Token", token.toString())
            /*dId = GetDevicesUUID();
                        Log.e("dId",dId);*/
            var url: String
            url = _url ?: Constant.URL

            Log.e("URL", url)
            webView!!.loadUrl(url)
         })
   }

   override fun onResume() {
      super.onResume()
      val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
      badgeIntent.putExtra("badge_count", 0)
      badgeIntent.putExtra("badge_count_package_name", packageName)
      badgeIntent.putExtra("badge_count_class_name", launcherClassName)
      sendBroadcast(badgeIntent)
   }

   private val launcherClassName: String?
      private get() {
         val intent = Intent(Intent.ACTION_MAIN)
         intent.addCategory(Intent.CATEGORY_LAUNCHER)
         val pm = applicationContext.packageManager
         val resolveInfos = pm.queryIntentActivities(intent, 0)
         for (resolveInfo in resolveInfos) {
            val pkgName = resolveInfo.activityInfo.applicationInfo.packageName
            if (pkgName.equals(packageName, ignoreCase = true)) {
               return resolveInfo.activityInfo.name
            }
         }
         return null
      }

   internal inner class CustomWebClient : WebViewClient() {
      override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
         val builder = AlertDialog.Builder(this@MainActivity)
         builder.setMessage("유효하지 않은 인증서 입니다. 계속 진행하시겠습니까?")
         builder.setPositiveButton("continue") { dialog, which -> handler.proceed() }
         builder.setNegativeButton("cancel") { dialog, which -> handler.cancel() }
         val dialog = builder.create()
         dialog.show()
      }

      override fun onReceivedError(
         view: WebView, errorCode: Int,
         description: String, failingUrl: String
      ) {
         super.onReceivedError(view, errorCode, description, failingUrl)
         when (errorCode) {
            ERROR_AUTHENTICATION, ERROR_BAD_URL, ERROR_CONNECT, ERROR_FAILED_SSL_HANDSHAKE, ERROR_FILE, ERROR_FILE_NOT_FOUND, ERROR_HOST_LOOKUP, ERROR_IO, ERROR_PROXY_AUTHENTICATION, ERROR_REDIRECT_LOOP, ERROR_TIMEOUT, ERROR_TOO_MANY_REQUESTS, ERROR_UNKNOWN, ERROR_UNSUPPORTED_AUTH_SCHEME, ERROR_UNSUPPORTED_SCHEME -> {}
         }
      }

      override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
         /* progress.setVisibility(View.VISIBLE);
            if (!url.startsWith("http://m.snowpeak.co.kr/bicon/bicon_view.html")) {
                preBeaconId1 = "";
            }*/
         super.onPageStarted(view, url, favicon)
         if (!url.startsWith("https://oauth.telegram.org/auth/push?bot_id=")){
            binding.progressBar.visibility = View.VISIBLE
         }
         //
      }

      //페이지 로딩 종료시 호출
      override fun onPageFinished(view: WebView, Url: String) {
         binding.progressBar.visibility = View.GONE
         Log.e("onPageFinished", Url)

            if ("about:blank".equals(Url) && view.getTag() != null)
            {
               view.loadUrl(view.getTag().toString());
            }
            else
            {
               view.setTag(Url);
            }


      }


      override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
         if (url != null) {
            Log.e("shouldOverrideUrl", url)
         }
         if (url != null) {
            if (url.startsWith("tel:")) {
                 val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                 //전화거는 화면까지만 이동 시킬꺼면 Intent.ACTION_DIAL
                 startActivity(intent)
                 return true
            }else{
                 if (url != null) {
                    view?.loadUrl(url)
                 }
            }
         }


         return true
      }

   }

   private var mUploadMessage: ValueCallback<Uri?>? = null
   private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
   private var mCameraPhotoPath: String? = null
   var popup: WebView? = null

   internal inner class CustomChrome(activity: Activity?) : FullscreenableChromeClient(activity) {
      override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
         super.onGeolocationPermissionsShowPrompt(origin, callback)
         callback.invoke(origin, true, false)
      }

      override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
         return super.onJsAlert(view, url, message, result)
      }

      override fun onPermissionRequest(request: PermissionRequest) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            request.grant(request.resources)
         }
      }

      override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
         webView!!.postDelayed({ webView!!.scrollTo(0, 0) }, 50)
         popup = WebView(this@MainActivity)
         val param = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
         popup!!.layoutParams = param
         view.addView(popup)
         //popup.requestFocus();
         popup!!.postDelayed({ popup!!.scrollTo(0, 0) }, 100)
         popup!!.webChromeClient = CustomChrome(this@MainActivity)
         popup!!.webViewClient = CustomWebClient()
         popup!!.addJavascriptInterface(AndroidBridge(popup!!), "AndroidBridge")
         val set = popup!!.settings
         set.javaScriptEnabled = true
         set.allowFileAccess = true
         set.pluginState = WebSettings.PluginState.ON_DEMAND
         set.javaScriptCanOpenWindowsAutomatically = true
         set.setUserAgentString(CommonUtils.getUserAgentFakeLogin())
         if (Build.VERSION.SDK_INT >= 21) {
            webView!!.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
         }
         //set.setAppCacheEnabled(true)
         set.domStorageEnabled = true
         popup!!.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            var contentDisposition = contentDisposition
            try {
               if (ContextCompat.checkSelfPermission(
                     this@MainActivity,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE
                  )
                  !== PackageManager.PERMISSION_GRANTED
               ) {
                  // Should we show an explanation?
                  if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                     )
                  ) {
                     ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        111
                     )
                  } else {
                     ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        111
                     )
                  }
                  return@DownloadListener
               }
               val request = DownloadManager.Request(Uri.parse(url))
               val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
               contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8")
               // 파일명 잘라내기
               var fileName = contentDisposition
               if (fileName != null && fileName.length > 0) {
                  val idxFileName = fileName.indexOf("filename=")
                  if (idxFileName > -1) {
                     fileName = fileName.substring(idxFileName + 9).trim { it <= ' ' }
                  }
                  if (fileName.endsWith(";")) {
                     fileName = fileName.substring(0, fileName.length - 1)
                  }
                  if (fileName.startsWith("\" ") && fileName.startsWith("\" ")) {
                     fileName = fileName.substring(1, fileName.length - 1)
                  }
               }

               // 세션 유지를 위해 쿠키 세팅하기
               val cookie = CookieManager.getInstance().getCookie(url)
               request.addRequestHeader("Cookie", cookie)
               request.setMimeType(mimeType)
               request.addRequestHeader("User-Agent", userAgent)
               request.setDescription("Downloading File")
               request.setAllowedOverMetered(true)
               request.setAllowedOverRoaming(true)
               request.setTitle(fileName)
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                  request.setRequiresCharging(false)
               }
               request.allowScanningByMediaScanner()
               request.setAllowedOverMetered(true)
               request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
               request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
               dm.enqueue(request)
               Toast.makeText(applicationContext, "파일을 다운로드 합니다.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
               e.printStackTrace()
            }
         })
         set.savePassword = false
       //  set.setAppCacheEnabled(true)

         //set.setDomStorageEnabled(true);
         val transport = resultMsg.obj as WebViewTransport
         //popup.setWebChromeClient(this);
         transport.webView = popup
         resultMsg.sendToTarget()
         return true
      }

      override fun onCloseWindow(window: WebView) {
         super.onCloseWindow(window)
         popup = null
         webView!!.removeView(window)
         window.destroy()
      }

      // For Android Version < 3.0
      fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
         //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
         mUploadMessage = uploadMsg
         val intent = Intent(Intent.ACTION_GET_CONTENT)
         intent.addCategory(Intent.CATEGORY_OPENABLE)
         intent.type = TYPE_IMAGE
         startActivityForResult(intent, INPUT_FILE_REQUEST_CODE)
      }

      // For 3.0 <= Android Version < 4.1
      fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String) {
         //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
         openFileChooser(uploadMsg, acceptType, "")
      }

      // For 4.1 <= Android Version < 5.0
      fun openFileChooser(uploadFile: ValueCallback<Uri?>?, acceptType: String, capture: String) {
         Log.d(javaClass.name, "openFileChooser : $acceptType/$capture")
         mUploadMessage = uploadFile
         //imageChooser(fileChooserParams.acceptTypes)
      }

      // For Android Version 5.0+
      // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
      override fun onShowFileChooser(
         webView: WebView,
         filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams
      ): Boolean {
         println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3")
         if (mFilePathCallback != null) {
            mFilePathCallback!!.onReceiveValue(null)
         }
         mFilePathCallback = filePathCallback
         var isCapture = fileChooserParams.isCaptureEnabled
         if (isCapture) {
            imageChooser(fileChooserParams.acceptTypes)
         } else {
            fileChooser(fileChooserParams.acceptTypes)
         }
         return true
      }

      private fun fileChooser(acceptTypes: Array<String>) {
         /*val contentSelectionIntent = Intent(Intent.ACTION_PICK)
         contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
         contentSelectionIntent.type = TYPE_IMAGE
         val chooserIntent = Intent(Intent.ACTION_CHOOSER)
         chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
         chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser")*/
         val chooserIntent = Intent(Intent.ACTION_GET_CONTENT)
         if(acceptTypes.size>0) {
            chooserIntent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
         }
         var isMedia = false
         acceptTypes.forEach {
            if (it.startsWith("image")||it.startsWith("video")){
               isMedia = true
            }
         }
         if (isMedia) chooserIntent.type = "*/*" else chooserIntent.type = "application/*"
         //chooserIntent.type = "*/*"
         startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
      }

      private fun imageChooser(acceptTypes: Array<String>) {
/*         var intent = Intent(Intent.ACTION_PICK).apply {
            type = MediaStore.Images.Media.CONTENT_TYPE
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
         }
         Intent.createChooser(intent, "Image Chooser").run {
            //putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            startActivityForResult(this, INPUT_FILE_REQUEST_CODE)
         }*/

         var type = "image/*"
         for (t in acceptTypes){
            if (t.indexOf("image")!=0)
               type = "video/*"
         }

         var state = Environment.getExternalStorageState()
         if (!TextUtils.equals(state, Environment.MEDIA_MOUNTED)) {
            return
         }

         var cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         if (type=="video/*") cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
         cameraIntent.resolveActivity(packageManager!!)?.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
               createImageFile2()
            } catch (ex: IOException) {
               // Error occurred while creating the File
               null
            }
            // Continue only if the File was successfully created
            photoFile?.also {
               val photoURI: Uri = FileProvider.getUriForFile(this@MainActivity,
                  BuildConfig.APPLICATION_ID + ".provider",
                  it
               )
               cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
         }


/*         var intent = Intent(Intent.ACTION_PICK).apply {
            //type = MediaStore.Images.Media.CONTENT_TYPE
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            putExtra(Intent.EXTRA_MIME_TYPES,acceptTypes)
         }*/

         var intent = Intent(Intent.ACTION_PICK).apply {
            if (type=="video/*") {
               type = MediaStore.Video.Media.CONTENT_TYPE
               data = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
               type = MediaStore.Images.Media.CONTENT_TYPE
               data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

         }

         Intent.createChooser(intent, "Chooser").run {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            startActivityForResult(this, INPUT_FILE_REQUEST_CODE)
         }
      }
   }

   /**
    * More info this method can be found at
    * http://developer.android.com/training/camera/photobasics.html
    *
    * @return
    * @throws IOException
    */
   @Throws(IOException::class)
   private fun createImageFile(): File {
      // Create an image file name
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
      val imageFileName = "JPEG_" + timeStamp + "_"
      val storageDir = Environment.getExternalStoragePublicDirectory(
         Environment.DIRECTORY_PICTURES
      )
      return File.createTempFile(
         imageFileName,  /* prefix */
         ".jpg",  /* suffix */
         storageDir /* directory */
      )
   }

   private fun createImageFile2(): File {
      // Create an image file name
      val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
      val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
      return File.createTempFile(
         "JPEG_${timeStamp}_", /* prefix */
         ".jpg", /* suffix */
         storageDir /* directory */
      ).apply {
         // Save a file: path for use with ACTION_VIEW intents
         currentPhotoPath = absolutePath
      }
   }


   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
         if (requestCode == 112) {
            init(null)
         }

         mFilePathCallback?.let {
            var imageData = data
            if (imageData == null) {
               imageData = Intent()
               imageData?.data = Uri.fromFile(File(currentPhotoPath))
            }
            if (imageData?.data == null) {
               imageData?.data = Uri.fromFile(File(currentPhotoPath))
            }
            it.onReceiveValue(FileChooserParams.parseResult(resultCode, imageData))
            mFilePathCallback = null
         }

/*         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mFilePathCallback == null) {
               super.onActivityResult(requestCode, resultCode, data)
               return
            }
            val results = arrayOf(getResultUri(data))
            mFilePathCallback!!.onReceiveValue(results as Array<Uri>)
            mFilePathCallback = null
         } else {
            if (mUploadMessage == null) {
               super.onActivityResult(requestCode, resultCode, data)
               return
            }
            val result = getResultUri(data)
            Log.d(javaClass.name, "openFileChooser : $result")
            mUploadMessage!!.onReceiveValue(result)
            mUploadMessage = null
         }*/
      } else {
         if (mFilePathCallback != null) mFilePathCallback!!.onReceiveValue(null)
         if (mUploadMessage != null) mUploadMessage!!.onReceiveValue(null)
         mFilePathCallback = null
         mUploadMessage = null
         super.onActivityResult(requestCode, resultCode, data)
      }
   }

   private fun getResultUri(data: Intent?): Uri? {
      var result: Uri? = null
      if (data == null || TextUtils.isEmpty(data.dataString)) {
         // If there is not data, then we may have taken a photo
         if (mCameraPhotoPath != null) {
            result = Uri.parse(mCameraPhotoPath)
         }
      } else {
         var filePath: String? = ""
         filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            data.dataString
         } else {
            "file:" + data.data?.let { RealPathUtil.getRealPath(this, it) }
         }
         result = Uri.parse(filePath)
      }
      return result
   }

   override fun onBackPressed() {
      if (popup != null) {
         //popup.goBack();
         /* webView.removeView(popup);
            popup = null;*/
         popup!!.loadUrl("javascript:window.close();")
         popup = null
         return
      }
      if (webView!!.url!!.endsWith("/main") || webView!!.url == Constant.URL || webView!!.url!!.endsWith("/main")) {
         backPress()
      } else {
         if (webView!!.canGoBack()) {
            webView!!.goBack()
         } else {
            backPress()
         }
      }
   }

   private var backKeyPressedTime: Long = 0
   fun backPress() {
      if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
         backKeyPressedTime = System.currentTimeMillis()
         showGuide()
         return
      }
      if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
         finish()
      }
   }

   fun showGuide() {
      val toast = Toast.makeText(
         this,
         "\'뒤로\'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT
      )
      toast.show()
   }

   private fun permissionCheck(): Boolean {
      return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !== PackageManager.PERMISSION_GRANTED ||
         ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED
      ) {
         ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            112
         )
         false
      } else {
         true
      }
   }

   override fun onDestroy() {
      super.onDestroy()
   }

   override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
      if (requestCode == 112) {
         init(null)
      }
   }

   companion object {
      private const val TYPE_IMAGE = "image/*"
      private const val INPUT_FILE_REQUEST_CODE = 1
   }
}