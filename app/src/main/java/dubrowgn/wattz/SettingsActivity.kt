package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView

const val settingsName = "settings"
const val settingsUpdateInd = "$namespace.settings-update-ind"

class SettingsActivity : Activity() {
    private val batteryReceiver = BatteryDataReceiver()
    private val serviceConnection = StatusServiceConnection()

    private lateinit var charging: TextView
    private lateinit var currentScalar: RadioGroup
    private lateinit var indicatorUnits: RadioLayout
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch
    private lateinit var power: TextView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showIndicator: Switch

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    inner class BatteryDataReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            debug("BatteryDataReceiver.onReceive()")

            if (intent == null)
                return

            val ind = getString(R.string.indeterminate)

            charging.text = intent.getStringExtra("charging") ?: ind
            power.text = intent.getStringExtra("power") ?: ind
        }
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        currentScalar.check(
            when (settings.getFloat("currentScalar", -1f)) {
                1000f -> R.id.currentScalar1000
                .001f -> R.id.currentScalar0_001
                else -> R.id.currentScalar1
            }
        )
        indicatorUnits.check(
            when (settings.getString("indicatorUnits", null)) {
                "A" -> R.id.indicatorA
                "Ah" -> R.id.indicatorAh
                "C" -> R.id.indicatorC
                "V" -> R.id.indicatorV
                "Wh" -> R.id.indicatorWh
                "%" -> R.id.indicatorPerc
                else -> R.id.indicatorW
            }
        )
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)
        showIndicator.isChecked = settings.getBoolean("showIndicator", true)
    }

    @SuppressLint("ApplySharedPref")
    private fun onChange() {
        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", invertCurrent.isChecked)
            .putFloat(
                "currentScalar",
                when(currentScalar.checkedRadioButtonId) {
                    R.id.currentScalar1000 -> 1000f
                    R.id.currentScalar0_001 -> 0.001f
                    else -> 1f
                }
            )
            .putString(
                "indicatorUnits",
                when (indicatorUnits.checkedRadioButtonId) {
                    R.id.indicatorA -> "A"
                    R.id.indicatorAh -> "Ah"
                    R.id.indicatorC -> "C"
                    R.id.indicatorV -> "V"
                    R.id.indicatorWh -> "Wh"
                    R.id.indicatorPerc -> "%"
                    else -> "W"
                }
            )
            .putBoolean("showIndicator", showIndicator.isChecked)
            .commit()

        sendBroadcast(Intent().setPackage(packageName).setAction(settingsUpdateInd))

        // the service stops its own indicator when disabled, but must be started to show it
        if (showIndicator.isChecked)
            startForegroundService(Intent(this, StatusService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        charging = findViewById(R.id.charging)
        currentScalar = findViewById(R.id.currentScalar)
        indicatorUnits = findViewById(R.id.indicatorUnits)
        invertCurrent = findViewById(R.id.invertCurrent)
        power = findViewById(R.id.power)
        showIndicator = findViewById(R.id.showIndicator)

        loadPrefs()

        currentScalar.setOnCheckedChangeListener { _, _ -> onChange() }
        indicatorUnits.checkChangedCallback = { _ -> onChange() }
        invertCurrent.setOnCheckedChangeListener { _, _ -> onChange() }
        showIndicator.setOnCheckedChangeListener { _, _ -> onChange() }
    }

    override fun onStart() {
        debug("onStart()")

        super.onStart()

        bindService(Intent(this, StatusService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        debug("onStop()")

        unbindService(serviceConnection)

        super.onStop()
    }

    override fun onPause() {
        debug("onPause()")

        unregisterReceiver(batteryReceiver)

        super.onPause()
    }

    override fun onResume() {
        debug("onResume()")

        super.onResume()

        registerReceiver(batteryReceiver, IntentFilter(batteryDataResp), RECEIVER_NOT_EXPORTED)
        sendBroadcast(Intent().setPackage(packageName).setAction(batteryDataReq))
    }

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    fun onBack(view: View) {
        finish()
    }
}
