package kr.co.dzcure.utils;

import android.content.Context

object CommonUtils {
   @JvmStatic
   fun isEmpty(str: String?): Boolean {
      var str = str
      if (str == null) {
         return true
      }
      str = str.replace(" ".toRegex(), "")
      if ("null" == str) {
         return true
      }
      return if ("" == str) {
         true
      } else false
   }

   fun getPref(context: Context, key: String?): String? {
      val pref = context.getSharedPreferences("info", 0)
      return pref.getString(key, "")
   }

   fun setPref(context: Context, key: String?, value: String?) {
      val pref = context.getSharedPreferences("info", 0)
      val editor = pref.edit()
      editor.putString(key, value)
      editor.commit()
   }

   fun getUserAgentFakeLogin(): String? {
      return "Mozilla/5.0 AppleWebKit/535.19 Chrome/56.0.0 Mobile Safari/535.19"
      //return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
   }

   fun getUserAgentFake(): String? {
      return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/56.0.0 Safari/535.19"
      //return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
   }

   fun versionCompare(myVersion: String, diffVersion: String): Int {
      /*
      myVersion 이높음 : -1
      버전이 같음 : 0
      diffVersion 이높음 : 1
       */
      var result = 0
      try {
         val v1: String
         val v2: String
         var length = 0
         v1 = myVersion
         v2 = diffVersion
         val strv_01 = v1.split("\\.".toRegex()).toTypedArray()
         val strv_02 = v2.split("\\.".toRegex()).toTypedArray()
         val intv_01 = IntArray(strv_01.size)
         val intv_02 = IntArray(strv_02.size)
         length = Math.min(intv_01.size, intv_02.size)
         for (i in intv_01.indices) {
            intv_01[i] = strv_01[i].toInt()
         }
         for (i in intv_02.indices) {
            intv_02[i] = strv_02[i].toInt()
         }
         for (i in 0 until length) {
            if (intv_01[i] > intv_02[i]) {
               result = -1
               break
            } else if (intv_01[i] < intv_02[i]) {
               result = 1
               break
            } else if (i == length) {
               if (intv_01.size > length) {
                  result = -1
                  break
               } else if (intv_02.size > length) {
                  result = 1
                  break
               }
            } else {
               result = 0
               continue
            }
         }
      } catch (e: java.lang.Exception) {
         return 0
      }
      return result
   }
}