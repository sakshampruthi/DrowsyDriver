package com.saksham.driverdrowsy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Instrumentation.ActivityResult
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
//import com.google.mlkit.common.model.LocalModel
//import com.google.mlkit.vision.label.ImageLabeling
//import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.saksham.driverdrwosy.R
import com.saksham.driverdrowsy.camera.CameraSourcePreview
import com.saksham.driverdrowsy.camera.GraphicOverlay
import com.tbruyelle.rxpermissions3.RxPermissions
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CrashDetector.CrashListener, LocationListener,
    RecognitionListener {

    private lateinit var mPreview: CameraSourcePreview
    private lateinit var mGraphicOverlay: GraphicOverlay
    private lateinit var end_button: AppCompatButton
    private lateinit var layout: ConstraintLayout
    private lateinit var n_mode: AppCompatToggleButton
    private lateinit var tv: AppCompatTextView
    private lateinit var tv_1: AppCompatTextView
    private var cameraSource: CameraSource? = null
    private var mp: MediaPlayer? = null
    private var count = 0
    private var count1 = 0
    var flag = 0
    val SMS_REQUEST_CODE = 8
    val s_time = 500
    private lateinit var mLocationManager: LocationManager
    lateinit var crashDetector: CrashDetector
    lateinit var crashDialog: androidx.appcompat.app.AlertDialog
    private lateinit var sharedPreferences: SharedPreferences
    private var vol = 0
    private lateinit var audioManager: AudioManager
    lateinit var dig: AlertDialog
    lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)
        dig = AlertDialog.Builder(this)
            .setTitle("Drowsy Alert !!!")
            .setMessage("Tracker suspects that the driver is experiencing Drowsiness, Please park your car on side.")
            .setPositiveButton(
                android.R.string.ok
            ) { dialog, which ->
                stop_playing()
                flag = 0
            }.setIcon(android.R.drawable.ic_dialog_alert)
            .create()
        dig.setOnDismissListener {
            stop_playing()
            flag = 0
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("By using this application you agree to allow us to use the location, camera and microphone of device as and when required only while using the app, for emergency purpose and to monitor drowsiness. This application in no way collects, stores or send any user information anywhere. This application in no way provides safety against accident in case the driver sleeps. This application is not a replacement to driving when sleepy. Please stop the car immediately, away from traffic if you feel drowsy at any given point of time. Driving when sleepy is extremely dangerous can lead serious accidents or death.")
            .setCancelable(false)
            .setPositiveButton("Ok") { dialog, which ->
                sharedPreferences.edit().putBoolean("privacy", true).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
                finishAffinity()
            }

        mPreview = findViewById(R.id.preview)
        mGraphicOverlay = findViewById(R.id.faceOverlay)
        end_button = findViewById(R.id.button)
        layout = findViewById(R.id.topLayout)
        n_mode = findViewById(R.id.toggleButton)
        n_mode.textOn = "Night Mode ON"
        n_mode.text = "Night Mode"
        n_mode.textOff = "N-Mode OFF"
        crashDetector = CrashDetector(this)
        crashDetector.registerCrashListener(this)
        n_mode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mPreview.visibility = View.INVISIBLE
                Toast.makeText(
                    applicationContext,
                    "Increase Brightness to maximum for higher accuracy",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                mPreview.visibility = View.VISIBLE
            }
        }

        if (sharedPreferences.getString("phone", null).isNullOrEmpty()) {
            showPhoneDialog()
        }
        tv = findViewById(R.id.textView3)
        tv_1 = findViewById(R.id.textView4)
        initCrashAlertDialog()

        if (!sharedPreferences.getBoolean("privacy", false)) {
            dialog.show()
        }

        end_button.setOnClickListener {
//            val next = Intent(this@FaceTrackerActivity, end::class.java)
//            count = 0
//            count1 = 0
//            next.putExtra(key_3, start)
//            next.putExtra(key, tv_1.text)
//            next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            startActivity(next)
            finishAffinity()
        }


        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc: Int = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
        requestSMSPermission()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECORD_AUDIO
                ),
                4
            )
        }

    }

    override fun onLocationChanged(p0: Location) {
        if (p0.hasSpeed() && tv_1.text == "Face Missing")
            AlertDialog.Builder(this)
                .setTitle("Face Missing")
                .setMessage("It appears that you are driving and your face is not being tracked. Please fix your phone in such a way that you face is visible at all times to the device front camera")
                .setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    fun showPhoneDialog() {
        val dialog = Dialog(this)

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_phone_no)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val textInputLayout = dialog.findViewById<TextInputLayout>(R.id.textInputLayout)
        val save = dialog.findViewById<AppCompatButton>(R.id.save)

        textInputLayout.editText?.doAfterTextChanged { textInputLayout.error = null }

        save.setOnClickListener {
            if (textInputLayout.editText?.text.isNullOrEmpty()) {
                textInputLayout.error = "Enter a valid number"
                textInputLayout.requestFocus()
                return@setOnClickListener
            }
            sharedPreferences.edit().putString("phone", textInputLayout.editText?.text.toString())
                .apply()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun requestSMSPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECORD_AUDIO
                ),
                SMS_REQUEST_CODE
            )
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Toast.makeText(this@MainActivity, "ready", Toast.LENGTH_SHORT).show()
    }

    override fun onBeginningOfSpeech() {

    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onBufferReceived(buffer: ByteArray?) {

    }

    override fun onEndOfSpeech() {

    }

    override fun onError(error: Int) {

    }

    override fun onResults(results: Bundle?) {

    }

    override fun onPartialResults(partialResults: Bundle?) {
        val result = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (result != null) {
            for (s in result) {
                if (s.contains("stop") || s.contains("shop") || s.contains("top"))
                    stopDialog()
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {

    }

    private fun speechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)

    }

    private val startSpeechRecognition =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.data != null) {
                val result = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (result?.get(0)?.contains("stop") == true || result?.get(0)
                        ?.contains("shop") == true
                )
                    stopDialog()
            }

        }

    fun stopDialog() {
        dig.dismiss()
        stop_playing()
        speechRecognizer.destroy()

    }

    private fun requestCameraPermission() {
        Log.w(
            "TAG",
            "Camera permission is not granted. Requesting permission"
        )
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO
        )
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                RC_HANDLE_CAMERA_PERM
            )
            return
        }
        val thisActivity: Activity = this
        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(
                thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }
        Snackbar.make(
            mGraphicOverlay, "Access to the camera is needed for detection",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Ok", listener)
            .show()
    }

    private fun createCameraSource() {
        val context = applicationContext
        val detector = FaceDetector.Builder(context)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .build()
        detector.setProcessor(
            MultiProcessor.Builder(GraphicFaceTrackerFactory())
                .build()
        )
//        loadModel()
        if (!detector.isOperational) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Toast.makeText(
                applicationContext,
                "Dependencies are not yet available. ",
                Toast.LENGTH_LONG
            ).show()
        }
        cameraSource = CameraSource.Builder(context, detector)
            .setRequestedPreviewSize(640, 480)
            .setFacing(CameraSource.CAMERA_FACING_FRONT)
            .setRequestedFps(60.0f)
            .build()
    }

    // Graphic Face Tracker
    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face> {
        override fun create(face: Face): Tracker<Face> {
            return GraphicFaceTracker(mGraphicOverlay)
        }
    }

    private inner class GraphicFaceTracker(private val mOverlay: GraphicOverlay) :
        Tracker<Face>() {
        private val mFaceGraphic: FaceGraphic = FaceGraphic(mOverlay)

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face) {
            mFaceGraphic.setId(faceId)
        }

        var state_i = 0
        var state_f = -1
        var start: Long = 0
        var end = System.currentTimeMillis()
        var begin: Long = 0
        var stop: Long = 0
        var c = 0
        override fun onUpdate(detectionResults: Detections<Face>, face: Face) {
            mOverlay.add(mFaceGraphic)
            mFaceGraphic.updateFace(face)
            if (flag == 0) {
                eye_tracking(face)
            }
        }

        override fun onMissing(detectionResults: Detections<Face>) {
            mOverlay.remove(mFaceGraphic)
            setText(tv_1, "Face Missing")
        }

        override fun onDone() {
            mOverlay.remove(mFaceGraphic)
        }

        private fun setText(text: TextView, value: String) {
            runOnUiThread(Runnable { text.text = value })
        }

        private fun eye_tracking(face: Face) {
            val l = face.isLeftEyeOpenProbability
            val r = face.isRightEyeOpenProbability
            state_i = if (l < 0.50 && r < 0.50) {
                0
            } else {
                1
            }
            if (state_i != state_f) {
                start = System.currentTimeMillis()
                if (state_f == 0) {
                    c = incrementer_1()
                }
                end = start
                stop = System.currentTimeMillis()
            } else if (state_i == 0 && state_f == 0) {
                begin = System.currentTimeMillis()
                if (begin - stop > s_time) {
                    c = incrementer()
                    alert_box()
                    flag = 1
                }
                begin = stop
            }
            state_f = state_i
            status()
        }

        fun status() {
            runOnUiThread {
                val s: Int = get_incrementer()
                if (s < 5) {
                    setText(tv_1, "Active")
                    tv_1.setTextColor(Color.GREEN)
                    tv_1.typeface = Typeface.DEFAULT_BOLD
                }
                if (s > 4) {
                    setText(tv_1, "Sleepy")
                    tv_1.setTextColor(Color.YELLOW)
                    tv_1.typeface = Typeface.DEFAULT_BOLD
                }
                if (s > 8) {
                    setText(tv_1, "Drowsy")
                    tv_1.setTextColor(Color.RED)
                    tv_1.typeface = Typeface.DEFAULT_BOLD
                }
            }
        }

    }

//    fun loadModel(){
//        try {
//            val localModel = LocalModel.Builder()
//                .setAssetFilePath("android.resource://$packageName/model.tflite")
//                .build()
//
//            val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
//                .setConfidenceThreshold(0.5f)
//                .setMaxResultCount(5)
//                .build()
//            ImageLabeling.getClient(customImageLabelerOptions)
//        }
//        catch (_:Exception){}
//    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
        crashDetector.onResume()
        val filter = IntentFilter()
        filter.addAction("com.saksham.driverdrowsy.user_location")
        registerReceiver(mServiceReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        mPreview.stop()
        crashDetector.onPause()
        try {
            unregisterReceiver(mServiceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource?.release()
        }
        crashDetector.unregisterCrashListener(this)
        stop_playing()
    }

    fun incrementer(): Int {
        count++
        return count
    }

    fun incrementer_1(): Int {
        count1++
        return count1
    }

    fun get_incrementer(): Int {
        return count
    }

    fun play_media() {
        vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (vol < 100) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )
        }
        stop_playing()
        mp = MediaPlayer.create(this, R.raw.buzzer)
        mp?.start()
    }

    fun stop_playing() {
        if (mp != null) {
            mp?.stop()
            mp?.release()
            mp = null
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
    }


    companion object {
        private const val RC_HANDLE_CAMERA_PERM = 2
        private const val RC_HANDLE_GMS = 9001
    }

    private fun startCameraSource() {

        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            applicationContext
        )
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(
                this,
                code,
                RC_HANDLE_GMS
            )
            dlg?.show()
        }
        if (cameraSource != null) {
            try {
                mPreview.start(cameraSource, mGraphicOverlay)
            } catch (e: IOException) {
                Log.e(
                    "TAG",
                    "Unable to start camera source.",
                    e
                )
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (requestCode == 157)
            getLocation()

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission, so create the camerasource
            createCameraSource()
            return
        }
        Log.e(
            "TAG",
            "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"
        )
        val listener =
            DialogInterface.OnClickListener { dialog, id -> finish() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("ALERT")
            .setMessage("This application cannot run because it does not have the camera permission. The application will now exit.")
            .setPositiveButton("Ok", listener)
            .show()
    }

    fun alert_box() {
        play_media()
        runOnUiThread {
            play_media()
            speechRecognition()
            dig.show()
        }
    }

    private fun getLocation() {
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                157
            )
        } else if (mLocationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        ) {
            startService(
                Intent(
                    this,
                    LocationService::class.java
                )
            )
        }
    }


    private val mServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val latitude = intent?.getDoubleExtra("Latitude", 0.0)
            val longitude = intent?.getDoubleExtra("Longitude", 0.0)
            val address = intent?.getStringExtra("Address")
            if (latitude != null && longitude != null) {
                sendEmergencyMessage(longitude, latitude, address.toString())
            }
        }
    }

    fun sendEmergencyMessage(longitude: Double, latitude: Double, address: String) {
        val msg =
            "I might have been in a collision around $address."
        val msg2 =
            "My current location is: https://www.google.com/maps/place/$latitude,$longitude \n\nBring some help."
        val smsManager: SmsManager = getSystemService(SmsManager::class.java)
        val phone = sharedPreferences.getString("phone", null)
        if (phone != null) {
            smsManager.sendTextMessage(phone, null, msg, null, null)
            smsManager.sendTextMessage(phone, null, msg2, null, null)
        }
        Toast.makeText(this, "Alert Send", Toast.LENGTH_SHORT).show()
    }

    override fun onCrashSuspected() {
        crashDialog.show()
    }

    fun initCrashAlertDialog() {
        crashDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Crash Detected")
            .setMessage("Would you like to send an alert?")
            .setPositiveButton(
                "Send Alert"
            ) { dialog, which ->
                getLocation()
            }
            .setNegativeButton("Cancel", null)
            .create()
        crashDialog.setOnShowListener(object : DialogInterface.OnShowListener {
            private val AUTO_DISMISS_MILLIS = 6000L
            override fun onShow(dialog: DialogInterface) {
                val defaultButton =
                    (dialog as androidx.appcompat.app.AlertDialog).getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                val negativeButtonText = defaultButton.text
                object : CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        defaultButton.text = ""
                        defaultButton.text = java.lang.String.format(
                            Locale.getDefault(), "%s (%d)",
                            negativeButtonText,
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                        )
                    }

                    override fun onFinish() {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                            getLocation()
                        }
                    }
                }.start()
            }
        })
    }

}