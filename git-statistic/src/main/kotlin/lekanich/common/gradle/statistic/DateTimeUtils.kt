package lekanich.common.gradle.statistic

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for formatting dates and times.
 */
object DateTimeUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Format an Instant to a human-readable date-time string.
     */
    fun formatDateTime(instant: Instant): String {
        return formatter.format(instant)
    }
}
