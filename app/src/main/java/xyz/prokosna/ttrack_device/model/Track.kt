package xyz.prokosna.ttrack_device.model

import com.google.gson.annotations.SerializedName

data class Track(
    @SerializedName("telemetry")
    var telemetry: DeviceTelemetry,
    @SerializedName("log")
    var log: FakeLog
)