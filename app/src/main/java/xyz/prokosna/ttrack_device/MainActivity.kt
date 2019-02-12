package xyz.prokosna.ttrack_device

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import xyz.prokosna.ttrack_device.model.Track
import xyz.prokosna.ttrack_device.service.FakeLogFetcher
import xyz.prokosna.ttrack_device.service.TelemetryCollector
import xyz.prokosna.ttrack_device.service.TrackSender
import xyz.prokosna.ttrack_device.store.RealmStore
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


private const val SATELLITE_URI = "https://topy-biz-ttrack.appspot.com/fakes"
private const val GATEWAY_URI = "ssl://mqtt.googleapis.com:8883"
private const val REQUEST_CODE = 1000
private var satelliteUri = SATELLITE_URI
private var gatewayUri = GATEWAY_URI
private var privateKey: PrivateKey? = null

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val satelliteText = findViewById<EditText>(R.id.satelliteUri)
        val gatewayText = findViewById<EditText>(R.id.gatewayUri)

        satelliteText.setText(SATELLITE_URI)
        gatewayText.setText(GATEWAY_URI)

        val keyIs = assets.open("ttrack-private-pk8.pem")
        val keyStr = keyIs.bufferedReader().use { it.readText() }
        val key = keyStr.replace("-----BEGIN PRIVATE KEY-----\n", "")
            .replace("\n-----END PRIVATE KEY-----\n", "")
            .replace("\n", "")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(key))
        val keyFactory = KeyFactory.getInstance("RSA")
        privateKey = keyFactory.generatePrivate(keySpec)

        // Permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), REQUEST_CODE
            )
        } else {
            startApp()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            startApp()
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startApp() {
        val startButton = findViewById<Button>(R.id.startButton)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val satelliteText = findViewById<EditText>(R.id.satelliteUri)
        val gatewayText = findViewById<EditText>(R.id.gatewayUri)

        startButton.setOnClickListener {
            satelliteUri = satelliteText.text.toString()
            gatewayUri = gatewayText.text.toString()

            // Foreground
            val intent = Intent(application, AppMainService::class.java)

            startForegroundService(intent)

            finish()
        }

        resetButton.setOnClickListener {
            val intent = Intent(application, AppMainService::class.java)
            stopService(intent)
        }
    }
}

class AppMainService : Service() {
    private var timer: Timer? = Timer(true)
    private val fakeLogFetcher = FakeLogFetcher()
    private var telemetryCollector: TelemetryCollector? = null
    private var trackSender: TrackSender? = null
    private var realmStore: RealmStore? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val context = applicationContext

        val requestCode = 0
        val channelId = "default"
        val title = context.getString(R.string.app_name)

        val pendingIntent = PendingIntent.getActivity(
            context, requestCode,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId, title, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Silent Notification"
        channel.setSound(null, null)
        channel.enableLights(false)
        channel.lightColor = Color.BLUE
        channel.enableVibration(false)

        notificationManager.createNotificationChannel(channel)
        val notification = Notification.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.btn_star)
            .setContentText("TTrack")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .build()

        startForeground(1, notification)

        // Store
        realmStore = RealmStore(context)

        // Sensor
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Telemetry
        telemetryCollector = TelemetryCollector(context, sensorManager)
        telemetryCollector!!.beginMonitoring()

        // Sender
        trackSender = TrackSender(gatewayUri, privateKey!!)

        if (timer == null) {
            timer = Timer(true)
        }
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val tel = telemetryCollector!!.collect()
                realmStore!!.addDeviceTelemetry(tel)

                val tels = realmStore!!.findAllDeviceTelemetry()
                if (tels.count() > 1000) {
                    stopSelf()
                }
                for (t in tels) {
                    val log = fakeLogFetcher.fetch(satelliteUri)
                    val track = Track(t, log)
                    try {
                        trackSender!!.send(track)
                        realmStore!!.removeDeviceTelemetry(t)
                    } catch (e: Exception) {
                        Log.e("debug", "Failed to send a message: $e")
                    }
                }
            }
        }, 2000, 2000)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
        telemetryCollector?.stopMonitoring()
        trackSender?.close()
        realmStore?.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}