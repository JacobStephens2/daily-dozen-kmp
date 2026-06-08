package page.stephens.dailydozen.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

// Always English, regardless of device locale (matches JS Date semantics).
private val JS_WEEKDAYS = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") // isoDayNumber 1..7
private val JS_MONTHS =
    arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * The blob's dateKey, matching JavaScript `Date.prototype.toDateString()`:
 * `"EEE MMM dd yyyy"` — 3-letter English weekday + month, zero-padded day,
 * 4-digit year, single spaces (SYNC_CONTRACT.md §6).
 */
fun LocalDate.toJsDateString(): String {
    val wd = JS_WEEKDAYS[dayOfWeek.isoDayNumber - 1]
    val mo = JS_MONTHS[monthNumber - 1]
    val dd = dayOfMonth.toString().padStart(2, '0')
    return "$wd $mo $dd $year"
}

/**
 * "Today" in the device's LOCAL timezone — mirrors the web's `new Date()`.
 *
 * Deliberately NOT normalized to UTC: §6 documents that two devices in
 * different zones near midnight may use different keys, and "fixing" that with
 * UTC would break web compatibility. Do not change this without re-reading §6.
 */
fun todayKey(
    clock: Clock = Clock.System,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): String = clock.now().toLocalDateTime(tz).date.toJsDateString()
