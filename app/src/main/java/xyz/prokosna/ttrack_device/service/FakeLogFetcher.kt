package xyz.prokosna.ttrack_device.service

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.prokosna.ttrack_device.model.FakeLog

class FakeLogFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetch(uri: String): FakeLog {
        val req = Request.Builder().url(uri).get().build()
        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            throw RuntimeException("Fetching a fake log was failed: " + res.code())
        }
        return gson.fromJson(res.body().string(), FakeLog::class.java)
    }
}