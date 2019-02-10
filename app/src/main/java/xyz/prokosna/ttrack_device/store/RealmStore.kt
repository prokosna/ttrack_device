package xyz.prokosna.ttrack_device.store

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import xyz.prokosna.ttrack_device.model.DeviceTelemetry
import xyz.prokosna.ttrack_device.model.MetaStats


class RealmStore(context: Context) {
    init {
        Realm.init(context)
        val config = RealmConfiguration.Builder().build()
        // Realm.setDefaultConfiguration(config)
        Realm.deleteRealm(config)

        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val ret = realm.where(MetaStats::class.java).findAll()
            if (ret.count() <= 0) {
                realm.insert(MetaStats())
            }
        }
    }

    fun addDeviceTelemetry(deviceTelemetry: DeviceTelemetry) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            realm.insert(deviceTelemetry)

            val meta = realm.where(MetaStats::class.java).findFirst()
            if (meta != null) {
                meta.queueNum + 1
            }
        }
    }

    fun removeDeviceTelemetry(deviceTelemetry: DeviceTelemetry) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction { r ->
            val telemetry = realm.where(DeviceTelemetry::class.java)
                .equalTo("id", deviceTelemetry.id)
                .findAll()
            telemetry.deleteAllFromRealm()

            val meta = r.where(MetaStats::class.java).findFirst()
            if (meta != null) {
                meta.queueNum - 1
                meta.totalNum + 1
            }
        }
    }

    fun findAllDeviceTelemetry(): List<DeviceTelemetry> {
        val realm = Realm.getDefaultInstance()
        return realm.where(DeviceTelemetry::class.java).findAll()
    }

    fun getMetaStats(): MetaStats {
        val realm = Realm.getDefaultInstance()
        val meta = realm.where(MetaStats::class.java).findFirst()
        if (meta != null) {
            return meta
        } else {
            throw RuntimeException("No MetaStats instances in Realm DB")
        }
    }

    fun clearAll() {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val telemetries = realm.where(DeviceTelemetry::class.java).findAll()
            telemetries.deleteAllFromRealm()
            val meta = realm.where(MetaStats::class.java).findAll()
            meta.deleteAllFromRealm()
        }
    }

    fun close() {
        val realm = Realm.getDefaultInstance()
        realm.close()
    }
}