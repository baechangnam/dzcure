package kr.co.dzcure.utils;

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.HashMap

class NetworkService : AsyncTask<String?, String?, String?> {
   var urlAddr: String
   var param: String? = null
   var callback: ApiCallback? = null

   constructor(url: String) {
      urlAddr = url
   }

   constructor(url: String, callback: ApiCallback?) {
      urlAddr = url
      this.callback = callback
   }

   fun setParameter(params: HashMap<String?, String?>) {
      val sbParams = StringBuilder()
      var i = 0
      for (key in params.keys) {
         try {
            if (i != 0) {
               sbParams.append("&")
            }
            sbParams.append(key).append("=").append(URLEncoder.encode(params[key], "UTF-8"))
         } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
         }
         i++
      }
      param = sbParams.toString()
      Log.e("Parameter", param!!)
   }

   override fun onPreExecute() {
      super.onPreExecute()
   }

   override fun onPostExecute(result: String?) {
      if (callback != null) {
         /*Log.e("Result", result);
			JSONObject JsonResult = null;
			try {
				JsonResult = new JSONObject(result);
			} catch (JSONException e) {
			}
			callback.callback(JsonResult);*/
         callback!!.callback(result)
      }
   }

   override fun doInBackground(vararg params: String?): String? {
      val result = StringBuilder()
      try {
         val url = Constant.URL + urlAddr
         Log.e("call url", url)
         val urlObj = URL(url)
         val conn = urlObj.openConnection() as HttpURLConnection
         conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
         conn.doOutput = true
         conn.requestMethod = "POST"
         conn.setRequestProperty("Accept-Charset", "UTF-8")
         conn.readTimeout = 10000
         conn.connectTimeout = 15000
         conn.connect()
         val wr = DataOutputStream(conn.outputStream)
         wr.writeBytes(param)
         wr.flush()
         wr.close()
         val reader = BufferedReader(InputStreamReader(conn.inputStream))
         var line: String?
         while (reader.readLine().also { line = it } != null) {
            result.append(line)
         }
         conn.disconnect()
      } catch (e: Exception) {
         e.printStackTrace()
      }
      return result.toString()
   }
}