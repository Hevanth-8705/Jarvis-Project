package com.example.jarvisandroid

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import ai.picovoice.porcupine.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        private const val CHANNEL_ID = "JarvisServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "JarvisService"
        
        private const val BACKEND_IP = "10.0.2.2"
        private const val BACKEND_URL = "http://$BACKEND_IP:8000/api/v1/agent/execute"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var bubbleView: View? = null
    private var isBubbleShowing = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var porcupineManager: PorcupineManager? = null
    
    private lateinit var memoryManager: MemoryManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                if (level != -1 && level < 15) {
                    proactiveSuggestion("Sir, battery is critical ($level%). I suggest initiating power-save protocols.")
                }
            }
        }
    }

    private var connectivityManager: ConnectivityManager? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            proactiveSuggestion("Sir, we have lost internet connectivity. Switching to local core mode.")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // Ensure startForeground is called immediately to prevent crash
        createNotificationChannel()
        val notification = createNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Foreground service start failed", e)
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            memoryManager = MemoryManager(this)
            memoryManager.loadMemory()

            setupFloatingUI()
            setupVoice()
            setupTTS()
            initWakeWord()
            
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            
            connectivityManager = getSystemService()
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error in service setup", e)
            Toast.makeText(this, "Jarvis initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Jarvis Autonomous Intelligence",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for Jarvis AI Background Service"
        }
        val manager = getSystemService<NotificationManager>()
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis AI Online")
            .setContentText("Monitoring context and awaiting commands...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("InflateParams")
    private fun setupFloatingUI() {
        try {
            if (!Settings.canDrawOverlays(this)) return

            windowManager = getSystemService()
            val inflater = LayoutInflater.from(this)
            floatingView = inflater.inflate(R.layout.floating_widget, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 400
            }

            windowManager?.addView(floatingView, params)

            val mascot = floatingView?.findViewById<JarvisMascotView>(R.id.mascot)
            mascot?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            if (floatingView?.isAttachedToWindow == true) {
                                windowManager?.updateViewLayout(floatingView, params)
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (abs(event.rawX - initialTouchX) < 10) v?.performClick()
                            return true
                        }
                    }
                    return false
                }
            })
            mascot?.setOnClickListener { startListening() }
        } catch (e: Exception) {
            Log.e(TAG, "Floating UI Error: ${e.message}")
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Handler(Looper.getMainLooper()).post { floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setSpeaking(true) }
                    }
                    override fun onDone(utteranceId: String?) {
                        Handler(Looper.getMainLooper()).post { floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setSpeaking(false) }
                    }
                    
                    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        Handler(Looper.getMainLooper()).post { floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setSpeaking(false) }
                    }
                    
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Handler(Looper.getMainLooper()).post { floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setSpeaking(false) }
                    }
                })
            }
        }
    }

    private fun setupVoice() {
        Handler(Looper.getMainLooper()).post {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) processCommand(matches[0])
                        else resumeWakeWord()
                    }
                    override fun onError(error: Int) {
                        Log.e(TAG, "Speech Error: $error")
                        floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setEmotion("idle")
                        resumeWakeWord()
                    }
                    override fun onReadyForSpeech(params: Bundle?) {
                        floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setEmotion("listening")
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun initWakeWord() {
        try {
            val accessKey = "YOUR_ACCESS_KEY" 
            if (accessKey == "YOUR_ACCESS_KEY") {
                Log.w(TAG, "Wake word engine not started: Access Key missing")
                return
            }
            
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey) 
                .setKeywords(arrayOf(Porcupine.BuiltInKeyword.JARVIS))
                .build(this) { Handler(Looper.getMainLooper()).post { startListening() } }
            porcupineManager?.start()
        } catch (t: Throwable) { 
            Log.e(TAG, "Porcupine Initialization Error: ${t.message}") 
        }
    }

    private fun startListening() {
        try {
            porcupineManager?.stop()
            val mascot = floatingView?.findViewById<JarvisMascotView>(R.id.mascot)
            mascot?.setEmotion("listening")
            val name = memoryManager.getPreference("name") ?: "Sir"
            speak("Awaiting your directive, $name.")
            speechIntent?.let { speechRecognizer?.startListening(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startListening: ${e.message}")
            resumeWakeWord()
        }
    }

    private fun resumeWakeWord() {
        try {
            porcupineManager?.start()
            floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setEmotion("idle")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming wake word: ${e.message}")
        }
    }

    private fun processCommand(command: String) {
        val mascot = floatingView?.findViewById<JarvisMascotView>(R.id.mascot)
        mascot?.setEmotion("thinking")
        
        Thread {
            var conn: HttpURLConnection? = null
            try {
                Log.d(TAG, "Attempting to connect to: $BACKEND_URL")
                val url = URL(BACKEND_URL)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000 
                conn.readTimeout = 10000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val requestBody = JSONObject().apply {
                    put("query", command)
                    put("history", memoryManager.getHistory())
                    put("system_context", memoryManager.getSystemContext())
                    put("device_context", JSONObject().apply {
                        put("battery", getBatteryLevel())
                        put("network", getNetworkType())
                        put("time", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
                    })
                }

                conn.outputStream.use { it.write(requestBody.toString().toByteArray()) }
                
                val responseCode = conn.responseCode
                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseStr)
                    
                    val answer = responseJson.optString("answer", "Logic loop incomplete.")
                    val executionPlan = responseJson.optJSONArray("plan")
                    val emotion = responseJson.optString("emotion", "happy")

                    Handler(Looper.getMainLooper()).post {
                        mascot?.setEmotion(emotion)
                        showBubble(answer)
                        speak(answer)
                        executionPlan?.let { executePlan(it) }
                        memoryManager.addInteraction(command, answer)
                        resumeWakeWord()
                    }
                } else {
                    throw Exception("Backend returned $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}")
                handleBrainError(command, e.message ?: "Unknown Error")
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun handleBrainError(command: String, errorMessage: String) {
        Handler(Looper.getMainLooper()).post {
            val mascot = floatingView?.findViewById<JarvisMascotView>(R.id.mascot)
            mascot?.setEmotion("confused")
            
            val lowCommand = command.lowercase()
            val simulatedAnswer = when {
                lowCommand.contains("hello") || lowCommand.contains("hey") -> "Hello Sir. My neural link to the backend is down, but my local core is online."
                lowCommand.contains("time") -> "The current time is ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}, Sir."
                lowCommand.contains("battery") -> "Sir, the device battery is at ${getBatteryLevel()}%."
                else -> "Sir, the neural link is unstable. I am operating on local emergency protocols."
            }
            
            showBubble(simulatedAnswer)
            speak(simulatedAnswer)
            resumeWakeWord()
        }
    }

    private fun executePlan(plan: JSONArray) {
        for (i in 0 until plan.length()) {
            try {
                val step = plan.getJSONObject(i)
                handleAction(step)
            } catch (e: Exception) {
                Log.e(TAG, "Plan execution error at step $i: ${e.message}")
            }
        }
    }

    private fun handleAction(action: JSONObject) {
        val type = action.optString("type")
        when (type) {
            "launch_app" -> {
                val pkg = action.optString("package")
                packageManager.getLaunchIntentForPackage(pkg)?.let {
                    startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            "web_search" -> {
                val query = action.optString("query")
                val intent = Intent(Intent.ACTION_VIEW, "https://google.com/search?q=$query".toUri())
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            "brightness" -> {
                if (Settings.System.canWrite(this)) {
                    val value = action.optInt("value")
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
                }
            }
            "volume" -> {
                val audioManager = getSystemService<AudioManager>()
                val value = action.optInt("value")
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
            }
            "notify" -> proactiveSuggestion(action.optString("message"))
            "save_preference" -> {
                val key = action.optString("key")
                val value = action.optString("value")
                memoryManager.savePreference(key, value)
            }
        }
    }

    private fun proactiveSuggestion(text: String) {
        Handler(Looper.getMainLooper()).post {
            floatingView?.findViewById<JarvisMascotView>(R.id.mascot)?.setEmotion("alert")
            showBubble(text)
            speak(text)
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService<BatteryManager>()
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }

    private fun getNetworkType(): String {
        return try {
            val cm = connectivityManager ?: return "Offline"
            val network = cm.activeNetwork ?: return "Offline"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "Offline"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun speak(text: String) {
        if (ttsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JarvisAgent")
        }
    }

    @SuppressLint("InflateParams")
    private fun showBubble(text: String) {
        try {
            if (isBubbleShowing) bubbleView?.let { if (it.isAttachedToWindow) windowManager?.removeView(it) }
            
            val inflater = LayoutInflater.from(this)
            bubbleView = inflater.inflate(R.layout.chat_bubble, null).apply {
                findViewById<TextView>(R.id.bubbleText).text = text
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val loc = IntArray(2)
                floatingView?.getLocationOnScreen(loc)
                x = loc[0] + 130
                y = loc[1] - 80
            }
            
            windowManager?.addView(bubbleView, params)
            isBubbleShowing = true
            
            Handler(Looper.getMainLooper()).postDelayed({
                if (isBubbleShowing && bubbleView?.isAttachedToWindow == true) {
                    windowManager?.removeView(bubbleView)
                    isBubbleShowing = false
                }
            }, 9000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bubble: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
        
        porcupineManager?.delete()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        
        if (floatingView?.isAttachedToWindow == true) windowManager?.removeView(floatingView)
        if (isBubbleShowing && bubbleView?.isAttachedToWindow == true) windowManager?.removeView(bubbleView)
    }
}
