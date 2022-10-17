package com.example.sipci_android


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.ingenieriajhr.blujhr.BluJhr
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.LocalDate
import java.time.LocalDate.*
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    var permisosOnBluetooth = false
    var requiredPermissions = listOf<String>()
    var devicesBluetooth = ArrayList<String>()

    var x = ""
    var y = ""

    lateinit var blue:BluJhr
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        blue = BluJhr(this)
        blue.onBluetooth()

        listDeviceBluetooth.setOnItemClickListener { adapterView, view, i, l ->
            if (devicesBluetooth.isNotEmpty()) {
                blue.connect(devicesBluetooth[i])
                blue.setDataLoadFinishedListener(object : BluJhr.ConnectedBluetooth {
                    override fun onConnectState(state: BluJhr.Connected) {
                        when (state) {

                            BluJhr.Connected.True -> {
                                Toast.makeText(applicationContext, "True", Toast.LENGTH_SHORT)
                                    .show()
                                listDeviceBluetooth.visibility = View.GONE
                                imageButton.visibility = View.VISIBLE
                                rxReceived()
                            }

                            BluJhr.Connected.Pending -> {
                                Toast.makeText(applicationContext, "Pending", Toast.LENGTH_SHORT)
                                    .show()

                            }

                            BluJhr.Connected.False -> {
                                Toast.makeText(applicationContext, "False", Toast.LENGTH_SHORT)
                                    .show()
                            }

                            BluJhr.Connected.Disconnect -> {
                                Toast.makeText(applicationContext, "Disconnect", Toast.LENGTH_SHORT)
                                    .show()
                                listDeviceBluetooth.visibility = View.VISIBLE
                                imageButton.visibility = View.GONE
                            }

                        }
                    }
                })
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        x =  location.latitude.toString()
                        y = location.longitude.toString()
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            x =  mLastLocation.latitude.toString()
            y = mLastLocation.longitude.toString()
        }
    }

    private fun checkPermissions(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun rxReceived() {
        blue.loadDateRx(object:BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
//                consola.text = consola.text.toString()+rx
                if (rx == "alarm"){
                    Toast.makeText(applicationContext, "Conexion!!!!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    val PERMISSION_ID = 42
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (blue.checkPermissions(requestCode,grantResults)){
            Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show()
            blue.initializeBluetooth()
        }else{
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
                blue.initializeBluetooth()
            }else{
                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!blue.stateBluetoooth() && requestCode == 100){
            blue.initializeBluetooth()
        }else{
            if (requestCode == 100){
                devicesBluetooth = blue.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    listDeviceBluetooth.adapter = adapter
                }else{
                    Toast.makeText(this, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun selectBluetoothDevice(view: View){
        listDeviceBluetooth.visibility = View.VISIBLE
        imageButton.visibility = View.GONE
        blueButton.visibility = View.GONE
    }

    fun setAlarm(view: View) {
        try{
            blue.bluTx("1")
            Toast.makeText(applicationContext, "Sent??", Toast.LENGTH_SHORT).show()
        }
        catch(e: Exception) {
            Toast.makeText(applicationContext,"No BT connected",Toast.LENGTH_LONG).show()
        }
        // Upload data
    }

    override fun onBackPressed() {
    try {
        if (listDeviceBluetooth.visibility.equals("VISIBLE")) {
            listDeviceBluetooth.visibility = View.GONE
            imageButton.visibility = View.VISIBLE
            blueButton.visibility = View.VISIBLE
            Toast.makeText(applicationContext,"failed",Toast.LENGTH_SHORT).show()
        }
        super.onBackPressed()
    }
    catch(e: Exception) {
        Toast.makeText(applicationContext, "failed", Toast.LENGTH_SHORT).show()
    }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkData(view: View){
        getLocation()
        var time = getTime()
        val url = URL("https://")
        val connection = url.openConnection()
        BufferedReader(InputStreamReader(connection.getInputStream())).use { inp ->
            var line: String?
            while (inp.readLine().also { line = it } != null) {
                var data = line!!.toFloat()
                val root = back.rootView
                if (data > 70){
                    root.setBackgroundColor(0xFFEA09)
                }
                else if (data > 30 && data < 80){
                    root.setBackgroundColor(0xFFC300)
                }
                else{
                    root.setBackgroundColor(0x00D530)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTime(): String {
        val date: LocalDate = LocalDate.now()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
        val text: String = date.format(formatter)
        val parsedDate: LocalDate = LocalDate.parse(text, formatter)
        return parsedDate.toString()
    }

}

