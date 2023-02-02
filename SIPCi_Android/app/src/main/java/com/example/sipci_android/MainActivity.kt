package com.example.sipci_android


import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.ingenieriajhr.blujhr.BluJhr
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var permisosOnBluetooth = false
    var requiredPermissions = listOf<String>()
    var devicesBluetooth = ArrayList<String>()

    var x = ""
    var y = ""
    var linea = ""
    var status = false

    lateinit var blue:BluJhr
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        blue = BluJhr(this)
        blue.onBluetooth()

        Intent(this, KeepOpen::class.java).also { intent ->
            startService(intent)
        }

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
                        y = location.latitude.toString()
                        x = location.longitude.toString()
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
                    setAlarmB()
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

    fun setAlarmI() {
        try{
            blue.bluTx("1")
            val mp = MediaPlayer.create(applicationContext, R.raw.alarma)
            if(mp.isPlaying && status){
                mp.reset()
                mp.stop()
                mp.release()
                status = false
            }
            else if (!status) {
                mp.start()
                mp.setLooping(true)
                status = true
            }
        }
        catch(e: Exception) {
            Toast.makeText(applicationContext,"No BT connected",Toast.LENGTH_LONG).show()
        }
        val mp = MediaPlayer.create(applicationContext, R.raw.alarma)
        if(mp.isPlaying && status){
            mp.reset()
            mp.stop()
            mp.release()
            status = false
        }
        else if (!status) {
            mp.start()
            mp.setLooping(true)
            status = true
        }
    }

    fun setAlarmB() {
        val mp = MediaPlayer.create(applicationContext, R.raw.alarma)
        if(mp.isPlaying && status){
            mp.reset()
            mp.stop()
            mp.release()
            status = false
        }
        else if (!status) {
            mp.start()
            mp.setLooping(true)
            status = true
        }
    }

    fun setAlarm(view: View) {
        try{
            blue.bluTx("1")
            val mp = MediaPlayer.create(applicationContext, R.raw.alarma)
            if(mp.isPlaying && status){
                mp.reset()
                mp.stop()
                mp.release()
                status = false
            }
            else if (!status) {
                mp.start()
                mp.setLooping(true)
                status = true
            }
        }
        catch(e: Exception) {
            Toast.makeText(applicationContext,"No BT connected",Toast.LENGTH_LONG).show()
        }
        val mp = MediaPlayer.create(applicationContext, R.raw.alarma)
        if(mp.isPlaying && status){
            mp.reset()
            mp.stop()
            mp.release()
            status = false
        }
        else if (!status) {
            mp.start()
            mp.setLooping(true)
            status = true
        }
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
        val queue = Volley.newRequestQueue(this)
        val url = "https://sipciait.pythonanywhere.com/botonazo/?a="+x+"&b="+y
        println(url)
        val srtRQ = StringRequest(Request.Method.GET,url, { response ->
            this.linea = response.toString() }, { error -> println(error)}
            )
        var nLinea = this.linea
        nLinea = nLinea.replace("[","")
        nLinea = nLinea.replace("]","")
        println(nLinea)
        var data = nLinea
//        var data = nLinea.toFloatOrNull()
        if (data > "70"){
            back.setBackgroundResource(R.color.DangerBG)
        }
        else if (data > "30" && data < "80"){
            back.setBackgroundResource(R.color.MediumBG)
        }
        else{
            back.setBackgroundResource(R.color.BasicBG)
        }
        queue.add(srtRQ)
    }

    private val PRESS_INTERVAL = 700
    private var mUpKeyEventTime: Long = 0
    @Override
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_VOLUME_DOWN == event.keyCode) {
            if (event.eventTime - mUpKeyEventTime < PRESS_INTERVAL) {
                setAlarmI()
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Override
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
            mUpKeyEventTime = event.eventTime
        }
        return super.onKeyUp(keyCode, event)
    }
}

class KeepOpen : Service(){
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
       //Do nothing
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }
}
