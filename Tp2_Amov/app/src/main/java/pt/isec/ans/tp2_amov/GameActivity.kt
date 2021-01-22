package pt.isec.ans.tp2_amov

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.concurrent.thread


class GameActivity : AppCompatActivity(), LocationListener, OnMapReadyCallback {
    lateinit var nomeEquipa: String
    lateinit var idJogador: String
    lateinit var floc: FusedLocationProviderClient
    lateinit var currentLocation: Location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var nMapBoundary: LatLngBounds
    lateinit var mMap: GoogleMap

    val ISEC = LatLng(40.1925, -8.4115)
    var locActive = false
    var db = FirebaseFirestore.getInstance()
    var locEnabled = false
    var flag: Boolean = false
    var tempoEsperaUpdates = 2000
    var nPlayers: Int = 0

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        nomeEquipa = intent.getStringExtra("NOME_EQUIPA").toString()
        nPlayers = intent.getIntExtra("NJOGADORES", 0)
        idJogador = intent.getStringExtra("IDJOGADOR").toString()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        currentLocation = Location("")

        RequestPermission()

        getLastLocation()

        floc = LocationServices.getFusedLocationProviderClient(this)

        (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync(this)

        threadData()
    }

//    fun setCameraView() {
//        var botBound: Double = currentLocation.latitude - .1
//        var leftBound: Double = currentLocation.longitude - .1
//        var topBound: Double = currentLocation.latitude + .1
//        var rigthBound: Double = currentLocation.longitude + .1
//
//        nMapBoundary = LatLngBounds(LatLng(botBound, leftBound), LatLng(topBound, rigthBound))
//
//        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(nMapBoundary, 0))
//    }

    private fun threadData() {

        var correThread = true
        thread {
            do {

                var data = hashMapOf(
                    "${idJogador}" to arrayListOf(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                )
                db.collection("Equipas").document("${nomeEquipa}").set(data, SetOptions.merge())
                val docRef = db.collection("Equipas").document("${nomeEquipa}")

                Thread.sleep(tempoEsperaUpdates.toLong())
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            atualizaMapa(document.data as MutableMap<String, Any>)
                        }
                    }
            } while (correThread)
        }
    }

    private fun atualizaMapa(data: MutableMap<String, Any>) {
        var listCords: ArrayList<LatLng> = ArrayList()

        for (key in data) {
            var player = key.key

            if (player != "flag") {
                var list: ArrayList<Double> = key.value as ArrayList<Double>
                var latitude = list[0]
                var longitude = list[1]

                if (latitude == 0.0 || longitude == 0.0) {
                    flag = false
                    var docData = hashMapOf("flag" to false)
                    db.collection("Equipas").document("${nomeEquipa}")
                        .set(docData, SetOptions.merge())
                    return
                } else {
                    listCords.add(LatLng(latitude, longitude))
                    Log.d("log", "[$player]: ($latitude ,  $longitude)")
                }
            } else {
                flag = key.value as Boolean
            }
        }

        var docData = hashMapOf("flag" to true)
        db.collection("Equipas").document("${nomeEquipa}")
            .set(docData, SetOptions.merge())

        fazPoligno(listCords)

    }

    private fun fazPoligno(listCords: ArrayList<LatLng>) {
//        var auxList = listCords
//        for (obj in auxList){
//            val loc1 = Location(LocationManager.GPS_PROVIDER)
//            val loc2 = Location(LocationManager.GPS_PROVIDER)
//
//            loc1.latitude = obj.latitude
//            loc1.longitude = obj.longitude
//
//            var distance = 200.0
//            for (objj in auxList){
//                if(!objj.equals(obj)){
//                    loc2.latitude = objj.latitude
//                    loc2.longitude = objj.longitude
//                    if (loc1.distanceTo(loc2)<distance){
//
//                    }
//                }
//            }
//
//
//        }

        for (obj in listCords) {
            if (flag == true) {
                val pol = PolygonOptions()
                pol.strokeColor(Color.CYAN)
                pol.fillColor(Color.GRAY)
                pol.visible(true)
                mMap.clear()
                for (cord in listCords)
                    pol.add(cord)
                mMap.addPolygon(pol)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates(true)
    }

    override fun onPause() {
        super.onPause()
        if (locActive) {
            floc.removeLocationUpdates(locationCallback)
            locActive = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startLocationUpdates(false)
    }

    fun startLocationUpdates(askPerm: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (askPerm)
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 12
                )
            else
                finish()
            return
        }

        if (!locEnabled) {
            val locReq = LocationRequest().apply {
                interval = 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            floc.requestLocationUpdates(locReq, locationCallback, null)
            locEnabled = true
        }
    }


    override fun onLocationChanged(location: Location) {
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap?) {
        map ?: return
        mMap = map
        mMap.isMyLocationEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true

        val cp = CameraPosition.Builder().target(ISEC).zoom(17f)
            .bearing(0f).tilt(0f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp))
    }


    fun CheckPermission(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    fun RequestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            101
        )
    }

    fun isLocationEnabled(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        if (CheckPermission()) {
            if (isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        NewLocationData()
                    } else {
                        currentLocation = location
                        Log.i(
                            "Debug:",
                            "1your last last location: " + location.latitude.toString() + " " + location.longitude.toString()
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Please Turn on Your device Location", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            RequestPermission()
        }
    }

    @SuppressLint("MissingPermission")
    fun NewLocationData() {
        var locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var lastLocation: Location = locationResult.lastLocation
            currentLocation = lastLocation
            Log.i(
                "Debug:",
                "2your last last location: " + lastLocation.latitude.toString() + " " + lastLocation.longitude.toString()
            )
        }
    }
}



