package com.example.temisap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.temisap.network.ODataResponse
import com.example.temisap.network.SapCommand
import com.example.temisap.network.SapHanaApiClient
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class MainActivity : AppCompatActivity(), OnGoToLocationStatusChangedListener {
    private lateinit var robot: Robot
    private lateinit var tvSapStatus: TextView
    private lateinit var tvRobotStatus: TextView
    private lateinit var btnFetchData: Button
    private lateinit var btnDemoMode: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvSapStatus = findViewById(R.id.tvSapStatus)
        tvRobotStatus = findViewById(R.id.tvRobotStatus)
        btnFetchData = findViewById(R.id.btnFetchData)
        btnDemoMode = findViewById(R.id.btnDemoMode)
        robot = Robot.getInstance()
        btnFetchData.setOnClickListener {
            fetchCommandFromSap()
        }
        btnDemoMode.setOnClickListener {
            runDemo()
        }
    }
    override fun onStart() {
        super.onStart()
        robot.addOnGoToLocationStatusChangedListener(this)
    }
    override fun onStop() {
        super.onStop()
        robot.removeOnGoToLocationStatusChangedListener(this)
    }
    private fun runDemo() {
        val demoSapTarget = "Kush A"
        val mappedLocation = mapSapTargetToRobotLocation(demoSapTarget)

        val savedLocations = robot.locations
        if (savedLocations.contains(mappedLocation)) {
            updateSapStatus("MOCK: Moving to $mappedLocation", "#4CAF50")
            speak("Demo Mode. Moving to $mappedLocation")
            robot.goTo(mappedLocation)
        } else {
            val errString = "Error: '$mappedLocation' not found. Available: $savedLocations"
            updateSapStatus(errString, "#F44336")
            speak("I do not know where $mappedLocation is.")
            Log.e("SAP_HANA", errString)
        }
    }
    // Helper function to map SAP floorplan names to robotemi internal map names
    private fun mapSapTargetToRobotLocation(sapTarget: String): String {
        return when (sapTarget.trim().lowercase()) {
            "kush a" -> "future of work"
            // You can add more mappings here in the future:
            // "warehouse a" -> "Warehouse 1"
            // "kitchen" -> "Cafeteria"
            else -> sapTarget // If no exact mapping exists, just rely on the original string
        }
    }
    private fun fetchCommandFromSap() {
        updateSapStatus("Connecting to SAP...", "#FF9800")
        SapHanaApiClient.instance.getPendingCommands().enqueue(object : Callback<ODataResponse> {
            override fun onResponse(call: Call<ODataResponse>, response: Response<ODataResponse>) {
                if (response.isSuccessful) {
                    val commands = response.body()?.value
                    if (!commands.isNullOrEmpty()) {
                        // Find a pending MOVE command
                        val command = commands.find { it.status == "PENDING" && it.action == "MOVE" }
                        if (command != null) {
                            val target = command.target
                            val mappedLocation = mapSapTargetToRobotLocation(target)

                            val savedLocations = robot.locations
                            if (savedLocations.contains(mappedLocation)) {
                                updateSapStatus("Success: $target -> $mappedLocation", "#4CAF50")
                                speak("New command. Moving to $mappedLocation")
                                robot.goTo(mappedLocation)
                            } else {
                                val errString = "Error: '$mappedLocation' not found. Available: $savedLocations"
                                updateSapStatus(errString, "#F44336")
                                speak("I do not know where $mappedLocation is.")
                                Log.e("SAP_HANA", errString)
                            }
                        } else {
                            updateSapStatus("No pending MOVE commands.", "#9E9E9E")
                        }
                    } else {
                        updateSapStatus("SAP: Empty Command Queue", "#9E9E9E")
                    }
                } else {
                    val errorDetail = "SAP Error ${response.code()}"
                    updateSapStatus(errorDetail, "#F44336")
                    Log.e("SAP_HANA", errorDetail)
                }
            }
            override fun onFailure(call: Call<ODataResponse>, t: Throwable) {
                val failureMsg = "Network Error: Tunnel Offline"
                updateSapStatus(failureMsg, "#F44336")
                Log.e("SAP_HANA", "Request failed", t)
                speak("Connection to SAP failed.")
            }
        })
    }
    private fun updateSapStatus(text: String, colorHex: String) {
        runOnUiThread {
            tvSapStatus.text = text
            tvSapStatus.setTextColor(android.graphics.Color.parseColor(colorHex))
        }
    }
    private fun speak(text: String) {
        robot.speak(TtsRequest.create(text, false))
    }
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        runOnUiThread {
            tvRobotStatus.text = "Robot: $status ($location)"
            when (status) {
                OnGoToLocationStatusChangedListener.START -> tvRobotStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                OnGoToLocationStatusChangedListener.COMPLETE -> {
                    tvRobotStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    speak("Task completed at $location.")
                }
                OnGoToLocationStatusChangedListener.ABORT -> {
                    tvRobotStatus.setTextColor(android.graphics.Color.RED)
                    speak("Movement aborted.")
                }
            }
        }
    }
}