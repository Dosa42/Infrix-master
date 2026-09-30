package com.example.data.backend

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.util.Locale

data class DevicePermissionStatus(
    val permission: String,
    val title: String,
    val isGranted: Boolean,
    val description: String
)

data class GeoLocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val provider: String = "GPS",
    val readableAddress: String? = null
)

class DeviceCapabilitiesManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Inspects current status of all requested Android permissions
     */
    fun checkAllPermissions(): List<DevicePermissionStatus> {
        val permissions = listOf(
            Triple(Manifest.permission.ACCESS_FINE_LOCATION, "Precieze Locatie (GPS)", "Noodzakelijk voor navigatie, werklocaties en Google Maps routebepaling."),
            Triple(Manifest.permission.ACCESS_COARSE_LOCATION, "Globale Locatie (Netwerk)", "Biedt snelle benadering van werkregio en netwerklocatie."),
            Triple(Manifest.permission.CAMERA, "Camera", "Voor het maken van foto's van voltooide werkzaamheden, schade of barcodes."),
            Triple(Manifest.permission.RECORD_AUDIO, "Microfoon & Audio", "Voor spraakmemo's, audioverslagen van monteurs en handsfree notities."),
            Triple(Manifest.permission.POST_NOTIFICATIONS, "Meldingen", "Voor realtime alerts over taakwijzigingen, goedkeuringen en beveiliging."),
            Triple(Manifest.permission.CALL_PHONE, "Telefoon & Bellen", "Voor direct 1-klik bellen van klanten of monteurs vanuit het dossier."),
            Triple(Manifest.permission.READ_PHONE_STATE, "Telefoonstatus", "Voor netwerkstatus en telemetrie van buitendienstapparaten.")
        )

        return permissions.map { (perm, name, desc) ->
            val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            DevicePermissionStatus(
                permission = perm,
                title = name,
                isGranted = granted,
                description = desc
            )
        }
    }

    /**
     * Fetches current location coordinates from the device LocationManager
     */
    fun getCurrentLocation(): GeoLocationData? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        try {
            val gpsLoc: Location? = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc: Location? = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLoc = gpsLoc ?: netLoc

            if (bestLoc != null) {
                val address = resolveAddress(bestLoc.latitude, bestLoc.longitude)
                return GeoLocationData(
                    latitude = bestLoc.latitude,
                    longitude = bestLoc.longitude,
                    accuracy = bestLoc.accuracy,
                    provider = bestLoc.provider ?: "Network",
                    readableAddress = address
                )
            }
        } catch (_: SecurityException) {
            return null
        } catch (_: Exception) {
            return null
        }

        // Geen fallback coördinaten: als het apparaat nog geen GPS fix heeft, retourneer null
        return null
    }

    private fun resolveAddress(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(lat, lng, 1)
            if (!results.isNullOrEmpty()) {
                val item = results[0]
                "${item.thoroughfare ?: ""} ${item.subThoroughfare ?: ""}, ${item.locality ?: ""}".trim().trimStart(',')
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Launches native phone dialer with phone number
     */
    fun dialPhoneNumber(phoneNumber: String) {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        if (cleanNumber.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
