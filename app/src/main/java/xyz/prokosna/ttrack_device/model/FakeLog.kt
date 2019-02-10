package xyz.prokosna.ttrack_device.model

data class FakeLog(
    val host: String?,
    val ip: String?,
    val method: String?,
    val uri: String?,
    val size: Long?,
    val reqsize: Long?,
    val referer: String?,
    val status: String?,
    val ua: String?,
    val reqtime: Float?,
    val geo: String?
)