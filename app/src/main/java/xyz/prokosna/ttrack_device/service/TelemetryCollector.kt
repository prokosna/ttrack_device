package xyz.prokosna.ttrack_device.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import xyz.prokosna.ttrack_device.model.DeviceTelemetry


class TelemetryCollector(
    private val context: Context,
    private val sensorManager: SensorManager
) : SensorEventListener {

    // Sensors
    private val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val pressSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val humidSensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

    // FusedLocation
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private val locationSettingsRequest: LocationSettingsRequest
    private val locationCallback: LocationCallback
    private val locationRequest: LocationRequest
    private var requestingLocationUpdates = false

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                if (p0 != null) {
                    deviceTelemetry.location!!.lat = p0.lastLocation.latitude
                    deviceTelemetry.location!!.lon = p0.lastLocation.longitude
                    deviceTelemetry.location!!.alt = p0.lastLocation.altitude
                }
            }
        }

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private val deviceTelemetry = DeviceTelemetry()

    fun beginMonitoring() {
        // Sensors
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, pressSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, humidSensor, SensorManager.SENSOR_DELAY_UI)

        // Location
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(context, object : OnSuccessListener<LocationSettingsResponse> {
                override fun onSuccess(p0: LocationSettingsResponse?) {
                    Log.i("debug", "All location settings are ok")
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.myLooper()
                    )
                }
            })
            .addOnFailureListener(
                context
            ) { Log.i("debug", "Any settings are not good") }
        requestingLocationUpdates = true
    }

    fun stopMonitoring() {
        // Sensors
        sensorManager.unregisterListener(this)

        // Location
        if (!requestingLocationUpdates) {
            return
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener(
                context
            ) { requestingLocationUpdates = false }
    }

    fun collect(): DeviceTelemetry {
        return deviceTelemetry.copy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                deviceTelemetry.accelerometer!!.x = event.values[0]
                deviceTelemetry.accelerometer!!.y = event.values[1]
                deviceTelemetry.accelerometer!!.z = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                deviceTelemetry.gyroscope!!.x = event.values[0]
                deviceTelemetry.gyroscope!!.y = event.values[1]
                deviceTelemetry.gyroscope!!.z = event.values[2]
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                deviceTelemetry.rotationVector!!.x = event.values[0]
                deviceTelemetry.rotationVector!!.y = event.values[1]
                deviceTelemetry.rotationVector!!.z = event.values[2]
                deviceTelemetry.rotationVector!!.w = event.values[3]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                deviceTelemetry.magneticField!!.x = event.values[0]
                deviceTelemetry.magneticField!!.y = event.values[1]
                deviceTelemetry.magneticField!!.z = event.values[2]
            }
            Sensor.TYPE_PROXIMITY -> {
                deviceTelemetry.proximity = event.values[0]
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                deviceTelemetry.temperature = event.values[0]
            }
            Sensor.TYPE_LIGHT -> {
                deviceTelemetry.light = event.values[0]
            }
            Sensor.TYPE_PRESSURE -> {
                deviceTelemetry.pressure = event.values[0]
            }
            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                deviceTelemetry.humidity = event.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing to do
    }
}