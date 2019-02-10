package xyz.prokosna.ttrack_device.service

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.*
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import xyz.prokosna.ttrack_device.model.DeviceTelemetry


class TelemetryCollector(
    private val context: Activity,
    private val sensorManager: SensorManager,
    private val locationManager: LocationManager
) : SensorEventListener, LocationListener {

    private val provider: String
    private val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val pressSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val humidSensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

    init {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.powerRequirement = Criteria.POWER_HIGH
        criteria.isAltitudeRequired = true
        criteria.isSpeedRequired = false
        criteria.isCostAllowed = true
        criteria.isBearingRequired = false
        criteria.horizontalAccuracy = Criteria.ACCURACY_HIGH
        criteria.verticalAccuracy = Criteria.ACCURACY_HIGH
        provider = locationManager.getBestProvider(criteria, true)
    }

    private val deviceTelemetry = DeviceTelemetry()

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1000
            )
        }
    }

    fun beginMonitoring() {
        checkPermissions()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(provider, 10000, 5.0f, this)
        }
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, pressSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, humidSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopMonitoring() {
        locationManager.removeUpdates(this)
        sensorManager.unregisterListener(this)
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

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            return
        }
        deviceTelemetry.location!!.lat = location.latitude
        deviceTelemetry.location!!.lon = location.longitude
        deviceTelemetry.location!!.alt = location.altitude
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        when (status) {
            LocationProvider.OUT_OF_SERVICE, LocationProvider.TEMPORARILY_UNAVAILABLE -> {
                deviceTelemetry.location!!.lat = null
                deviceTelemetry.location!!.lon = null
                deviceTelemetry.location!!.alt = null
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing to do
    }

    override fun onProviderEnabled(provider: String?) {
        // Nothing to do
    }

    override fun onProviderDisabled(provider: String?) {
        // Nothing to do
    }
}