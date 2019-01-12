package cn.intersight.teslaremote


import android.app.*
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import kotlin.math.roundToInt

class WidgetPanel : AppWidgetProvider() {

    class WidgetUpdateService : JobIntentService() {

        companion object {
            private const val JOB_ID = 13720

            fun addWork(context: Context, work: Intent) {
                enqueueWork(context, WidgetUpdateService::class.java, JOB_ID, work)
            }

        }

        override fun onDestroy() {
            Log.d("MYTEST", "执行任务完成！")
            super.onDestroy()
        }

        override fun onHandleWork(intent: Intent) {

            Log.d("MYTEST", "执行请求:${intent.action}")
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val thisAppWidget = ComponentName(this.packageName, WidgetPanel::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            if (intent.action == ACTION_UPDATE_WIDGET || intent.action == ACTION_KEY) {
                //不需要唤醒的操作
                when (intent.action) {
                    ACTION_UPDATE_WIDGET -> updateWidget(this, appWidgetManager, appWidgetIds)
                    ACTION_KEY -> commandKey(this)
                }

            } else {
                //需要唤醒的操作
                val sharedPref =
                    this.getSharedPreferences(this.getString(R.string.user_data), Context.MODE_PRIVATE)
                val token = sharedPref.getString(this.getString(R.string.user_token), "")!!
                val id = sharedPref.getString(this.getString(R.string.user_vehicle_id), "")!!

                Log.d("MYTEST", "token:$token")
                Log.d("MYTEST", "id:$id")

                //设置唤醒状态
                Log.d("MYTEST", "开始唤醒！")

                setWakeUpVehicle(this, appWidgetManager, appWidgetIds)

                val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/wake_up"

                val queue = Volley.newRequestQueue(this)
                val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, url, null,
                    com.android.volley.Response.Listener<JSONObject> { response ->
                        Log.d("MYTEST", "唤醒成功！")

                        //进行任务分发
                        when (intent.action) {
                            ACTION_CLIMATE -> commandFan(this, appWidgetManager, appWidgetIds)
                            ACTION_LOCK -> commandLock(this, appWidgetManager, appWidgetIds)
                            ACTION_KEY -> commandKey(this)
                            ACTION_SPEAKER -> commandSpeaker(this, appWidgetManager, appWidgetIds)
                        }

                    }, com.android.volley.Response.ErrorListener { error ->
                        run {
                            WidgetPanel().showNotification(this, "唤醒失败：${error.localizedMessage}")
                            setOfflineState(this, appWidgetManager, appWidgetIds)
                        }
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

        private fun setWakeUpVehicle(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            setOfflineState(context, appWidgetManager, appWidgetIds)
            val views = RemoteViews(context.packageName, R.layout.widget_panel)
            views.setImageViewResource(R.id.icon_state, R.drawable.png_waking)

            views.setTextViewText(R.id.title_name, getString(R.string.waking_hint))

            views.setInt(R.id.button_fan, "setBackgroundResource", R.drawable.button_fan_offline)
            views.setTextColor(R.id.button_fan, Color.WHITE)
            views.setCharSequence(R.id.button_fan, "setText", "--.-°")
            views.setInt(R.id.button_lock, "setBackgroundResource", R.drawable.button_lock_offline)
            views.setInt(R.id.button_key, "setBackgroundResource", R.drawable.button_key_offline)
            views.setInt(R.id.button_nav, "setBackgroundResource", R.drawable.button_nav_offline)
            views.setInt(R.id.button_speaker, "setBackgroundResource", R.drawable.button_speaker_offline)
            views.setBoolean(R.id.button_fan, "setEnabled", false)
            views.setBoolean(R.id.button_lock, "setEnabled", false)
            views.setBoolean(R.id.button_key, "setEnabled", false)
            views.setBoolean(R.id.button_nav, "setEnabled", false)
            views.setBoolean(R.id.button_speaker, "setEnabled", false)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(action, null, context, WidgetPanel::class.java)
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            Log.d("MYTEST", "开始运行更新流程！")


            if (setLoadingState(context, appWidgetManager, appWidgetIds)) {
                Log.d("MYTEST", "开始检测数据可用性！")
                val sharedPref =
                    context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
                val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
                val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!

                if (token == "") {

                    setOfflineState(context, appWidgetManager, appWidgetIds)
                    val intent = Intent(this, SplashActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    return
                }
                if (id == "") {
                    setOfflineState(context, appWidgetManager, appWidgetIds)
                    val intent = Intent(this, VehicleSelectionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    return
                }
                requestInfo(context, appWidgetManager, appWidgetIds)
            } else {
                Log.d("MYTEST", "更新失败！")
                setOfflineState(context, appWidgetManager, appWidgetIds)
            }

        }

        private fun setOfflineState(
            context: Context,
            appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
        ) {

            Log.d("MYTEST", "设置成离线模式！")

            val gson = Gson()
            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val myTeslaString = sharedPref.getString(context.getString(R.string.user_vehicle_data), "")!!
            if (myTeslaString != "") {

                val myTesla = gson.fromJson(myTeslaString, TeslaVehicle::class.java)

                val views = RemoteViews(context.packageName, R.layout.widget_panel)

                views.setTextViewText(R.id.title_name, getString(R.string.off_line))

                views.setImageViewResource(R.id.icon_state, R.drawable.png_offline)
                views.setViewVisibility(R.id.icon_state, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.icon_state, getPendingSelfIntent(context, ACTION_UPDATE_WIDGET))
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }

        private fun setLoadingState(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ): Boolean {

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_panel)

            views.setTextViewText(R.id.title_name, getString(R.string.synchronizing))

            //重置按钮至不可读
            views.setImageViewResource(R.id.icon_state, R.drawable.png_sending)

            views.setInt(R.id.button_fan, "setBackgroundResource", R.drawable.button_fan_offline)
            views.setTextColor(R.id.button_fan, Color.WHITE)
            views.setCharSequence(R.id.button_fan, "setText", "--.-°")
            views.setInt(R.id.button_lock, "setBackgroundResource", R.drawable.button_lock_offline)
            views.setInt(R.id.button_key, "setBackgroundResource", R.drawable.button_key_offline)
            views.setInt(R.id.button_nav, "setBackgroundResource", R.drawable.button_nav_offline)
            views.setInt(R.id.button_speaker, "setBackgroundResource", R.drawable.button_speaker_offline)
            views.setBoolean(R.id.button_fan, "setEnabled", false)
            views.setBoolean(R.id.button_lock, "setEnabled", false)
            views.setBoolean(R.id.button_key, "setEnabled", false)
            views.setBoolean(R.id.button_nav, "setEnabled", false)
            views.setBoolean(R.id.button_speaker, "setEnabled", false)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
            return true
        }

        private fun requestInfo(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            Log.d("MYTEST", "开始获取车辆最新数据")
            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!

            val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/vehicle_data"
            val gson = Gson()
            val queue = Volley.newRequestQueue(context)
            val jsonObjectRequest = object : JsonObjectRequest(Request.Method.GET, url, null,
                com.android.volley.Response.Listener<JSONObject> { response ->
                    Log.d("MYTEST", "获取成功，开始写入！")

                    with(sharedPref.edit()) {
                        putString(getString(R.string.user_vehicle_data), response.toString())
                        apply()
                    }

//                    myTesla = gson.fromJson(response.toString(), TeslaVehicle::class.java)
                    updateView(context, appWidgetManager, appWidgetIds)

                }, com.android.volley.Response.ErrorListener { error ->
                    run {
                        WidgetPanel().showNotification(context, error)
                        setOfflineState(context, appWidgetManager, appWidgetIds)
                    }
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

        private fun updateView(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            Log.d("MYTEST", "开始更新视图")

            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryISO = telephonyManager.networkCountryIso

            val gson = Gson()
            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val myTeslaString = sharedPref.getString(context.getString(R.string.user_vehicle_data), "")!!
            if (myTeslaString != "") {

                val myTesla = gson.fromJson(myTeslaString, TeslaVehicle::class.java)

                for (appWidgetId in appWidgetIds) {
                    // Construct the RemoteViews object
                    val views = RemoteViews(context.packageName, R.layout.widget_panel)

                    //设置车辆名称
                    views.setTextViewText(R.id.title_name, myTesla.response.display_name)

                    //设置在线状态
                    views.setViewVisibility(R.id.icon_state, View.VISIBLE)

                    views.setOnClickPendingIntent(R.id.icon_state, getPendingSelfIntent(context, ACTION_UPDATE_WIDGET))

                    when (myTesla.response.state) {
                        "online" -> views.setImageViewResource(R.id.icon_state, R.drawable.png_online)
                        "offline" -> views.setImageViewResource(R.id.icon_state, R.drawable.png_offline)
                    }

                    //获取剩余里程
                    val estRange = 1.609344f * myTesla.response.charge_state.ideal_battery_range.toFloat()

                    Log.d("MYTEST", "剩余里程：$estRange km")

                    views.setTextViewText(
                        R.id.text_range, estRange.roundToInt().toString()
                    )

                    //获取电量百分比
                    when (myTesla.response.charge_state.battery_level) {
                        in 1..9 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_0)
                        in 10..19 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_10)
                        in 20..29 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_20)
                        in 30..39 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_30)
                        in 40..49 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_40)
                        in 50..59 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_50)
                        in 60..69 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_60)
                        in 70..79 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_70)
                        in 80..89 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_80)
                        in 90..90 -> views.setImageViewResource(R.id.icon_battery, R.drawable.png_battery_90)
                    }

                    //获取空调信息
                    //获取室内温度
                    val temp: String? = myTesla.response.climate_state.inside_temp

                    //判断空调是否打开
                    views.setTextColor(R.id.button_fan, Color.BLACK)
                    views.setBoolean(R.id.button_fan, "setEnabled", true)
                    views.setOnClickPendingIntent(R.id.button_fan, getPendingSelfIntent(context, ACTION_CLIMATE))
                    when (myTesla.response.climate_state.is_climate_on) {
                        "true" -> {
                            views.setTextColor(R.id.button_fan, Color.WHITE)
                            views.setCharSequence(
                                R.id.button_fan, "setText",
                                "${myTesla.response.climate_state.driver_temp_setting}°"
                            )
                            if (temp != null && myTesla.response.climate_state.driver_temp_setting >= temp.toDouble())
                                views.setInt(R.id.button_fan, "setBackgroundResource", R.drawable.button_fan_warm)
                            else views.setInt(R.id.button_fan, "setBackgroundResource", R.drawable.button_fan_cold)
                        }
                        "false" -> {
                            views.setInt(R.id.button_fan, "setBackgroundResource", R.drawable.button_fan)
                            if (temp == null)
                                views.setCharSequence(R.id.button_fan, "setText", "--.-°")
                            else views.setCharSequence(R.id.button_fan, "setText", "$temp°")
                        }
                    }

                    //判断车锁
                    views.setBoolean(R.id.button_lock, "setEnabled", true)
                    views.setOnClickPendingIntent(R.id.button_lock, getPendingSelfIntent(context, ACTION_LOCK))
                    when (myTesla.response.vehicle_state.locked) {
                        "true" -> views.setInt(R.id.button_lock, "setBackgroundResource", R.drawable.button_lock)
                        "false" -> views.setInt(R.id.button_lock, "setBackgroundResource", R.drawable.button_unlock)
                    }

                    //判断远程驾驶
                    views.setBoolean(R.id.button_key, "setEnabled", true)
                    views.setOnClickPendingIntent(R.id.button_key, getPendingSelfIntent(context, ACTION_KEY))

                    when (myTesla.response.vehicle_state.remote_start) {
                        "true" -> views.setInt(R.id.button_key, "setBackgroundResource", R.drawable.button_nokey)
                        "false" -> views.setInt(R.id.button_key, "setBackgroundResource", R.drawable.button_key)
                    }

                    //设置导航与鸣笛

                    if (countryISO != "cn") {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setPackage("com.google.android.apps.maps")
                        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                        views.setOnClickPendingIntent(R.id.button_nav, pendingIntent)
                    } else {
                        val intent = Intent(context, NavShareActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                        views.setOnClickPendingIntent(R.id.button_nav, pendingIntent)
                    }


                    views.setOnClickPendingIntent(R.id.button_speaker, getPendingSelfIntent(context, ACTION_SPEAKER))

                    views.setBoolean(R.id.button_nav, "setEnabled", true)
                    views.setBoolean(R.id.button_speaker, "setEnabled", true)
                    views.setInt(R.id.button_nav, "setBackgroundResource", R.drawable.button_nav)
                    views.setInt(R.id.button_speaker, "setBackgroundResource", R.drawable.button_speaker)

                    appWidgetManager.updateAppWidget(appWidgetId, views)

                }
            }
        }

        private fun commandFan(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

            setLoadingState(context, appWidgetManager, appWidgetIds)

            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!
            val myTeslaString = sharedPref.getString(context.getString(R.string.user_vehicle_data), "")!!
            val gson = Gson()

            if (myTeslaString != "") {

                val myTesla = gson.fromJson(myTeslaString, TeslaVehicle::class.java)


                Log.d("MYTEST", "执行空调命令，ID为：$id")
                val views = RemoteViews(context.packageName, R.layout.widget_panel)
                views.setTextViewText(R.id.title_name, getString(R.string.climate_command))


                var url = ""
                when (myTesla.response.climate_state.is_climate_on) {
                    "true" -> url =
                            "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/auto_conditioning_stop"
                    "false" -> url =
                            "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/auto_conditioning_start"
                }
                val queue = Volley.newRequestQueue(context)
                val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, url, null,
                    com.android.volley.Response.Listener<JSONObject> { response ->
                        val info = gson.fromJson(response.toString(), CommandResult::class.java)

                        Log.d("MYTEST", "空调操作完毕，info:${info.result}")
                        requestInfo(context, appWidgetManager, appWidgetIds)

                    }, com.android.volley.Response.ErrorListener { error ->
                        WidgetPanel().showNotification(context, error)
                        setOfflineState(context, appWidgetManager, appWidgetIds)
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

        private fun commandLock(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

            setLoadingState(context, appWidgetManager, appWidgetIds)

            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!
            val myTeslaString = sharedPref.getString(context.getString(R.string.user_vehicle_data), "")!!
            val gson = Gson()

            if (myTeslaString != "") {

                Log.d("MYTEST", "执行门锁命令，ID为：$id")

                val myTesla = gson.fromJson(myTeslaString, TeslaVehicle::class.java)

                val url = if (myTesla.response.vehicle_state.locked == "false")
                    "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/door_lock"
                else "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/door_unlock"
                val gson = Gson()
                val queue = Volley.newRequestQueue(context)
                val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, url, null,
                    com.android.volley.Response.Listener<JSONObject> { response ->

                        val info = gson.fromJson(response.toString(), CommandResult::class.java)
                        Log.d("MYTEST", "info:${info.reason}")
                        Log.d("MYTEST", "info:${info.result}")
                        requestInfo(context, appWidgetManager, appWidgetIds)

                    }, com.android.volley.Response.ErrorListener { error ->
                        WidgetPanel().showNotification(context, error)
                        setOfflineState(context, appWidgetManager, appWidgetIds)
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

        private fun commandKey(context: Context) {
            val intent = Intent(context, RemoteKeyActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        private fun commandSpeaker(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            setLoadingState(context, appWidgetManager, appWidgetIds)

            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!
            val myTeslaString = sharedPref.getString(context.getString(R.string.user_vehicle_data), "")!!
            val gson = Gson()

            if (myTeslaString != "") {

                val myTesla = gson.fromJson(myTeslaString, TeslaVehicle::class.java)


                Log.d("MYTEST", "执行鸣笛命令，ID为：$id")

                val url = "https://owner-api.teslamotors.com//api/1/vehicles/$id/command/honk_horn"

                val queue = Volley.newRequestQueue(context)
                val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, url, null,
                    com.android.volley.Response.Listener<JSONObject> { response ->
                        val info = gson.fromJson(response.toString(), CommandResult::class.java)

                        Log.d("MYTEST", "执行命令成功！info:${info.result}")
                        requestInfo(context, appWidgetManager, appWidgetIds)

                    }, com.android.volley.Response.ErrorListener { error ->
                        WidgetPanel().showNotification(context, error)
                        setOfflineState(context, appWidgetManager, appWidgetIds)
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

        private fun commandNav(context: Context) {}

    }


    fun showNotification(context: Context, e: Any?) {
        val notification = NotificationCompat.Builder(context, "channel_01")
            .setSmallIcon(R.drawable.ic_tesla_error)
            .setContentTitle("错误信息")
            .setContentText(e.toString())
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(1, notification.build())
        }

    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MYTEST", "接受到意图：$intent")

        WidgetUpdateService.addWork(context, intent)

        //收到系统广播，就另外分发
        super.onReceive(context, intent)

    }


    companion object {
        private const val ACTION_REQUEST_ID = "cn.intersight.teslaremote.action.request_id"
        private const val ACTION_UPDATE_WIDGET = "android.appwidget.action.APPWIDGET_UPDATE"
        private const val ACTION_REQUEST_INFO = "cn.intersight.teslaremote.action.request_info"
        private const val ACTION_UPDATE_VIEW = "cn.intersight.teslaremote.action.update_view"
        private const val ACTION_KEY = "cn.intersight.teslaremote.action.enable_key"
        private const val ACTION_NAV = "cn.intersight.teslaremote.action.nav_share"
        private const val ACTION_LOCK = "cn.intersight.teslaremote.action.lock"
        private const val ACTION_SPEAKER = "cn.intersight.teslaremote.action.speaker"
        private const val ACTION_CLIMATE = "cn.intersight.teslaremote.action.climate"
    }
}

