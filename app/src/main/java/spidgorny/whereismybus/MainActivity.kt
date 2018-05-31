package spidgorny.whereismybus

import android.Manifest
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import android.os.StrictMode
import android.util.Log
import im.delight.android.location.SimpleLocation
import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import kotlinx.android.synthetic.main.content_main.*
import android.content.pm.PackageManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private val klass = "MainActivity"

    private var location: SimpleLocation? = null

    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

	private var enabled = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

		this.onActivityCreated()
    }

	/**
	 * Does not override anything
	 */
	fun onActivityCreated() {
		this.initUI()
		this.initFAB()
	}

	private fun checkPermissions(): Boolean {
		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
				PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
				PackageManager.PERMISSION_GRANTED) {
			Log.d(this.klass, "Permissions OK")
			return true
		}
		return false
	}

	private fun initPermissions() {
		Log.d(this.klass, "Permissions Request")
		ActivityCompat.requestPermissions(this, arrayOf(
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION),
				MY_PERMISSIONS_REQUEST_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    this.enableSendingData()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun initUI() {
        // ec124bcd7a25bc02
        Log.d(this.klass, this.getDeviceID())
        this.deviceID.text = this.getDeviceID()
    }

    private fun getDeviceID(): String? {
        return Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID);
    }

    private fun initFAB() {
		fab.setOnClickListener { view ->
			Log.d(this.klass, "FAB click")
//            val latitude = location!!.latitude
//            val longitude = location!!.longitude
//            val speed = location!!.speed
			if (this.enabled) {
				this.enabled = false
			} else {
				if (!this.checkPermissions()) {
					this.initPermissions()
				} else {
					this.enableSendingData()
				}
			}
		}
	}

	protected fun enableSendingData() {
		if (this.initLocation()) {
			this.enabled = true
			this.updateLocation()
		}
	}

	private fun initLocation(): Boolean {
//        val context = getActivity(this).findViewById(android.R.id.content)
		val context = this.layout1

		val lsEnabled = SmartLocation.with(this.applicationContext).location().state().locationServicesEnabled()
		if (!lsEnabled) {
			Snackbar.make(context, "LocationService not enabled", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show()
		} else {
			Snackbar.make(context, "LocationService OK", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show()
		}

		val gpsEnabled = SmartLocation.with(this.applicationContext).location().state().isGpsAvailable
		if (!gpsEnabled) {
			Snackbar.make(context, "GPS not enabled", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show()
		} else {
			Snackbar.make(context, "GPS OK", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show()
		}
		return lsEnabled && gpsEnabled;
	}

	/**
	 * @deprecated
	 */
	private fun initLocationDeprecated() {
		Log.d(this.klass, "location is set")
		this.location = SimpleLocation(this, true, false, 5 * 1000, true)
		// if we can't access the location yet
		if (!this.location!!.hasLocationEnabled()) {
			// ask the user to enable location access
			SimpleLocation.openSettings(this)
		}

		location!!.setListener({
			fun onPositionChanged() {
				val latitude = location!!.latitude
				val longitude = location!!.longitude
				val speed = location!!.speed
				val location = latitude.toString() + "," + longitude.toString() + " speed: " + speed.toString()
				Log.d(this.klass + " onPosChg", location)
			}
		})
	}

	protected fun updateLocation() {
		SmartLocation.with(this.applicationContext).location()
				.oneFix()
				.start(OnLocationUpdatedListener() {
					val latitude = it.latitude
					val longitude = it.longitude
					val speed = it.speed

					val location = latitude.toString() + "," + longitude.toString() + " speed: " + speed.toString()
					Log.d(this.klass, location)
					Snackbar.make(this.layout1, location, Snackbar.LENGTH_LONG)
							.setAction("Action", null).show()

					val response = this.pushLocation(latitude, longitude, speed)
					Snackbar.make(this.layout1, response.toString(), Snackbar.LENGTH_LONG)
							.setAction("Action", null).show()

				})
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun pushLocation(lat: Double, lon: Double, speed: Float): String? {
        val client = OkHttpClient()

        val url = "http://192.168.1.6/whereismybus/api.php?lat="+lat.toString()+"&lon="+lon.toString()+"&speed="+speed.toString()
        val request = Request.Builder()
                .url(url)
                .build()

        try {
            val response = client.newCall(request).execute()
            return response.body()?.string()
        } catch (e: IOException) {
            return e.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val context = this.applicationContext
        SmartLocation.with(context).location().stop()
    }

    override fun onResume() {
        super.onResume()

        // make the device update its location
        if (this.location != null) {
            this.location!!.beginUpdates()
        }

        // ...
    }

    override fun onPause() {
        // stop location updates (saves battery)
        if (this.location != null) {
            this.location!!.endUpdates()
        }

        // ...

        super.onPause()
    }
}
