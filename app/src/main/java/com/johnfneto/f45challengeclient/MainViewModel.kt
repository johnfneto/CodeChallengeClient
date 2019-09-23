package com.johnfneto.f45challengeclient

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = javaClass.simpleName
    private val parentJob = Job()

    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private lateinit var activity: MainActivity

    internal fun callServer(url: String, activity: MainActivity) {
        this.activity = activity
        connectToServer(url)
    }

    private fun connectToServer(url: String) {
        scope.launch {
            try {
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.setRequestProperty("User-Agent", "test")
                urlConnection.setRequestProperty("Connection", "close")
                urlConnection.connectTimeout = 5000
                urlConnection.readTimeout = 5000
                urlConnection.useCaches = false
                urlConnection.connect()
                // Callback to the activity with the result
                activity.onData(urlConnection.responseCode == 200)

            } catch (e: IOException) {
                Log.e(TAG, "Timeout or unreachable host or failed DNS lookup", e)
                // Callback to the activity with failure
                activity.onData(false)
            }
        }
    }
}