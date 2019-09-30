package com.senior.sensoraction

import android.content.Context
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.hardware.SensorManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Button
import android.widget.Toast
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var isPressed: Boolean = false
    val gyroArray = ArrayList<Array<Float>>()
    val accelArray = ArrayList<Array<Float>>()
    //val mutex = Semaphore(1);
    private val intentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //idk tbh
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager


        //register listener for sensor type
        //gyroscope
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        //accelerometer
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        //button
        val startBtn = findViewById<Button>(R.id.startBtn)
        startBtn.setOnClickListener {
            Toast.makeText(applicationContext, "start\n", Toast.LENGTH_SHORT).show()
            isPressed = true
        }

        val stopBtn = findViewById<Button>(R.id.stopBtn)
        stopBtn.setOnClickListener {
            Toast.makeText(applicationContext, "stop\n", Toast.LENGTH_SHORT).show()
            isPressed = false
            //removed while testing
            thread {
                sendData()
            }
        }
        //wifi direct setup

        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)


    }


    override fun onSensorChanged(event: SensorEvent?) {
        val gyroFloat = -999.9F
        val accelFloat = 0F

        //check which sensor had the event and update the ui accordingly
        if (isPressed)
            when {
                event!!.sensor.type == Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeTxt.text =
                        "GYROSCOPE biased:\n" + "X= ${event.values[0]}\n" + "Y=${event.values[1]} \n" + "Z=${event.values[2]}"
                    gyroArray.add(
                        arrayOf(
                            Calendar.getInstance().timeInMillis.toFloat(),
                            event.values[0],
                            event.values[1],
                            event.values[2],
                            gyroFloat
                        )
                    )
                }
                event.sensor.type == Sensor.TYPE_ACCELEROMETER -> {
                    accelTxt.text =
                        "accelerometer biased\n" + "X= ${event.values[0]}\n" + "Y=${event.values[1]} \n" + "Z=${event.values[2]}"
                    accelArray.add(
                        arrayOf(
                            Calendar.getInstance().timeInMillis.toFloat(),
                            event.values[0],
                            event.values[1],
                            event.values[2],
                            accelFloat
                        )
                    )
                }
            }
        else {
            gyroscopeTxt.text = "GYROSCOPE biased:\n" + "not recording"
            accelTxt.text = "accelerometer biased\n" + "not recording"
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    fun sendData() {
        val address: String = ipTxt.text.toString()
        val port: Int = with(portTxt) { text.toString().toInt() }
        if (address != "" && port != 0) {
            val connection = Socket(address, port)
            Looper.prepare()
            Toast.makeText(
                applicationContext,
                "Connected to server at $address on port $port",
                Toast.LENGTH_SHORT
            ).show()
            val writer: OutputStream = connection.getOutputStream()
            writer.write("time,x,y,z,type,".toByteArray())
            for (i in gyroArray) {
                writer.write("t=${i[0]},x=${i[1]},y=${i[2]},z=${i[3]},type=${i[4]},".toByteArray())
            }
            for (i in accelArray) {
                writer.write("t=${i[0]},x=${i[1]},y=${i[2]},z=${i[3]},type=${i[4]},".toByteArray())
            }
            writer.write("~~~AST~~~".toByteArray())
            connection.close()
            gyroArray.clear()
            accelArray.clear()
        }
    }

}
