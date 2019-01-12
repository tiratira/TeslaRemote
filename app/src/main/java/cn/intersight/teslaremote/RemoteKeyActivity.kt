package cn.intersight.teslaremote


import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.transition.Scene
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import org.json.JSONObject

class RemoteKeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_key)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (ViewGroup.LayoutParams.WRAP_CONTENT))
        window.setGravity(Gravity.CENTER)

        val sceneRoot: ViewGroup = findViewById(R.id.remote_container)
        val scene1 = Scene.getSceneForLayout(sceneRoot, R.layout.layout_remote_key_scene1, this)
        scene1.enter()

    }

    private class RemoteTimer(val context: Context, period: Long, tick: Long, val v: TextView) :
        CountDownTimer(period, tick) {
        override fun onFinish() {
            v.text = "0:00"

            val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
            WidgetPanel.WidgetUpdateService.addWork(context,intent)

            (context as Activity).finish()

        }

        override fun onTick(millisUntilFinished: Long) {
            val remainSecs = millisUntilFinished / 1000
            v.text =
                    "${remainSecs / 60}:${if (remainSecs % 60 < 10L) "0" + (remainSecs % 60).toString() else (remainSecs % 60).toString()}"
        }

    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.confirm_key_button -> {
                checkRemoteKey(this,findViewById<TextInputEditText>(R.id.remote_password).text.toString())
            }

            R.id.cancel_key_button -> this.finish()


            R.id.close_key_button -> {
                val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
                WidgetPanel.WidgetUpdateService.addWork(this,intent)
                this.finish()
            }
        }
    }

    private fun checkRemoteKey(context: Context, password: String) {
        if (password != "" && password.length >= 6) {
            val dialog = LoginActivity().showLoading(this, getString(R.string.waking_hint))

            val sharedPref = getSharedPreferences(getString(R.string.user_data), MODE_PRIVATE)
            val token = sharedPref.getString(getString(R.string.user_token), "")!!
            val id = sharedPref.getString(getString(R.string.user_vehicle_id), "")!!

            val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/wake_up"

            val gson = Gson()
            val queue = Volley.newRequestQueue(this)
            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST, url, null,
                com.android.volley.Response.Listener<JSONObject> { response ->

                    val info = gson.fromJson(response.toString(), CommandResult::class.java)

                    if (info.result) {
                        dialog.findViewById<TextView>(R.id.loadingTitle).text = "Remote Starting"
                        enableRemote(context, dialog, password)
                    }


                }, com.android.volley.Response.ErrorListener { error ->

                    val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                    val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                    val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                    text.text = "Error: " + error.networkResponse.statusCode
                    icError.visibility = View.VISIBLE
                    progress.visibility = View.INVISIBLE

                    Handler().postDelayed({
                        dialog.dismiss()
                    }, 2000)

                }) {
                override fun getHeaders(): HashMap<String, String> {
                    val header = HashMap<String, String>()
                    header["user-agent"] = getString(R.string.app_name)
                    header["Authorization"] = "Bearer $token"
                    return header
                }
            }
            queue.add(jsonObjectRequest)


        }
    }

    private fun enableRemote(context: Context, dialog: Dialog, password: String) {

        val sharedPref = getSharedPreferences(getString(R.string.user_data), MODE_PRIVATE)
        val token = sharedPref.getString(getString(R.string.user_token), "")!!
        val id = sharedPref.getString(getString(R.string.user_vehicle_id), "")!!

        val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/remote_start_drive"

        val jsonObject = JSONObject()
        jsonObject.put("password", password)

        val gson = Gson()
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            com.android.volley.Response.Listener<JSONObject> { response ->

                val info = gson.fromJson(response.toString(), CommandResult::class.java)

                if (info.result) {
                    dialog.findViewById<TextView>(R.id.loadingTitle).text = getString(R.string.success)
                    dialog.findViewById<ProgressBar>(R.id.progress).visibility = View.INVISIBLE
                    dialog.findViewById<ImageView>(R.id.done_icon).visibility = View.VISIBLE

                    Handler().postDelayed({
                        dialog.dismiss()
                        val sceneRoot: ViewGroup = findViewById(R.id.remote_container)
                        val scene2 = Scene.getSceneForLayout(sceneRoot, R.layout.layout_remote_key_scene2, this)
                        scene2.enter()

                        val timerView = findViewById<TextView>(R.id.timer)

                        val timer = RemoteTimer(this, 121000, 1000, timerView)
                        timer.start()
                    }, 1000)
                }


            }, com.android.volley.Response.ErrorListener { error ->

                val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                text.text = "Error: " + error.networkResponse.statusCode
                icError.visibility = View.VISIBLE
                progress.visibility = View.INVISIBLE

                Handler().postDelayed({
                    dialog.dismiss()
                }, 2000)

            }) {
            override fun getHeaders(): HashMap<String, String> {
                val header = HashMap<String, String>()
                header["user-agent"] = getString(R.string.app_name)
                header["Authorization"] = "Bearer $token"
                return header
            }
        }
        queue.add(jsonObjectRequest)
    }
}
