import android.content.Context

import android.graphics.Bitmap

import android.graphics.Canvas

import android.graphics.Color

import android.os.Bundle

import android.widget.Button

import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap

import com.google.android.gms.maps.MapView

import com.google.android.gms.maps.model.*

import com.google.maps.DirectionsApi

import com.google.maps.GeoApiContext

import com.google.maps.model.Directions

import com.google.maps.model.LatLng

import com.google.maps.model.Route

import kotlinx.coroutines.GlobalScope

import kotlinx.coroutines.asCoroutineDispatcher

import kotlinx.coroutines.launch

import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    private lateinit var map: GoogleMap

    private lateinit var pickupMarker: Marker

    private lateinit var dropoffMarker: Marker

    private lateinit var carMarker: Marker

    private lateinit var carPlateTextView: TextView

    private lateinit var fareTextView: TextView

    private lateinit var distanceTextView: TextView

    private lateinit var durationTextView: TextView

    private lateinit var carIcon: BitmapDescriptor

    private lateinit var pickupIcon: BitmapDescriptor

    private lateinit var dropoffIcon: BitmapDescriptor

    private lateinit var polylineOptions: PolylineOptions

    private var polyline: Polyline? = null

    private lateinit var startButton: Button

    private lateinit var stopButton: Button

    private var isTracking = false

    private var pickupLocation: LatLng? = null

    private var dropoffLocation: LatLng? = null

    private var carLocation: LatLng? = null

    private var carPlate: String? = null

    private var distance: Double? = null

    private var duration: Double? = null

    private var fare: Double? = null

    private val directionsService = DirectionsApiService()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Khởi tạo MapView

        mapView = findViewById(R.id.mapView)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { googleMap ->

            map = googleMap

            map.setOnMapClickListener { latLng ->

                if (!isTracking) {

                    pickupLocation = latLng

                    pickupMarker.position = latLng

                    pickupMarker.isVisible = true

                    updateUI()

                }

            }

            // Khởi tạo các biểu tượng

            carIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(this, R.drawable.ic_car))

            pickupIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_pickup)

            dropoffIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_dropoff)

            polylineOptions = PolylineOptions().width(10f).color(Color.BLUE).geodesic(true)

            pickupMarker = map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pickup)).visible(false))

            dropoffMarker = map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_dropoff)).visible(false))

            carMarker = map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).icon(carIcon).flat(true).anchor(0.5f, 0.5f))

            // Khởi tạo các TextView

            carPlateTextView = findViewById(R.id.carPlateTextView)

            fareTextView = findViewById(R.id.fareTextView)

            distanceTextView = findViewById(R.id.distanceTextView)

            durationTextView = findViewById(R.id.durationTextView)

            // Khởi tạo các Button

            startButton = findViewById(R.id.startButton)

            stopButton = findViewById(R.id.stopButton)

            startButton.setOnClickListener {

                if (pickupLocation != null && dropoffLocation != null && !isTracking) {

                    startTracking()

                }

            }

            stopButton.setOnClickListener {

                if (isTracking) {

                    stopTracking()

                }

            }

        }

    }

    private fun startTracking() {

        isTracking = true

        startButton.isEnabled = false

        stopButton.isEnabled = true

        // Hiển thị điểm đón và điểm đến trên bản đồ

        pickupMarker.isVisible = true

        dropoffMarker.isVisible = true

        pickupMarker.position = pickupLocation!!

        dropoffMarker.position = dropoffLocation!!

        // Lấy thông tin hành trình từ Google Maps Directions API

        GlobalScope.launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {

            val directions = directionsService.getDirections(pickupLocation!!, dropoffLocation!!)

            runOnUiThread {

                handleDirections(directions)

            }

        }

    }

    private fun stopTracking() {

        isTracking = false

        startButton.isEnabled = true

        stopButton.isEnabled = false

        // Ẩn điểm đón và điểm đến trên bản đồ

        pickupMarker.isVisible = false

        dropoffMarker.isVisible = false

        // Xóa đường đi trên bản đồ

        polyline?.remove()

        // Xóa thông tin hành trình

        distance = null

        duration = null

        fare = null

        carPlate = null

        // Cập nhật giao diện

        updateUI()

    }

    private fun handleDirections(directions: Directions) {

        // Lấy thông tin hành trình

        val route: Route = directions.routes[0]

        val legs = route.legs

        val distance = legs[0].distance.inMeters.toDouble() / 1000.0

        val duration = legs[0].duration.inSeconds.toDouble() / 60.0

if (routes.isNotEmpty()) {

    // truy cập các phần tử của mảng routes

}

        // Tính toán giá cước

        fare = calculateFare(baseFare, distanceFare, durationFare)

        // Lấy thông tin vị trí của xe

        val steps = legs[0].steps

        val lastStep = steps[steps.size - 1]

        val carLocation = lastStep.endLocation

        carPlate = "ABC 123"

        // Hiển thị thông tin hành trình trên bản đồ

        showRoute(route)

        showCar(carLocation)

        // Cập nhật giao diện

        this.distance = distance

        this.duration = duration

        updateUI()

    }

    private fun showRoute(route: Route) {

        // Xóa đường đi cũ (nếu có)

        if (polyline != null) {

    polyline.remove()

}

        // Vẽ đường đi mới

        val points = route.overviewPolyline.decodePath()

        for (point in points) {

            polylineOptions.add(LatLng(point.lat, point.lng))

        }

        polyline = map.addPolyline(polylineOptions)

    }

    private fun showCar(location: LatLng) {

        carLocation = location

        carMarker.position = location

        carMarker.rotation = getBearingBetweenTwoPoints(location, polyline!!.points[1])

        carPlateTextView.text = carPlate

    }

    private fun updateUI() {

        if (distance != null && duration != null && fare != null) {

            distanceTextView.text = "${"%.1f".format(distance)} km"

            durationTextView.text = "${"%.1f".format(duration)} min"

            fareTextView.text = "${"%.1f".format(fare)} USD"

        } else {

            distanceTextView.text = ""

            durationTextView.text = ""

            fareTextView.text = ""

        }

    }

    private fun calculateFare(distance: Double, duration: Double): Double {

        val distanceInKm = 25

        val baseFare = 12000

        val shortDistanceFare = 15000

        val longDistanceFare = 11000

        val wattingTime = 1000

        val distanceFare = if (distance <= distanceInKm) {

            distance * shortDistanceFare

        } else {

            distanceInKm * shortDistanceFare + (distance - distanceInKm) * longDistanceFare

        }

        val durationFare = duration * wattingTime

               return baseFare + distanceFare + durationFare

    }

    override fun onResume() {

        super.onResume()

        mapView.onResume()

    }

    override fun onPause() {

        super.onPause()

        mapView.onPause()

    }

    override fun onDestroy() {

        super.onDestroy()

        mapView.onDestroy()

    }

    override fun onLowMemory() {

        super.onLowMemory

       mapView.onLowMemory()

    }

        private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {

            val drawable = ContextCompat.getDrawable(context, drawableId)!!

            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)

            drawable.draw(canvas)

            return bitmap

        }
