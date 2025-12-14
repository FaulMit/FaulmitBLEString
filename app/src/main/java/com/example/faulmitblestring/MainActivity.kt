package com.example.faulmitblestring


import android.graphics.Color
import android.view.View
import android.widget.SeekBar
import android.widget.Button
import android.widget.TextView
import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var isWriting = false
    private val writeQueue: Queue<String> = LinkedList()

    private val TAG = "FaulMitBLE"

    // Твой модуль: сервис 0xFFE0, характеристика 0xFFE1
    private val UART_SERVICE_UUID: UUID =
        UUID.fromString("0000FFE0-0000-1000-8000-00805f9b34fb")
    private val UART_CHAR_UUID: UUID =
        UUID.fromString("0000FFE1-0000-1000-8000-00805f9b34fb")

    // MAC-адрес модуля (возьми из nRF Connect)
    private val DEVICE_ADDRESS = "D2:ED:EB:1C:90:5D"

    // UI
    private lateinit var txtStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var edtText: EditText
    private lateinit var btnSendText: Button
    private lateinit var seekBrightness: SeekBar
    private lateinit var btnSendBrightness: Button
    private lateinit var txtBrightValue: TextView
    private lateinit var seekSpeed: SeekBar
    private lateinit var btnSendSpeed: Button
    private lateinit var txtSpeedValue: TextView
    private lateinit var btnModeStatic: Button
    private lateinit var btnModeRainbow: Button
    private lateinit var btnModeMulti: Button
    // RGB UI
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var txtRVal: TextView
    private lateinit var txtGVal: TextView
    private lateinit var txtBVal: TextView
    private lateinit var colorPreview: View
    private lateinit var btnSendRgb: Button

    private var currentR = 255
    private var currentG = 255
    private var currentB = 255



    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var uartCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    // Запрос runtime-разрешений
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.values.any { it }) {
                connectByMac()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        txtStatus = findViewById(R.id.txtStatus)
        btnConnect = findViewById(R.id.btnConnect)
        edtText = findViewById(R.id.edtText)
        btnSendText = findViewById(R.id.btnSendText)
        seekBrightness = findViewById(R.id.seekBrightness)
        btnSendBrightness = findViewById(R.id.btnSendBrightness)
        txtBrightValue = findViewById(R.id.txtBrightValue)
        seekSpeed = findViewById(R.id.seekSpeed)
        btnSendSpeed = findViewById(R.id.btnSendSpeed)
        txtSpeedValue = findViewById(R.id.txtSpeedValue)
        btnModeStatic = findViewById(R.id.btnModeStatic)
        btnModeRainbow = findViewById(R.id.btnModeRainbow)
        btnModeMulti = findViewById(R.id.btnModeMulti)
        btnModeStatic.setOnClickListener { sendMode(0) }
        btnModeRainbow.setOnClickListener { sendMode(1) }
        btnModeMulti.setOnClickListener { sendMode(2) }
        seekR = findViewById<SeekBar>(R.id.seekR)
        seekG = findViewById<SeekBar>(R.id.seekG)
        seekB = findViewById<SeekBar>(R.id.seekB)

        txtRVal = findViewById<TextView>(R.id.txtRVal)
        txtGVal = findViewById<TextView>(R.id.txtGVal)
        txtBVal = findViewById<TextView>(R.id.txtBVal)

        colorPreview = findViewById<View>(R.id.colorPreview)
        btnSendRgb = findViewById<Button>(R.id.btnSendRgb)

        seekR.max = 255
        seekG.max = 255
        seekB.max = 255

        seekR.progress = currentR
        seekG.progress = currentG
        seekB.progress = currentB

        txtRVal.text = currentR.toString()
        txtGVal.text = currentG.toString()
        txtBVal.text = currentB.toString()

        updatePreview()

        seekR.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentR = progress
                txtRVal.text = progress.toString()
                updatePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekG.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentG = progress
                txtGVal.text = progress.toString()
                updatePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentB = progress
                txtBVal.text = progress.toString()
                updatePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })



        seekR.max = 255
        seekG.max = 255
        seekB.max = 255

        seekR.progress = currentR
        seekG.progress = currentG
        seekB.progress = currentB

        updatePreview()



        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        btnConnect.setOnClickListener {
            checkPermissionsAndStart()
        }

        btnSendRgb.setOnClickListener {
            sendRgb(currentR, currentG, currentB)
        }


        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                txtBrightValue.text = "Brightness: $progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                txtSpeedValue.text = "Speed: $progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnSendText.setOnClickListener {
            val text = edtText.text.toString()
            if (text.isNotEmpty()) {
                sendPacket("#$text")
            } else {
                Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendBrightness.setOnClickListener {
            val value = seekBrightness.progress.coerceIn(0, 255)
            sendPacket("\$3,$value;")
        }

        btnSendSpeed.setOnClickListener {
            val value = seekSpeed.progress.coerceIn(0, 100)
            sendPacket("\$2,$value;")
        }
    }

    // ---------- Permissions ----------
    private fun checkPermissionsAndStart() {
        val needed = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) needed += Manifest.permission.BLUETOOTH_SCAN

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) needed += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) needed += Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            connectByMac()
        }
    }


    private fun connectByMac() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Bluetooth выключен", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            txtStatus.text = "Status: Connecting by MAC..."
            val device = adapter.getRemoteDevice(DEVICE_ADDRESS)
            connectToDevice(device)
        } catch (e: IllegalArgumentException) {
            txtStatus.text = "Status: Wrong MAC address"
            Toast.makeText(this, "Неверный MAC-адрес устройства", Toast.LENGTH_SHORT).show()
        }
    }


    private fun connectToDevice(device: BluetoothDevice) {
        txtStatus.text = "Status: Connecting to ${device.name}..."
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected, discovering services")
                    runOnUiThread {
                        txtStatus.text = "Status: Connected, discovering services..."
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected")
                    uartCharacteristic = null
                    runOnUiThread {
                        txtStatus.text = "Status: Disconnected"
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UART_SERVICE_UUID)
                if (service != null) {
                    uartCharacteristic = service.getCharacteristic(UART_CHAR_UUID)
                    uartCharacteristic?.writeType =
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                    runOnUiThread {
                        txtStatus.text = "Status: Ready"
                        Toast.makeText(
                            this@MainActivity,
                            "Connected to UART",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e(TAG, "UART service not found")
                    runOnUiThread {
                        txtStatus.text = "Status: UART service not found"
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered failed: $status")
            }
        }
    }

    // ---------- Send data ----------
    private fun sendPacket(text: String) {
        val gatt = bluetoothGatt
        val ch = uartCharacteristic

        if (gatt == null || ch == null) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Send: $text")

        ch.value = text.toByteArray(Charsets.UTF_8)
        val ok = gatt.writeCharacteristic(ch)
        if (!ok) {
            Toast.makeText(this, "Write failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendColor(index: Int) {
        // Команда Гайвера: $5,<номер>;
        val cmd = "\$5,$index;"
        sendPacket(cmd)
    }
    private fun sendMode(index: Int) {
        // Команда Гайвера: $4,<режим>;
        val cmd = "\$4,$index;"
        sendPacket(cmd)
    }
    private fun updatePreview() {
        val c = Color.rgb(currentR, currentG, currentB)
        colorPreview.setBackgroundColor(c)
    }

    private fun sendRgb(r: Int, g: Int, b: Int) {
        // Новая команда: $7,R,G,B;
        val cmd = "\$7,$r,$g,$b;"
        sendPacket(cmd)
    }



    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}
