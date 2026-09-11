package app.mininote.mininote.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.mininote.mininote.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Портировано из mini-note-design/android/notes-data.js (relTime). */
@Composable
fun formatRelativeTime(isoInstant: String): String {
    val instant = runCatching { Instant.parse(isoInstant) }.getOrNull() ?: return isoInstant
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(instant, now)
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())
        minutes < 60 * 24 -> {
            val hours = (minutes / 60).toInt()
            pluralStringResource(R.plurals.time_hours_ago, hours, hours)
        }
        minutes < 60 * 24 * 14 -> {
            val days = (minutes / (60 * 24)).toInt()
            pluralStringResource(R.plurals.time_days_ago, days, days)
        }
        else -> DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault()).format(instant)
    }
}
