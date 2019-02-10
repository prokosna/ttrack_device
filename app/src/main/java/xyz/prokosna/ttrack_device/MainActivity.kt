package xyz.prokosna.ttrack_device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.google.gson.GsonBuilder
import xyz.prokosna.ttrack_device.service.FakeLogFetcher
import xyz.prokosna.ttrack_device.service.TelemetryCollector
import xyz.prokosna.ttrack_device.store.RealmStore

private const val URI = "https://topy-biz-ttrack.appspot.com/fakes"
private const val REQUEST_CODE = 1000

class MainActivity : AppCompatActivity() {

    private val fakeLogFetcher = FakeLogFetcher()
    private val gson = GsonBuilder().serializeNulls().create()
    private val handler = Handler()
    private val isActive = false

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
        // Store
        val realmStore = RealmStore(this)

        // Sensor
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val telemetryCollector = TelemetryCollector(this, sensorManager, locationManager)
        telemetryCollector.beginMonitoring()

        val button = findViewById<Button>(R.id.startButton)
        button.setOnClickListener {
            val textLog = findViewById<TextView>(R.id.textLog)

            Thread(Runnable {
                val log = fakeLogFetcher.fetch(URI)
                handler.post {
                    // textLog.text = gson.toJson(log)
                }
            }).start()
            button.text = "ok"

            val tel = telemetryCollector.collect()
            textLog.text = gson.toJson(tel)
        }
    }
}
