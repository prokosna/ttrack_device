package xyz.prokosna.ttrack_device

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.gson.GsonBuilder
import xyz.prokosna.ttrack_device.service.FakeLogFetcher
import xyz.prokosna.ttrack_device.service.TelemetryCollector
import xyz.prokosna.ttrack_device.store.RealmStore

private const val URI = "https://topy-biz-ttrack.appspot.com/fakes"
private const val REQUEST_CODE = 1000
private var satelliteUri = URI
private var gatewayUri = URI

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    private val fakeLogFetcher = FakeLogFetcher()
    private val gson = GsonBuilder().serializeNulls().create()

    override fun onCreate() {
        super.onCreate()

        // Store
        val realmStore = RealmStore(this)

        // Sensor
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Telemetry
        val telemetryCollector = TelemetryCollector(this, sensorManager)
        telemetryCollector.beginMonitoring()

        Thread(Runnable {
            val log = fakeLogFetcher.fetch(URI)
            Log.i("debug", gson.toJson(log))
        }).start()

        val tel = telemetryCollector.collect()
        Log.i("debug", gson.toJson(tel))
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}