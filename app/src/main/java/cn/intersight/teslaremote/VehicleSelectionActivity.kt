package cn.intersight.teslaremote


import android.app.Activity
import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import org.json.JSONObject

class VehicleSelectionActivity : AppCompatActivity() {

    companion object {
        val CODE_OPTIONS: HashMap<String, String> = hashMapOf(
            "MDLS" to "Model S",
            "MDLX" to "Model X",
            "MDL3" to "Model 3",
            "BT37" to "75",
            "BT40" to "40",
            "BT60" to "60",
            "BT70" to "70",
            "BT85" to "85",
            "BTX4" to "90",
            "BTX5" to "75",
            "BTX6" to "100",
            "BTX7" to "75",
            "BTX8" to "85",
            "DV4W" to "D"
        )
        var vehicleList: RecyclerView? = null
    }

    //地址列表适配器
    private class VehicleAdapter(val context: Context, private val vehicleListResponse: List<Response>) :
        RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

        class VehicleViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): VehicleViewHolder {
            val v = LayoutInflater.from(p0.context).inflate(R.layout.vehicle_list_item, p0, false)
            return VehicleViewHolder(v)
        }

        override fun getItemCount() = vehicleListResponse.size

        override fun onBindViewHolder(p0: VehicleViewHolder, p1: Int) {
            p0.view.findViewById<TextView>(R.id.vehicle_model).text =
                    getModelFromOption(vehicleListResponse[p1].option_codes)
            p0.view.findViewById<TextView>(R.id.vehicle_name).text = vehicleListResponse[p1].display_name
            p0.view.findViewById<TextView>(R.id.vehicle_vin).text = "VIN: ${vehicleListResponse[p1].vin}"

            val sharedPref = context.getSharedPreferences(
                context.getString(R.string.user_data), Context.MODE_PRIVATE
            )
            val checkIcon = p0.view.findViewById<ImageView>(R.id.vehicle_selected)
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")
            val card = p0.view.findViewById<CoordinatorLayout>(R.id.vehicle_card)
            val button = (context as Activity).findViewById<MaterialButton>(R.id.create_widget)
            if (vehicleListResponse[p1].id == id) {
                checkIcon.visibility = View.VISIBLE
                card.elevation = 0f
                button.isClickable = true
            }


            card.setOnClickListener {
                if (checkIcon.visibility == View.INVISIBLE) {
                    with(sharedPref.edit()) {
                        putString(context.getString(R.string.user_vehicle_id), vehicleListResponse[p1].id)
                        apply()
                    }
                    vehicleList = context.findViewById(R.id.vehicle_selection_list)
                    for ( i in 0 until vehicleList!!.adapter!!.itemCount) {
                    val holder = vehicleList?.findViewHolderForLayoutPosition(i) as VehicleViewHolder
                    holder.view.findViewById<ImageView>(R.id.vehicle_selected).visibility = View.INVISIBLE
                    }

                    val dialog = LoginActivity().showLoading(context, context.getString(R.string.synchronizing))
                    whiteVehicleData(context,dialog,checkIcon,button)

                }
            }
        }

        private fun whiteVehicleData(context: Context,dialog: Dialog, checkIcon: ImageView?, button: MaterialButton?) {
            val sharedPref =
                context.getSharedPreferences(context.getString(R.string.user_data), Context.MODE_PRIVATE)
            val token = sharedPref.getString(context.getString(R.string.user_token), "")!!
            val id = sharedPref.getString(context.getString(R.string.user_vehicle_id), "")!!

            val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/vehicle_data"
            val gson = Gson()
            val queue = Volley.newRequestQueue(context)
            val jsonObjectRequest = object : JsonObjectRequest(Request.Method.GET, url, null,
                com.android.volley.Response.Listener<JSONObject> { response ->
                    Log.d("MYTEST", "已经获取到大数据！")

                    with(sharedPref.edit()) {
                        putString(context.getString(R.string.user_vehicle_data), response.toString())
                        apply()
                    }

                    dialog.dismiss()

                    checkIcon?.visibility = View.VISIBLE
                    button?.isClickable = true

                }, com.android.volley.Response.ErrorListener { error ->
                    run {
                        val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                        val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                        val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                        text.text = context.getString(R.string.connection_error)
                        icError.visibility = View.VISIBLE
                        progress.visibility = View.INVISIBLE
                        Handler().postDelayed({ dialog.dismiss() },2000)

                    }
                }) {
                override fun getHeaders(): HashMap<String, String> {
                    val header = HashMap<String, String>()
                    header["user-agent"] = context.getString(R.string.app_name)
                    header["Authorization"] = "Bearer $token"
                    return header
                }
            }
            queue.add(jsonObjectRequest)

        }

        private fun getModelFromOption(option_codes: String): String {
            var model = ""
            val options = option_codes.split(",")
            var battery = ""
            var drive = ""
            for (code in options) {
                if (code.contains("MDL")) model = CODE_OPTIONS[code]!!
                if (code.contains("BT")) battery = CODE_OPTIONS[code]!!
                if (code.contains("DV")) drive = CODE_OPTIONS[code]!!
            }
            return "$model $battery$drive"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_selection)

        val mSwipeLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)
        mSwipeLayout.setOnRefreshListener {
            val dialog = LoginActivity().showLoading(this, getString(R.string.synchronizing))
            getVehicleList(dialog)
        }

        val dialog = LoginActivity().showLoading(this, getString(R.string.synchronizing))
        getVehicleList(dialog)
    }

    fun onClick(view: View) {
        when(view.id) {
            R.id.create_widget -> {
                Log.d("MYTEST","开始创建微件")
                val mAppWidgetManager = getSystemService(AppWidgetManager::class.java)
                val myProvider = ComponentName(this, WidgetPanel::class.java)
                if (mAppWidgetManager.isRequestPinAppWidgetSupported) {
                    mAppWidgetManager.requestPinAppWidget(myProvider,null,null)
                }
                this.finish()
            }
            R.id.change_account -> {

                AlertDialog.Builder(this).apply {
                    setTitle(R.string.change_account_title)
                    setMessage(R.string.change_account_message)
                    setPositiveButton(R.string.confirm) { dialog, which ->
                        run {
                            dialog.dismiss()
                            resetAccount()
                        }
                    }
                    setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
                    show()
                }
            }
        }
    }

    private fun resetAccount() {
        val sharedPref = getSharedPreferences(getString(R.string.user_data), Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(getString(R.string.user_vehicle_id), "")
            putString(getString(R.string.user_token),"")
            apply()
        }
        val intent = Intent(this,SplashActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    private fun getVehicleList(dialog: Dialog) {
        val sharedPref = getSharedPreferences(getString(R.string.user_data), Context.MODE_PRIVATE)
        val token = sharedPref.getString(getString(R.string.user_token), "")!!

        val url = "https://owner-api.teslamotors.com/api/1/vehicles"
        val gson = Gson()
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            com.android.volley.Response.Listener<JSONObject> { response ->
                val info = gson.fromJson(response.toString(), VehicleInfo::class.java)
                val vehicleInfoList = info.response
                val viewManager = LinearLayoutManager(this)
                val viewAdapter = VehicleAdapter(this, vehicleInfoList)
                vehicleList = findViewById(R.id.vehicle_selection_list)
                vehicleList!!.apply {
                    hasFixedSize()
                    layoutManager = viewManager
                    adapter = viewAdapter
                }
                dialog.dismiss()
                findViewById<SwipeRefreshLayout>(R.id.swipe_container).isRefreshing = false

            }, com.android.volley.Response.ErrorListener { error ->
                run {
                    val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                    val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                    val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                    text.text = getString(R.string.connection_error)
                    icError.visibility = View.VISIBLE
                    progress.visibility = View.INVISIBLE
                    Handler().postDelayed({ dialog.dismiss() },2000)

                }
            }) {
            override fun getHeaders(): HashMap<String, String> {
                val header = HashMap<String, String>()
                header["Authorization"] = "Bearer $token"
                return header
            }
        }
        queue.add(jsonObjectRequest)
    }
}
