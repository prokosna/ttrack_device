package xyz.prokosna.ttrack_device.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class MetaStats : RealmObject() {
    @PrimaryKey
    var id: String = "metadata"
    var queueNum: Long = 0
    var totalNum: Long = 0
}