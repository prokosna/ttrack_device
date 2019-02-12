package xyz.prokosna.ttrack_device.service

import android.util.Log
import com.google.gson.GsonBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import xyz.prokosna.ttrack_device.model.Track
import java.security.PrivateKey
import java.time.Instant
import java.util.*

private const val topic = "/devices/GalaxyNote9/events"
private const val qos = 1
private const val clientId = "projects/topy-biz-ttrack/locations/asia-east1/registries/topy-devices/devices/GalaxyNote9"

class TrackSender(uri: String, val privateKey: PrivateKey) {
    private val client = MqttClient(uri, clientId, MemoryPersistence())
    private val connOpts = MqttConnectOptions()
    private val gson = GsonBuilder().serializeNulls().create()

    init {
        connect()
    }

    fun send(track: Track) {
        if (!client.isConnected) {
            connect()
            if (!client.isConnected) {
                throw RuntimeException("Failed to re-connect")
            }
        }
        val message = MqttMessage(gson.toJson(track).toByteArray())
        message.qos = qos
        Log.i("debug", gson.toJson(track))
        client.publish(topic, message)
    }

    fun close() {
        client.disconnect()
    }

    private fun connect() {
        connOpts.isCleanSession = true
        connOpts.password = createJwt("topy-biz-ttrack", privateKey, SignatureAlgorithm.RS256).toCharArray()
        val properties = Properties()
        properties.setProperty("com.ibm.ssl.protocol", "TLSv1.2")
        connOpts.sslProperties = properties
        connOpts.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        connOpts.userName = "unused"
        try {
            client.connect(connOpts)
            Log.i("debug", "MQTT client connected!")
        } catch (e: MqttException) {
            Log.w("debug", "Failed to connect: $e")
        }
    }

    private fun createJwt(projectId: String, privateKey: PrivateKey, alg: SignatureAlgorithm): String {
        val now = Instant.now()
        val jwtBuilder = Jwts.builder()
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(3600)))
            .setAudience(projectId)
        return jwtBuilder.signWith(alg, privateKey).compact()
    }
}