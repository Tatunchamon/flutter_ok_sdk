package com.example.flutter_ok_sdk


import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import org.json.JSONException
import org.json.JSONObject
import ru.ok.android.sdk.Odnoklassniki
import ru.ok.android.sdk.OkListener
import ru.ok.android.sdk.util.OkAuthType
import ru.ok.android.sdk.util.OkScope

class FlutterOkSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

    internal var methodChannelResult: Result? = null
    private lateinit var channel: MethodChannel

    private lateinit var okLoginManager: Odnoklassniki
    private lateinit var context: Context
    private lateinit var activity: Activity

    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity;
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_ok_sdk")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "login") {
            methodChannelResult = result

            okLoginManager = Odnoklassniki.createInstance(
                    context,
                    getResourceFromContext(context, "ok_sdk_app_id"),
                    getResourceFromContext(context, "ok_sdk_app_key")
            )

            okLoginManager.requestAuthorization(activity,
                    getResourceFromContext(context, "ok_redirect_url"),
                    OkAuthType.WEBVIEW_OAUTH,
                    OkScope.VALUABLE_ACCESS
            )
//            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else {
            result.notImplemented()
        }
    }

    private fun getResourceFromContext(context: Context, resName: String): String {
        val stringRes = context.resources.getIdentifier(resName, "string", context.packageName)
        if (stringRes == 0) {
            throw IllegalArgumentException(String.format("The 'R.string.%s' value it's not defined in your project's resources file.", resName))
        }
        return context.getString(stringRes)
    }

    private val okAuthCallback = object : OkListener {
        override fun onSuccess(json: JSONObject) {
            try {
                println(json)
                val token = json.getString("access_token")
                val secretKey = json.getString("session_secret_key")
                val expires_in = json.getString("expires_in")
                val hashmap = HashMap<String, String>()
                hashmap["access_token"] = token
                hashmap["secret"] = secretKey
                hashmap["expires_in"] = expires_in
                methodChannelResult?.success(hashmap)
            } catch (exception: JSONException) {
                onError(exception.localizedMessage)
            }

        }

        override fun onError(error: String?) {
            methodChannelResult?.error("UNAVAILABLE", "OK login error", null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (::okLoginManager.isInitialized) {
            return okLoginManager.onAuthActivityResult(requestCode, resultCode, data, okAuthCallback)
        }
        return false
    }
}
