package xyz.prokosna.ttrack_device.model

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class DeviceTelemetry() : RealmObject() {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()
    @SerializedName("loc")
    var location: LocationValue? = LocationValue()
    @SerializedName("acc")
    var accelerometer: Accelerometer? = Accelerometer()
    @SerializedName("gyro")
    var gyroscope: Gyroscope? = Gyroscope()
    @SerializedName("rot")
    var rotationVector: RotationVector? = RotationVector()
    @SerializedName("mag")
    var magneticField: MagneticField? = MagneticField()
    @SerializedName("prox")
    var proximity: Float? = null
    @SerializedName("temp")
    var temperature: Float? = null
    @SerializedName("light")
    var light: Float? = null
    @SerializedName("press")
    var pressure: Float? = null
    @SerializedName("humid")
    var humidity: Float? = null

    fun copy(): DeviceTelemetry {
        val dest = DeviceTelemetry()
        dest.location = LocationValue(location?.lat, location?.lon, location?.alt)
        dest.accelerometer = Accelerometer(accelerometer?.x, accelerometer?.y, accelerometer?.z)
        dest.gyroscope = Gyroscope(gyroscope?.x, gyroscope?.y, gyroscope?.z)
        dest.rotationVector = RotationVector(rotationVector?.x, rotationVector?.y, rotationVector?.z, rotationVector?.w)
        dest.magneticField = MagneticField(magneticField?.x, magneticField?.y, magneticField?.z)
        dest.proximity = proximity
        dest.temperature = temperature
        dest.light = light
        dest.pressure = pressure
        dest.humidity = humidity
        return dest
    }
}

open class LocationValue(
    var lat: Double? = null,
    var lon: Double? = null,
    var alt: Double? = null
) : RealmObject()

open class Accelerometer(
    var x: Float? = null,
    var y: Float? = null,
    var z: Float? = null
) : RealmObject()

open class Gyroscope(
    var x: Float? = null,
    var y: Float? = null,
    var z: Float? = null
) : RealmObject()

open class RotationVector(
    var x: Float? = null,
    var y: Float? = null,
    var z: Float? = null,
    var w: Float? = null
) : RealmObject()

open class MagneticField(
    var x: Float? = null,
    var y: Float? = null,
    var z: Float? = null
) : RealmObject()