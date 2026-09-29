package com.example.data.backend

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.data.model.CalendarEventEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarBackendService(private val context: Context) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateTimeFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    /**
     * Genereert een Google Maps navigatie/locatie URL
     */
    fun generateMapsUrl(location: String): String {
        val cleanLoc = location.trim()
        return if (cleanLoc.isNotBlank()) {
            "https://www.google.com/maps/search/?api=1&query=${Uri.encode(cleanLoc)}"
        } else {
            "https://www.google.com/maps"
        }
    }

    /**
     * Genereert een Google Route/Directions URL
     */
    fun generateDirectionsUrl(destination: String): String {
        val cleanDest = destination.trim()
        return if (cleanDest.isNotBlank()) {
            "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(cleanDest)}"
        } else {
            "https://www.google.com/maps"
        }
    }

    /**
     * Genereert een Google Search query voor achtergrond research over de afspraak
     */
    fun generateGoogleSearchQuery(title: String, clientName: String, location: String): String {
        val parts = mutableListOf<String>()
        if (title.isNotBlank()) parts.add(title)
        if (clientName.isNotBlank() && clientName != "Niet toegewezen") parts.add(clientName)
        if (location.isNotBlank() && location != "Locatie hoofdkantoor / werkplaats") parts.add(location)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "bedrijfsafspraak werkzaamheden"
    }

    /**
     * Maakt een Intent om de afspraak direct in te schieten in de Google Agenda / Native Agenda van het apparaat
     */
    fun createGoogleCalendarIntent(event: CalendarEventEntity): Intent {
        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append(event.description)
        if (event.workerName.isNotBlank()) {
            descriptionBuilder.append("\n\n👨‍🔧 Toegewezen Werker: ${event.workerName} (${event.workerUsername})")
        }
        if (event.clientName.isNotBlank()) {
            descriptionBuilder.append("\n🏢 Klant / Opdrachtgever: ${event.clientName} (${event.clientUsername})")
        }
        if (event.googleMapsUrl.isNotBlank()) {
            descriptionBuilder.append("\n🗺️ Google Maps Navigatie: ${event.googleMapsUrl}")
        }
        if (event.googleSearchQuery.isNotBlank()) {
            descriptionBuilder.append("\n🔍 Google Search Query: ${event.googleSearchQuery}")
        }
        descriptionBuilder.append("\n\n(Gesynchroniseerd via Admin Centraal Beheer)")

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.DESCRIPTION, descriptionBuilder.toString())
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimestampMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTimestampMillis)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Maakt een Intent voor Google Maps turn-by-turn navigatie naar de locatie van de afspraak
     */
    fun createGoogleMapsRouteIntent(location: String): Intent {
        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(location)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(generateDirectionsUrl(location))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Maakt een Intent voor Google Search onderzoek naar de afspraak/klant/locatie
     */
    fun createGoogleSearchIntent(searchQuery: String): Intent {
        val query = if (searchQuery.isNotBlank()) searchQuery else "afspraak research"
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        return Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Synchroniseert een PlanningTaskEntity automatisch naar een volwaardige CalendarEventEntity
     */
    fun syncPlanningTaskToCalendar(task: PlanningTaskEntity): CalendarEventEntity {
        val startTime = "09:00"
        val durationHours = if (task.estimatedHours > 0) task.estimatedHours else 2.0
        val endHour = (9 + durationHours.toInt()).coerceAtMost(23)
        val endMinutes = ((durationHours - durationHours.toInt()) * 60).toInt()
        val endTime = String.format(Locale.US, "%02d:%02d", endHour, endMinutes)

        val parsedDate = parseDateStringToMillis(task.scheduledDate)
        val startMillis = parsedDate + (9 * 3600000L)
        val endMillis = startMillis + (durationHours * 3600000L).toLong()

        val mapsUrl = generateDirectionsUrl(task.location)
        val searchQuery = generateGoogleSearchQuery(task.title, task.clientName, task.location)

        val color = when (task.priority.lowercase(Locale.ROOT)) {
            "urgent" -> "#EF4444" // Rood
            "hoog" -> "#F59E0B"   // Oranje
            else -> "#38BDF8"     // Hemelsblauw
        }

        return CalendarEventEntity(
            title = "[Werkorder] ${task.title}",
            description = "${task.description}\n\nStatus: ${task.status}\nGeschatte uren: ${task.estimatedHours} uur\nLocatie: ${task.location}",
            eventDate = task.scheduledDate,
            startTime = startTime,
            endTime = endTime,
            startTimestampMillis = startMillis,
            endTimestampMillis = endMillis,
            location = task.location,
            workerUsername = task.assignedWorkerUsername,
            workerName = task.assignedWorkerName,
            clientUsername = task.clientUsername,
            clientName = task.clientName,
            relatedTaskId = task.id,
            relatedRequestId = null,
            googleSearchQuery = searchQuery,
            googleMapsUrl = mapsUrl,
            syncStatus = "SYNCHRONIZED",
            isSyncedWithGoogleCalendar = true,
            calendarColorHex = color,
            priority = task.priority,
            reminderMinutes = 30
        )
    }

    /**
     * Synchroniseert een ServiceRequestEntity automatisch naar een volwaardige CalendarEventEntity
     */
    fun syncServiceRequestToCalendar(request: ServiceRequestEntity): CalendarEventEntity {
        val parsedDate = parseDateStringToMillis(request.preferredDate)
        val startMillis = parsedDate + (13 * 3600000L) // 13:00 uur
        val endMillis = startMillis + (2 * 3600000L)   // 15:00 uur

        val location = "Locatie van Klant: ${request.clientName}"
        val mapsUrl = generateDirectionsUrl(location)
        val searchQuery = generateGoogleSearchQuery(request.title, request.clientName, location)

        return CalendarEventEntity(
            title = "[Service Aanvraag] ${request.title}",
            description = "Aanvraag door ${request.clientName}:\n${request.description}\n\nUrgentie: ${request.urgency}\nStatus: ${request.status}",
            eventDate = request.preferredDate,
            startTime = "13:00",
            endTime = "15:00",
            startTimestampMillis = startMillis,
            endTimestampMillis = endMillis,
            location = location,
            workerUsername = "",
            workerName = "In te plannen werker",
            clientUsername = request.clientUsername,
            clientName = request.clientName,
            relatedTaskId = null,
            relatedRequestId = request.id,
            googleSearchQuery = searchQuery,
            googleMapsUrl = mapsUrl,
            syncStatus = "SYNCHRONIZED",
            isSyncedWithGoogleCalendar = true,
            calendarColorHex = "#A855F7", // Paars voor service aanvragen
            priority = request.urgency,
            reminderMinutes = 60
        )
    }

    /**
     * Exporteert de lijst met CalendarEvents naar een universeel iCalendar (.ics) formaat
     * Dit kan direct worden geïmporteerd in Google Calendar, Outlook, Apple Calendar etc.
     */
    fun exportToIcs(events: List<CalendarEventEntity>): String {
        val icsDateFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//RoleVault//Admin Calendar Backend 3.0//NL\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:RoleVault Admin Centraal\r\n")
        sb.append("X-WR-TIMEZONE:Europe/Amsterdam\r\n")

        for (event in events) {
            val dtStamp = icsDateFormatter.format(Date(event.createdAt))
            val dtStart = icsDateFormatter.format(Date(event.startTimestampMillis))
            val dtEnd = icsDateFormatter.format(Date(event.endTimestampMillis))
            val uid = "rolevault-${event.id}-${event.startTimestampMillis}@rolevault.internal"

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:$uid\r\n")
            sb.append("DTSTAMP:$dtStamp\r\n")
            sb.append("DTSTART:$dtStart\r\n")
            sb.append("DTEND:$dtEnd\r\n")
            sb.append("SUMMARY:${escapeIcsText(event.title)}\r\n")
            sb.append("DESCRIPTION:${escapeIcsText(event.description)}\r\n")
            if (event.location.isNotBlank()) {
                sb.append("LOCATION:${escapeIcsText(event.location)}\r\n")
            }
            sb.append("STATUS:CONFIRMED\r\n")
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    private fun escapeIcsText(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun parseDateStringToMillis(dateStr: String): Long {
        return try {
            if (dateStr.contains("-")) {
                val parts = dateStr.split("-")
                if (parts[0].length == 4) {
                    dateFormatter.parse(dateStr)?.time ?: System.currentTimeMillis()
                } else {
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr)?.time
                        ?: System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
