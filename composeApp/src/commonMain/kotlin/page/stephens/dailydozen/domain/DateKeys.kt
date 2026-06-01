package page.stephens.dailydozen.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Date keys must be byte-identical to JavaScript's `Date.prototype.toDateString()`
 * — e.g. "Fri Jan 17 2025" — because they are the map keys in the sync payload
 * shared with the web app. Format: `EEE MMM dd yyyy`, English, zero-padded day,
 * local time zone. (Hand-rolled rather than via a locale-aware formatter so the
 * output never drifts with the device locale.)
 */
object DateKeys {

    private val WEEKDAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    private val WEEKDAYS_LONG = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
    )
    private val MONTHS_LONG = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    /** JS getDay(): Sun=0..Sat=6. kotlinx DayOfWeek.ordinal is Mon=0..Sun=6. */
    private fun jsDayOfWeek(day: DayOfWeek): Int = (day.ordinal + 1) % 7

    fun toDateString(date: LocalDate): String {
        val dow = WEEKDAYS[jsDayOfWeek(date.dayOfWeek)]
        val mon = MONTHS[date.monthNumber - 1]
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "$dow $mon $day ${date.year}"
    }

    fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        Clock.System.todayIn(timeZone)

    fun todayKey(): String = toDateString(today())

    /** Header label, matching the web app: e.g. "Friday, January 17, 2025". */
    fun longLabel(date: LocalDate): String {
        val dow = WEEKDAYS_LONG[jsDayOfWeek(date.dayOfWeek)]
        val mon = MONTHS_LONG[date.monthNumber - 1]
        return "$dow, $mon ${date.dayOfMonth}, ${date.year}"
    }

    /** Short month-year label for the history header: e.g. "January 2025". */
    fun monthLabel(year: Int, monthNumber: Int): String =
        "${MONTHS_LONG[monthNumber - 1]} $year"
}
