package cn.intersight.teslaremote

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.transition.ChangeImageTransform
import android.transition.Fade
import android.transition.Slide
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = Slide().setDuration(500)
        window.exitTransition = Fade().setDuration(500)
        window.sharedElementExitTransition = ChangeImageTransform().setDuration(500)
        window.sharedElementEnterTransition = ChangeImageTransform().setDuration(500)
        setContentView(R.layout.activity_login)

        checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        checkSelfPermission(android.Manifest.permission.INTERNET)
        checkSelfPermission(android.Manifest.permission.INTERNET)
        checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
        checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
        checkSelfPermission(android.Manifest.permission.WAKE_LOCK)



        val passwordView = findViewById<TextInputEditText>(R.id.user_password)
        passwordView.setOnEditorActionListener { v, _, _ ->
            val user = findViewById<TextInputEditText>(R.id.user_email).text.toString()
            val password = v.text.toString()
            loginCheck(this, user, password)
            true
        }

    }

    fun loginClick(v: View) {
        when (v.id) {
            R.id.buttonLogin -> {
                val user = findViewById<TextInputEditText>(R.id.user_email).text.toString()
                val password = findViewById<TextInputEditText>(R.id.user_password).text.toString()
                loginCheck(this, user, password)
            }
        }
    }

    private fun loginCheck(context: Context, user: String, password: String) {
        Log.d("MYTEST", user)
        Log.d("MYTEST", password)
        val dialog = showLoading(context,"DON'T PANIC")
        getToken(user, password, dialog)
    }

    private fun getToken(user: String, password: String, dialog: Dialog) {
        val url = "https://owner-api.teslamotors.com/oauth/token?grant_type=password"
        val gson = Gson()

        val mParams = HashMap<String, String>()
        mParams["grant_type"] = "password"
        mParams["client_id"] = getString(R.string.api_client_id)
        mParams["client_secret"] = getString(R.string.api_client_secret)
        mParams["email"] = user
        mParams["password"] = password

        val request = JSONObject(mParams)

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST, url, request,
            com.android.volley.Response.Listener<JSONObject> { response ->
                val authResult = gson.fromJson(response.toString(), AuthResult::class.java)
                val sharedPref = getSharedPreferences(getString(R.string.user_data), Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString(getString(R.string.user_token), authResult.access_token)
                    apply()
                }
                dialog.findViewById<TextView>(R.id.loadingTitle).text = getString(R.string.synchronizing)
                dialog.dismiss()
                val intent = Intent(this, VehicleSelectionActivity::class.java)
                startActivity(intent)
                this.finish()

            }, com.android.volley.Response.ErrorListener { error ->
                run {
                    val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                    val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                    val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                    text.text = "Error: " + error.networkResponse.statusCode
                    icError.visibility = View.VISIBLE
                    progress.visibility = View.INVISIBLE

                    Handler().postDelayed({ dialog.dismiss() },2000)
                }

            }) {
            override fun getParams(): MutableMap<String, String> {

                return mParams
            }

        }
        queue.add(jsonObjectRequest)
    }


    fun showLoading(context : Context,str: String): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_loading)
        dialog.show()
        dialog.findViewById<TextView>(R.id.loadingTitle).text = str
        return dialog
    }
}
