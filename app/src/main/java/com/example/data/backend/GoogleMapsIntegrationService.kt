package com.example.data.backend

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteCalculationResult(
    val origin: String,
    val destination: String,
    val directDistanceKm: Double,
    val estimatedDriveMinutes: Int,
    val googleMapsUrl: String
)

class GoogleMapsIntegrationService(private val context: Context) {

    /**
     * Opens Google Maps Navigation directly to the specified address or coordinates
     */
    fun openNavigation(destinationAddressOrCoords: String): Boolean {
        return try {
            val encodedDest = Uri.encode(destinationAddressOrCoords.trim())
            // First try Google Navigation Intent
            val navUri = Uri.parse("google.navigation:q=$encodedDest")
            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                true
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shows a location on Google Maps
     */
    fun showLocationOnMap(queryOrAddress: String): Boolean {
        return try {
            val encoded = Uri.encode(queryOrAddress.trim())
            val geoUri = Uri.parse("geo:0,0?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculates direct distance between two GPS coordinates using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): RouteCalculationResult {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = Math.round((r * c) * 100.0) / 100.0

        // Estimate driving time roughly: assuming average 60 km/h + 5 min overhead
        val driveMinutes = ((distanceKm / 60.0) * 60 + 5).toInt().coerceAtLeast(2)
        val mapsUrl = "https://www.google.com/maps/dir/?api=1&origin=$lat1,$lon1&destination=$lat2,$lon2"

        return RouteCalculationResult(
            origin = "$lat1, $lon1",
            destination = "$lat2, $lon2",
            directDistanceKm = distanceKm,
            estimatedDriveMinutes = driveMinutes,
            googleMapsUrl = mapsUrl
        )
    }
}
