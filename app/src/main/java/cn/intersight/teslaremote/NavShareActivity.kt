package cn.intersight.teslaremote

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationListener
import android.location.LocationProvider
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.telephony.TelephonyManager
import android.text.style.CharacterStyle
import android.transition.Scene
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.transition.Visibility
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.maps.*
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.*
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.places.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import org.json.JSONObject
import java.sql.Timestamp

class NavShareActivity : AppCompatActivity() {

    companion object {
        var mMapView: MapView? = null
        var mGoogleMap: com.google.android.gms.maps.MapView? = null
        var targetTitle: String = ""
        var targetAddress: String = ""
        var poiDataset = ArrayList<PoiItem>()
        var mySavedInstanceState: Bundle? = null
        var searchViewEntered: Boolean = false
        var mMap: GoogleMap? = null
        var countryISO = ""

    }

    private lateinit var recyclerView: RecyclerView


    //地图平移侦听器
    private class MyCameraChangeListener(val context: Context) :
        AMap.OnCameraChangeListener {
        override fun onCameraChange(p0: CameraPosition?) {
        }

        override fun onCameraChangeFinish(p0: CameraPosition?) {

            if (p0 != null) {
                val query = PoiSearch.Query("", null)
                query.pageSize = 25

                val poiSearch = PoiSearch(context, query)
                poiSearch.setOnPoiSearchListener(NavShareActivity.MyPoiSearchListener(context))
                poiSearch.bound =
                        PoiSearch.SearchBound(LatLonPoint(p0.target.latitude, p0.target.longitude), 1000)
                poiSearch.searchPOIAsyn()
            }

        }
    }

    private fun commandSendNav(context: Context, name: String, addr: String, dialog: Dialog) {

        val sharedPref =
            getSharedPreferences(getString(R.string.user_data), Context.MODE_PRIVATE)
        val token = sharedPref.getString(getString(R.string.user_token), "")!!
        val id = sharedPref.getString(getString(R.string.user_vehicle_id), "")!!

        val url = "https://owner-api.teslamotors.com/api/1/vehicles/$id/command/navigation_request"
        if (name != "" && addr != "") {
            val finalAddr = "$name\\n\\nhttps://goo.gl/maps/X"

            Log.d("MYTEST", "地址为：$finalAddr")

            val jsonBody = JSONObject()

            val jsonAddr = JSONObject().apply {
                put("android.intent.extra.SUBJECT", name)
                put("android.intent.extra.TEXT", finalAddr)
            }

            val unixTime = System.currentTimeMillis() / 1000L

            jsonBody.put("type", "share_ext_content_raw")
            jsonBody.put("value", jsonAddr)
            jsonBody.put("locale", "zh-CN")
            jsonBody.put("timestamp_ms", unixTime.toString())

            Log.d("MYTEST", unixTime.toString())
            Log.d("MYTEST", "id:$id,token:$token")
            Log.d("MYTEST", jsonBody.toString())

            val gson = Gson()
            val queue = Volley.newRequestQueue(this)
            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                com.android.volley.Response.Listener<JSONObject> { response ->
                    val info = gson.fromJson(response.toString(), CommandResult::class.java)
                    Log.d("MYTEST", "info:${info.reason}")
                    Log.d("MYTEST", "info:${info.result}")

                    val icDone = dialog.findViewById<ImageView>(R.id.done_icon)
                    val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                    val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                    text.text = getString(R.string.sent_success)
                    icDone.visibility = View.VISIBLE
                    progress.visibility = View.INVISIBLE

                    Handler().postDelayed({
                        dialog.dismiss()
                        (context as Activity).finish()
                    }, 2000)


                }, com.android.volley.Response.ErrorListener { error ->
                    val icError = dialog.findViewById<ImageView>(R.id.error_icon)
                    val progress = dialog.findViewById<ProgressBar>(R.id.progress)
                    val text = dialog.findViewById<TextView>(R.id.loadingTitle)
                    text.text = "Error: " + error.networkResponse.statusCode
                    icError.visibility = View.VISIBLE
                    progress.visibility = View.INVISIBLE
                    Handler().postDelayed({ dialog.dismiss() }, 2000)
                }) {

//                override fun getBodyContentType(): String {
//                    return "application/json; charset=UTF-8"
//                }

                override fun getHeaders(): HashMap<String, String> {
                    val header = HashMap<String, String>()
                    header["Authorization"] = "Bearer $token"
                    return header
                }
            }
            queue.add(jsonObjectRequest)
        }

    }

    //搜索结果点击侦听器
    private class SearchItemOnClickListener(val context: Context) : View.OnClickListener {

        override fun onClick(v: View?) {
            Log.d("MYTEST", "检测到点按事件！$v")
            if (v != null && v.id == R.id.address_item) {
                val myAddr = v.findViewById<TextView>(R.id.poi_item_title).text as String
                Log.d("MYTEST", "检测地址为：$myAddr")
                for (poiItem in poiDataset) {
                    if (poiItem.title == myAddr) {
                        Log.d("MYTEST", "检测到列表对应项！")
                        targetTitle = myAddr
                        val addressSearch = GeocodeSearch(context)
                        addressSearch.setOnGeocodeSearchListener(MyGeocodeSearchListener())
                        val query = RegeocodeQuery(poiItem.latLonPoint, 50f, GeocodeSearch.AMAP)
                        addressSearch.getFromLocationAsyn(query)
                        NavShareActivity().searchViewExitTransit(context, poiItem.latLonPoint)
                    }
                }
            }
        }


    }

    //地址列表适配器
    private class PoiAdapter(val context: Context, private val poiDataset: ArrayList<PoiItem>) :
        RecyclerView.Adapter<PoiAdapter.PoiViewHolder>() {

        class PoiViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PoiViewHolder {
            val v = LayoutInflater.from(p0.context).inflate(R.layout.poi_list_item, p0, false)
            return PoiViewHolder(v)
        }

        override fun getItemCount() = poiDataset.size

        override fun onBindViewHolder(p0: PoiViewHolder, p1: Int) {
            Log.d("MYTEST", "地址为：${poiDataset[p1].adCode}")

            p0.view.findViewById<TextView>(R.id.poi_item_title).text = poiDataset[p1].title
            p0.view.findViewById<TextView>(R.id.poi_item_addr).text = poiDataset[p1].adName
            p0.view.setOnClickListener(SearchItemOnClickListener(context))
        }
    }

    private class MyGeocodeSearchListener : GeocodeSearch.OnGeocodeSearchListener {
        override fun onRegeocodeSearched(p0: RegeocodeResult?, p1: Int) {
            if (p0 != null) {
                targetAddress = p0.regeocodeAddress.formatAddress
            }
        }

        override fun onGeocodeSearched(p0: GeocodeResult?, p1: Int) {
            if (p0 != null) {

                val addressSource = p0.geocodeAddressList[0]
                targetAddress = addressSource.formatAddress + addressSource.neighborhood + addressSource.building
            }
        }

    }

    //POI搜搜结果侦听器
    private class MyPoiSearchListener(val context: Context) :
        PoiSearch.OnPoiSearchListener {
        override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {
            TODO()
        }

        override fun onPoiSearched(p0: PoiResult?, p1: Int) {
            Log.d("MYTEST", "POI搜索完毕：${p0?.pois}")
            val poiItems = p0?.pois
            poiDataset.clear()
            if (poiItems != null) {
                poiDataset = poiItems
                val viewManager = LinearLayoutManager(context)
                val viewAdapter = PoiAdapter(context, poiItems)
                val recyclerView = (context as Activity).findViewById<RecyclerView>(R.id.poiView)
                recyclerView.apply {
                    hasFixedSize()
                    layoutManager = viewManager
                    adapter = viewAdapter
                }
            }
        }
    }

    //地点搜索侦听器
    private class LocationSearchListener(val context: Context) :
        SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(p0: String?): Boolean {
            val searchView = (context as Activity).findViewById<android.widget.SearchView>(R.id.menu_search)
            searchView.isIconified = true
            searchView.setQuery(p0, false)
            return false
        }

        override fun onQueryTextChange(p0: String?): Boolean {
            if (p0 != null) {
                Log.d("MYTEST", "取得变更文字:$p0")
                if (countryISO == "cn") {
                    val query = PoiSearch.Query(p0, null)
                    query.pageSize = 25
                    val poiSearch = PoiSearch(context, query)
                    poiSearch.setOnPoiSearchListener(MyPoiSearchListener(context))
                    poiSearch.searchPOIAsyn()
                } else {
                    TODO("googleMAP 适配！")
//                    val radius = 100.12
//                    val targetNorthEast = SphericalUtil.computeOffset()
                    val myFilter = AutocompleteFilter.Builder()
                    myFilter.setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                    val myTask = Places.getGeoDataClient(context).getAutocompletePredictions(p0, null, myFilter.build())
                    myTask.addOnCompleteListener {
                        Log.d("MYTEST", "搜索到结果！！")
                        val places = it.result
                        if (places != null) {
                            val viewManager = LinearLayoutManager(context)
                            val viewAdapter = AutocompleteAdapter(context, places)
                            val recyclerView = (context as Activity).findViewById<RecyclerView>(R.id.poiView)
                            recyclerView.apply {
                                hasFixedSize()
                                layoutManager = viewManager
                                adapter = viewAdapter
                            }
                        }
                    }
                }

            }
            return true
        }

    }

    private fun searchViewInit(context: Context) {
        val mSceneRoot: ViewGroup = (context as Activity).findViewById(R.id.scene_root)
        val mScene1: Scene = Scene.getSceneForLayout(mSceneRoot, R.layout.activity_nav_before_search, context)
        mScene1.enter()

    }

    private fun searchViewEnterTransit(context: Context) {
        //定义两个场景
        val mSceneRoot: ViewGroup = (context as Activity).findViewById(R.id.scene_root)
        val mScene2: Scene = Scene.getSceneForLayout(mSceneRoot, R.layout.activity_nav_on_search, context)
        val transition = TransitionInflater.from(context).inflateTransition(R.transition.search_list_move)
        TransitionManager.go(mScene2, transition)
        mGoogleMap?.onDestroy()
        mMapView?.onDestroy()
        recyclerView = context.findViewById(R.id.poiView)
        searchViewEntered = true
    }

    private fun searchViewExitTransit(context: Context, addrPoint: Any?) {
        Log.d("MYTEST", "进入退出搜索进程")


        if (searchViewEntered) {
            Log.d("MYTEST", "检测到需要退回第一界面")
            val mSceneRoot: ViewGroup = (context as Activity).findViewById(R.id.scene_root)
            val mScene1: Scene = Scene.getSceneForLayout(mSceneRoot, R.layout.activity_nav_before_search, context)
            val transition = TransitionInflater.from(context).inflateTransition(R.transition.search_list_move)
            TransitionManager.go(mScene1, transition)
            val searchView = context.findViewById<SearchView>(R.id.menu_search)
            searchView.setQuery("", false)
            searchView.isIconified = true
            searchViewEntered = false

            when (addrPoint) {
                is Place -> {
                    TODO("googleMAP 适配！")
                    mGoogleMap = context.findViewById(R.id.googlemap)
                    mGoogleMap?.visibility = View.VISIBLE
                    mGoogleMap?.onCreate(mySavedInstanceState)
                    mGoogleMap?.getMapAsync {
                        googleMapInit(context, it, addrPoint)
                    }

                }
                is LatLonPoint -> {

                    mMapView = context.findViewById(R.id.amap)
                    mMapView?.visibility = View.VISIBLE
                    mMapView?.onCreate(mySavedInstanceState)
                }
            }
        }

        when (addrPoint) {
            is LatLonPoint -> mapViewInit(context, addrPoint)
        }


    }

    private fun mapViewInit(context: Context, addrPoint: LatLonPoint?) {
        Log.d("MYTEST", "执行地图初始化操作")

        recyclerView = (context as Activity).findViewById(R.id.poiView)

        val myLocationStyle = MyLocationStyle()

        myLocationStyle.strokeColor(Color.argb(40, 0, 183, 255))
        myLocationStyle.radiusFillColor(Color.argb(10, 0, 183, 255))
        myLocationStyle.strokeWidth(5f)

        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            myLocationStyle.showMyLocation(true)
        }




        if (addrPoint == null) {
            myLocationStyle.interval(2000)
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
        } else {
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        }

        val aMap = mMapView!!.map
        aMap.myLocationStyle = myLocationStyle
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.isMyLocationEnabled = true
        aMap.uiSettings.isMyLocationButtonEnabled = true
        aMap.uiSettings.isRotateGesturesEnabled = false
        aMap.uiSettings.isTiltGesturesEnabled = false


        aMap.setOnMapClickListener { LatLng ->
            Log.d("MYTEST", "检测到click事件！")
            val mCameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition(LatLng, 17f, 0f, 0f)
            )

            aMap.animateCamera(mCameraUpdate)
            for (marker in aMap.mapScreenMarkers) {
                marker.remove()
            }
            aMap.addMarker(MarkerOptions().position(LatLng))


        }

        aMap.setOnCameraChangeListener(MyCameraChangeListener(context))

        if (addrPoint != null) {
            Log.d("MYTEST", "执行移动地图操作")
            val myLatLng = LatLng(addrPoint.latitude, addrPoint.longitude)
            val mCameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition(myLatLng, 17f, 0f, 0f)
            )
            aMap.animateCamera(mCameraUpdate)
            for (marker in aMap.mapScreenMarkers) {
                marker.remove()
            }
            aMap.addMarker(MarkerOptions().position(myLatLng))

        } else {
            val mCameraUpdate = CameraUpdateFactory.zoomTo(17f)
            aMap.animateCamera(mCameraUpdate)
        }
    }

    private lateinit var mGeoDataClient: GeoDataClient
    private lateinit var mPlaceDetectionClient: PlaceDetectionClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mySavedInstanceState = savedInstanceState
        }


        //获取MCC信息
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        countryISO = telephonyManager.networkCountryIso
        Log.d("MYTEST", countryISO)


        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
        )

        //获取必要权限
        ActivityCompat.requestPermissions(this, permissions, 0)
        setContentView(R.layout.activity_nav_root)

        //改变窗口宽度并初始化
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (ViewGroup.LayoutParams.MATCH_PARENT))
        val toolbar = findViewById<Toolbar>(R.id.mapsearchbar)
        toolbar.inflateMenu(R.menu.menu_item)
        toolbar.title = getString(R.string.send_location)

        //设置边框蒙版
        val view = findViewById<CoordinatorLayout>(R.id.mapShareLayout)
        view.clipToOutline = true

        //置入视图
        searchViewInit(this)

        //获取列表视图
        recyclerView = findViewById(R.id.poiView)

        //地图视图初始化


        if (countryISO == "cn") {
            mMapView = findViewById(R.id.amap)
            mMapView?.visibility = View.VISIBLE
            mMapView?.onCreate(savedInstanceState)
            mapViewInit(this, null)
        } else {
            TODO("googleMAP 适配！")
            mGoogleMap = findViewById(R.id.googlemap)
            mGoogleMap?.visibility = View.VISIBLE
            mGoogleMap?.onCreate(savedInstanceState)
            mGoogleMap?.getMapAsync {
                googleMapInit(this, it, null)
            }
        }


        //初始化搜索框
        val searchView = findViewById<android.widget.SearchView>(R.id.menu_search)
        searchView.queryHint = "请搜索地址"
        searchView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

        //设置搜索框输入侦听器
        searchView.setOnQueryTextListener(LocationSearchListener(this))

        //搜索按钮点击事件注册
        searchView.setOnSearchClickListener {
            searchView.maxWidth
            searchViewEnterTransit(this)
            recyclerView = findViewById(R.id.poiView)
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem != null) {
                when (menuItem.itemId) {
                    R.id.menu_send -> {
                        Log.d("MYTEST", "接收发送按钮:$targetTitle,$targetAddress")
                        val dialog = LoginActivity().showLoading(this, "Sending...")
                        commandSendNav(this, targetTitle, targetAddress, dialog)
                    }
                }
            }
            true
        }

        //返回键按键侦听器
        toolbar.setNavigationOnClickListener {
            if (searchViewEntered) {
                searchView.setQuery("", false)
                searchView.isIconified = true
                searchViewExitTransit(this, null)
            } else this.finish()
        }

    }


    //GoogleMap 适配，开发中，慎用！
    private fun googleMapInit(context: Context, map: GoogleMap, target: Place?) {
        Log.d("MYTEST", "执行地图初始化操作")

        mGeoDataClient = Places.getGeoDataClient(context)
        mPlaceDetectionClient = Places.getPlaceDetectionClient(context)


        val options = GoogleMapOptions().mapType(GoogleMap.MAP_TYPE_NORMAL)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)

        com.google.android.gms.maps.MapView(context, options)

        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
        map.uiSettings.apply {
            isCompassEnabled = true
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isZoomControlsEnabled = true
        }

        //设定地图停止移动侦听器
        map.setOnCameraIdleListener {
            Log.d("MYTEST", "检测到map移动")
            val placeResult = mPlaceDetectionClient.getCurrentPlace(null)
            placeResult.addOnCompleteListener {
                Log.d("MYTEST", "搜索到结果！！")
                val places = it.result
                if (places != null) {
                    val viewManager = LinearLayoutManager(context)
                    val viewAdapter = PlacesAdapter(places)
                    recyclerView = (context as Activity).findViewById(R.id.poiView)
                    recyclerView.apply {
                        hasFixedSize()
                        layoutManager = viewManager
                        adapter = viewAdapter
                    }
                }
            }.addOnFailureListener {
                Log.d("MYTEST", "dddd")
            }
        }

        //设定地图点击侦听器
        map.setOnMapClickListener {
            Log.d("MYTEST", "检测到map点击")

            //清除所有markers
            map.clear()
            val markerOptions = com.google.android.gms.maps.model.MarkerOptions()
            markerOptions.position(it)
            map.addMarker(markerOptions)
            map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(it, 18f))

        }

        //移动map至定位点
        try {
            val locationResult = FusedLocationProviderClient(context as Activity).lastLocation
            locationResult.addOnCompleteListener {
                if (it.isSuccessful) {
                    val myLastLocation = it.result
                    if (myLastLocation != null) {
                        map.moveCamera(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                com.google.android.gms.maps.model.LatLng(
                                    myLastLocation.latitude,
                                    myLastLocation.longitude
                                ),
                                20f
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.d("MYTEST", e.toString())
        }

        if (target != null) {
            map.clear()
            val markerOptions = com.google.android.gms.maps.model.MarkerOptions()
            markerOptions.position(target.latLng)
                .title(target.name.toString())
            map.addMarker(markerOptions)
            map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(target.latLng, 18f))
        }

    }

    //GoogleMap 适配，开发中，慎用！
    private class PlacesAdapter(val data: PlaceLikelihoodBufferResponse) :
        RecyclerView.Adapter<PlacesAdapter.PlacesViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.poi_list_item, parent, false)
            return PlacesViewHolder(v)
        }

        override fun getItemCount() = data.count

        override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
            holder.view.findViewById<TextView>(R.id.poi_item_title).text = data[position].place.name
            holder.view.findViewById<TextView>(R.id.poi_item_addr).text = data[position].place.address
        }

        private class PlacesViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    }

    //GoogleMap 适配，开发中，慎用！
    private class AutocompleteAdapter(val context: Context, val data: AutocompletePredictionBufferResponse) :
        RecyclerView.Adapter<AutocompleteAdapter.AutocompleteViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutocompleteViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.poi_list_item, parent, false)
            return AutocompleteViewHolder(v)
        }

        override fun getItemCount() = data.count

        override fun onBindViewHolder(holder: AutocompleteViewHolder, position: Int) {
            holder.view.findViewById<TextView>(R.id.poi_item_title).text = data[position].getPrimaryText(null)
            holder.view.findViewById<TextView>(R.id.poi_item_addr).text = data[position].getSecondaryText(null)
            holder.view.setOnClickListener {
                val mGeoDataClient = Places.getGeoDataClient(context)
                val mGetPlaceTask = mGeoDataClient.getPlaceById(data[position].placeId)
                mGetPlaceTask.addOnCompleteListener { result ->
                    if (result.result != null) {
                        NavShareActivity().searchViewExitTransit(context, result.result?.get(0))
                    }

                }

            }
        }

        private class AutocompleteViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    }


    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
        mGoogleMap?.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
        mGoogleMap?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
        mGoogleMap?.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mMapView?.onSaveInstanceState(outState)
        mGoogleMap?.onSaveInstanceState(outState)
    }
}

