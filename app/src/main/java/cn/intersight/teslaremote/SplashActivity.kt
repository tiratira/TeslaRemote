package cn.intersight.teslaremote

import android.app.Activity
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.google.android.material.textfield.TextInputEditText


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.enterTransition = Fade().setDuration(500)
        window.exitTransition =Fade().setDuration(500)
        window.sharedElementExitTransition = ChangeImageTransform().setDuration(500)
        window.sharedElementEnterTransition = ChangeImageTransform().setDuration(500)

        setContentView(R.layout.activity_login_root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("channel_01", name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

//        WidgetPanel().showNotification(this, "测试通知")


        val sharedPref = getSharedPreferences(getString(R.string.user_data), Context.MODE_PRIVATE)
        val token = sharedPref.getString(getString(R.string.user_token),"")

        if (token!="") {
            val intent = Intent(this,VehicleSelectionActivity::class.java)
            startActivity(intent)
            this.finish()
        }


        val mSceneRoot: ViewGroup = findViewById(R.id.login_root)
        val scene1 = Scene.getSceneForLayout(mSceneRoot, R.layout.activity_login_scene1, this)
        TransitionManager.go(scene1,Fade().setDuration(0))
        mSceneRoot.postDelayed({
            val scene2 = Scene.getSceneForLayout(mSceneRoot, R.layout.activity_login_scene2, this)
            val transition= Fade().setDuration(1000)
            TransitionManager.go(scene2, transition)
        },1500)

    }

     fun continueClick(v:View) {

         when(v.id) {
             R.id.buttonContinue ->{
                 val intent = Intent(this,LoginActivity::class.java)
//                 val pairs = ArrayList<androidx.core.util.Pair<View,String>>()
//                 pairs.add(androidx.core.util.Pair(findViewById<ImageView>(R.id.appicon),"appicon_login"))
//                 pairs.add(androidx.core.util.Pair(findViewById<TextView>(R.id.apptitle),"apptitle_login"))
//                 val pairArray : Array<androidx.core.util.Pair<View,String>> = pairs.toTypedArray()
//                 val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this@SplashActivity,*pairArray)
                 startActivity(intent)
                 this.finish()
             }
         }

    }


}


