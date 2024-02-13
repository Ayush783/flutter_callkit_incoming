package com.hiennv.flutter_callkit_incoming
import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hiennv.flutter_callkit_incoming.widgets.RippleRelativeLayout
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.OkHttpClient
import kotlin.math.abs

class CallkitIncomingActivity : Activity() {

    companion object {

        private const val ACTION_ENDED_CALL_INCOMING =
                "com.hiennv.flutter_callkit_incoming.ACTION_ENDED_CALL_INCOMING"

        fun getIntent(context: Context, data: Bundle) = Intent(CallkitConstants.ACTION_CALL_INCOMING).apply {
            action = "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}"
            putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        fun getIntentEnded(context: Context, isAccepted: Boolean): Intent {
            val intent = Intent("${context.packageName}.${ACTION_ENDED_CALL_INCOMING}")
            intent.putExtra("ACCEPTED", isAccepted)
            return intent
        }
    }

    inner class EndedCallkitIncomingBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isFinishing) {
                val isAccepted = intent.getBooleanExtra("ACCEPTED", false)
                if (isAccepted) {
                    finishDelayed()
                } else {
                    finishTask()
                }
            }
        }
    }

    private var endedCallkitIncomingBroadcastReceiver = EndedCallkitIncomingBroadcastReceiver()

    private lateinit var ivBackground: ImageView
    private lateinit var llBackgroundAnimation: RippleRelativeLayout

    private lateinit var tvNameCaller: TextView
    private lateinit var subTextView: TextView
    private lateinit var tvNumber: TextView
    private lateinit var topHeader: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var ivAvatar: CircleImageView

    private lateinit var llAction: LinearLayout
    private lateinit var ivAcceptCall: ImageView
    private lateinit var tvAccept: TextView

    private lateinit var ivDeclineCall: ImageView
    private lateinit var tvDecline: TextView

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (!Utils.isTablet(this@CallkitIncomingActivity)) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
        transparentStatusAndNavigation()

        setContentView(R.layout.activity_callkit_incoming_v2)
        initView()
        incomingData(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                endedCallkitIncomingBroadcastReceiver,
                IntentFilter("${packageName}.${ACTION_ENDED_CALL_INCOMING}"),
                Context.RECEIVER_EXPORTED,
            )
        } else {
            registerReceiver(
                endedCallkitIncomingBroadcastReceiver,
                IntentFilter("${packageName}.${ACTION_ENDED_CALL_INCOMING}")
            )
        }
    }

    private fun wakeLockRequest(duration: Long) {

        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Callkit:PowerManager"
        )
        wakeLock.acquire(duration)
    }

    private fun transparentStatusAndNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                    (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION), false
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win: Window = window
        val winParams: WindowManager.LayoutParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }


    private fun incomingData(intent: Intent) {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        if (data == null) finish()

		val textColor = data?.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_COLOR, "#ffffff")
        val isShowCallID = data?.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_SHOW_CALL_ID, false)
        tvNameCaller.text = data?.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
        topHeader.text = "Incoming chat request from"
        tvNumber.text = data?.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, "")

        val subText = data?.getString(CallkitConstants.EXTRA_CALLKIT_SUB_TEXT, "")
        if (subText!=null && subText.isNotEmpty()) {
            subTextView.text = subText
            subTextView.visibility = View.VISIBLE
        }

        tvNumber.visibility = if (isShowCallID == true) View.VISIBLE else View.INVISIBLE

		try {
			tvNameCaller.setTextColor(Color.parseColor(textColor))
			tvNumber.setTextColor(Color.parseColor(textColor))
		} catch (error: Exception) {
		}

        val isShowLogo = data?.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_SHOW_LOGO, false)
        var appLogo = data?.getString(CallkitConstants.EXTRA_CALLKIT_APP_LOGO, "")
        if (appLogo != null && appLogo.isNotEmpty()) {
            if (!appLogo.startsWith("http://", true) && !appLogo.startsWith("https://", true)){
                appLogo = String.format("file:///android_asset/flutter_assets/%s", appLogo)
            }
            val headers = data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                    .load(appLogo)
                    .placeholder(R.drawable.transparent)
                    .error(R.drawable.transparent)
                    .into(ivLogo)
        }
        ivLogo.visibility = if (isShowLogo == true) View.VISIBLE else View.INVISIBLE

        var avatarUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, "")
        if (avatarUrl != null && avatarUrl.isNotEmpty()) {
            ivAvatar.visibility = View.VISIBLE
            if (!avatarUrl.startsWith("http://", true) && !avatarUrl.startsWith("https://", true)){
                avatarUrl = String.format("file:///android_asset/flutter_assets/%s", avatarUrl)
            }
            val headers = data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
        }

        val callType = data?.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, 0) ?: 0
        if (callType > 0) {
            ivAcceptCall.setImageResource(R.drawable.ic_video)
        }
        val duration = data?.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 0L) ?: 0L
        wakeLockRequest(duration)

        finishTimeout(data, duration)

        val textAccept = data?.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, "")
        tvAccept.text = if (TextUtils.isEmpty(textAccept)) getString(R.string.text_accept) else textAccept
        val textDecline = data?.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, "")
        tvDecline.text = if (TextUtils.isEmpty(textDecline)) getString(R.string.text_decline) else textDecline

		try {
			tvAccept.setTextColor(Color.parseColor(textColor))
			tvDecline.setTextColor(Color.parseColor(textColor))
		} catch (error: Exception) {
		}

        val backgroundColor = data?.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_COLOR, "#0955fa")
        try {
            ivBackground.setBackgroundColor(Color.parseColor(backgroundColor))
        } catch (error: Exception) {
        }
        var backgroundUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_URL, "")
        if (backgroundUrl != null && backgroundUrl.isNotEmpty()) {
            if (!backgroundUrl.startsWith("http://", true) && !backgroundUrl.startsWith("https://", true)){
                backgroundUrl = String.format("file:///android_asset/flutter_assets/%s", backgroundUrl)
            }
            val headers = data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                    .load(backgroundUrl)
                    .placeholder(R.drawable.transparent)
                    .error(R.drawable.transparent)
                    .into(ivBackground)
        }

        var acceptIconUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_ACCEPT_ICON_URL, "")
        if (acceptIconUrl != null && acceptIconUrl.isNotEmpty()) {
            if (!acceptIconUrl.startsWith("http://", true) && !acceptIconUrl.startsWith("https://", true)){
                acceptIconUrl = String.format("file:///android_asset/flutter_assets/%s", acceptIconUrl)
            }
            val headers = data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                    .load(acceptIconUrl)
                    .placeholder(R.drawable.transparent)
                    .error(R.drawable.transparent)
                    .into(ivAcceptCall)
        }

        var declineCallUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_DECLINE_ICON_URL, "")
        if (declineCallUrl != null && declineCallUrl.isNotEmpty()) {
            if (!declineCallUrl.startsWith("http://", true) && !declineCallUrl.startsWith("https://", true)){
                declineCallUrl = String.format("file:///android_asset/flutter_assets/%s", declineCallUrl)
            }
            val headers = data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                    .load(declineCallUrl)
                    .placeholder(R.drawable.transparent)
                    .error(R.drawable.transparent)
                    .into(ivDeclineCall)
        }
    }

    private fun finishTimeout(data: Bundle?, duration: Long) {
        val currentSystemTime = System.currentTimeMillis()
        val timeStartCall =
                data?.getLong(CallkitNotificationManager.EXTRA_TIME_START_CALL, currentSystemTime)
                        ?: currentSystemTime

        val timeOut = duration - abs(currentSystemTime - timeStartCall)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                finishTask()
            }
        }, timeOut)
    }

    private fun initView() {
        ivBackground = findViewById(R.id.ivBackground)

        tvNameCaller = findViewById(R.id.tvNameCaller)
        tvNumber = findViewById(R.id.tvNumber)
        subTextView = findViewById(R.id.subText)
        topHeader = findViewById(R.id.topHeader)
        ivLogo = findViewById(R.id.ivLogo)
        ivAvatar = findViewById(R.id.ivAvatar)

        llAction = findViewById(R.id.llAction)

        val params = llAction.layoutParams as MarginLayoutParams
        params.setMargins(0, 0, 0, Utils.getNavigationBarHeight(this@CallkitIncomingActivity))
        llAction.layoutParams = params

        ivAcceptCall = findViewById(R.id.ivAcceptCall)
        tvAccept = findViewById(R.id.tvAccept)
        ivDeclineCall = findViewById(R.id.ivDeclineCall)
        tvDecline = findViewById(R.id.tvDecline)
        animateAcceptCall()

        ivAcceptCall.setOnClickListener {
            onAcceptClick()
        }
        ivDeclineCall.setOnClickListener {
            onDeclineClick()
        }
    }

    private fun animateAcceptCall() {
        val shakeAnimation =
                AnimationUtils.loadAnimation(this@CallkitIncomingActivity, R.anim.shake_anim)
        ivAcceptCall.animation = shakeAnimation
    }


    private fun onAcceptClick() {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        val acceptIntent = TransparentActivity.getIntent(this, CallkitConstants.ACTION_CALL_ACCEPT, data)
        startActivity(acceptIntent)
        dismissKeyguard()
        finish()
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }


    private fun onDeclineClick() {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        val declineReasonList = data?.getStringArrayList(CallkitConstants.EXTRA_DECLINE_REASON_CALL_END)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.showcancelchatreason, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(dialogView)


        val textView = dialogView.findViewById<TextView>(R.id.text_view_error)
        val dontCancel = dialogView.findViewById<Button>(R.id.btn_dont_cancel)
        val cancelRequest = dialogView.findViewById<Button>(R.id.btn_cancel_request)
        Log.d("declinereason", declineReasonList.toString());

        val relativeLayout = dialogView.findViewById<RelativeLayout>(R.id.radio_relative_layout)


//        val radioButtons = arrayOf(radioButton1, radioButton2, radioButton3, radioButton4, radioButton5)
        val radioGroup =  RadioGroup(this)

        if (declineReasonList != null) {
            for (i in declineReasonList ){
                val radioButton = RadioButton(this)
                radioButton.text = i;
                radioButton.id = declineReasonList.indexOf(i); // Set a unique id for each radio button

                val blackColor = ContextCompat.getColor(this, R.color.black)
                radioButton.setTextColor(blackColor) // Set text color to black
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    radioButton.buttonTintList = ColorStateList.valueOf(blackColor)
                } // Set button tint (color) to black

                radioGroup.addView(radioButton)
            }
        }
        relativeLayout.addView(radioGroup);

        dontCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        cancelRequest.setOnClickListener {

            val checkedRadioButtonId = radioGroup.checkedRadioButtonId
            val checkedButtonIdText = radioGroup.findViewById<RadioButton>(checkedRadioButtonId)

            if(radioGroup.checkedRadioButtonId == -1){
                textView.visibility=TextView.VISIBLE

            }
            else{

                data?.putString(CallkitConstants.EXTRA_REASON_CALL_END_ACTION,checkedButtonIdText.text.toString());
                val intent = CallkitIncomingBroadcastReceiver.getIntentDecline(this@CallkitIncomingActivity, data)
                sendBroadcast(intent)
                finishTask()
                bottomSheetDialog.dismiss()
            }

        }
        textView.visibility=TextView.INVISIBLE

        bottomSheetDialog.show()

    }

    private fun finishDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            finishTask()
        }, 1000)
    }

    private fun finishTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun getPicassoInstance(context: Context, headers: HashMap<String, Any?>): Picasso {
        val client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    val newRequestBuilder: okhttp3.Request.Builder = chain.request().newBuilder()
                    for ((key, value) in headers) {
                        newRequestBuilder.addHeader(key, value.toString())
                    }
                    chain.proceed(newRequestBuilder.build())
                }
                .build()
        return Picasso.Builder(context)
                .downloader(OkHttp3Downloader(client))
                .build()
    }

    override fun onDestroy() {
        unregisterReceiver(endedCallkitIncomingBroadcastReceiver)
        super.onDestroy()
    }

    override fun onBackPressed() {}
}
