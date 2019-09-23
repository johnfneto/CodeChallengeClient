package com.johnfneto.f45challengeclient

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import java.net.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), OnDataCallback {

    private val TAG = javaClass.simpleName
    private val PORT = 12345
    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO
    private val scope = CoroutineScope(coroutineContext)
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        findViewById<TextView>(R.id.clientInfo).text = String.format("Client (phone 2) :" + getLocalIpAddress())

        findViewById<Button>(R.id.listenButton).setOnClickListener {
            startReceiving()
        }
    }

    /**
     * Callback from #MainViewModel
     *
     * Shows a Toast informing the user if the device was able to it's IP to the Server
     */
    override fun onData(success: Boolean) {
        runOnUiThread(Runnable {
            if (success) {
                Toast.makeText(this, "IP successfully sent to the Server", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "There was an error sending IP to the Server", Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Function used to listen to broadcasts.
     *
     * Waits until it receives a valid IP and calls viewmodel to make an http call to the server
     *
     */
    private fun startReceiving() {
        scope.launch {
            var serverIP: String
            val message = ByteArray(1500)
            try {
                val packet = DatagramPacket(message, message.size)
                val socket = DatagramSocket(PORT)
                // Waits until it receives a valid IP
                while (true) {
                    socket.receive(packet)
                    serverIP = String(message, 0, packet.length)
                    Log.d(TAG, "message :$serverIP")
                    if (isIPAddress(serverIP)) {
                        break
                    }
                }
                socket.close()

                // Call to viewModel to make a http cal to the server
                viewModel.callServer("http://$serverIP:$PORT/?ip=" + getLocalIpAddress(), this@MainActivity)

                // on the main thread updates the UI
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.clientInfo).text = String.format("Received Server IP :$serverIP")
                }
            } catch (e: Exception) {
                Log.e(TAG, "error  $e")
            }
        }
    }

    /**
     * Function to determine if the @param ip is an IP address
     *
     * @param ip
     * Returns true if @param ip is a valid IP address
     */
    private fun isIPAddress(ip: String): Boolean {
        return try {
            val inet = InetAddress.getByName(ip)
            inet.hostAddress == ip && inet is Inet4Address
        } catch (e: UnknownHostException) {
            false
        }
    }

    /**
     * Function used to get the device's IP address
     *
     * Returns a formatted ip address
     */
    private fun getLocalIpAddress(): String {
        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiMan.connectionInfo.ipAddress
        val ip = String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
        Log.d(TAG, "IP :$ip")
        return ip
    }
}