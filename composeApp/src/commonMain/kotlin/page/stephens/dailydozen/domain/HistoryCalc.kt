package page.stephens.dailydozen.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import page.stephens.dailydozen.domain.model.ProfileData

/** One calendar cell in the history view. */
data class HistoryCell(
    val day: Int,
    val date: LocalDate,
    val percent: Int,
    val hasData: Boolean,
    val isFuture: Boolean,
    val isToday: Boolean,
)

data class HistoryMonth(
    val year: Int,
    val month: Int,                 // 1-12
    val label: String,
    val leadingBlanks: Int,         // empty cells before day 1 (Sun-based)
    val cells: List<HistoryCell>,
    val streak: Int,
    val perfectDays: Int,
    val daysTracked: Int,
    val isCurrentMonth: Boolean,
)

/**
 * Port of the web app's `js/history.js` calendar + streak math, operating on a
 * profile's data map. Completion is total checked servings vs. the active total.
 */
object HistoryCalc {

    private fun dayCompleted(day: Map<String, List<Int>>?, active: List<String>): Int {
        if (day == null) return 0
        return active.sumOf { day[it]?.size ?: 0 }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val firstNext = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        return firstNext.minus(1, DateTimeUnit.DAY).dayOfMonth
    }

    /** JS getDay() (Sun=0..Sat=6) for the 1st of the month. */
    private fun firstWeekday(year: Int, month: Int): Int =
        (LocalDate(year, month, 1).dayOfWeek.ordinal + 1) % 7

    fun build(profile: ProfileData, year: Int, month: Int, today: LocalDate): HistoryMonth {
        val servings = Categories.servingsFor(profile.dietType, profile.customServings)
        val active = Categories.activeCategories(servings)
        val activeIds = active.map { it.category.id }
        val total = active.sumOf { it.servings }
        val data = profile.data

        var daysTracked = 0
        var perfectDays = 0
        val count = daysInMonth(year, month)
        val cells = ArrayList<HistoryCell>(count)
        for (day in 1..count) {
            val date = LocalDate(year, month, day)
            val key = DateKeys.toDateString(date)
            val dayData = data[key]
            val isFuture = date > today
            val isToday = date == today
            var percent = 0
            if (dayData != null) {
                val completed = dayCompleted(dayData, activeIds)
                daysTracked++
                if (total > 0 && completed >= total) perfectDays++
                percent = if (total > 0) (completed * 100) / total else 0
            }
            cells.add(
                HistoryCell(
                    day = day,
                    date = date,
                    percent = percent,
                    hasData = dayData != null && !isFuture,
                    isFuture = isFuture,
                    isToday = isToday,
                ),
            )
        }

        return HistoryMonth(
            year = year,
            month = month,
            label = DateKeys.monthLabel(year, month),
            leadingBlanks = firstWeekday(year, month),
            cells = cells,
            streak = streak(data, activeIds, total, today),
            perfectDays = perfectDays,
            daysTracked = daysTracked,
            isCurrentMonth = year == today.year && month == today.monthNumber,
        )
    }

    /** Global streak: today (if complete) plus consecutive complete days before it. */
    private fun streak(
        data: Map<String, Map<String, List<Int>>>,
        activeIds: List<String>,
        total: Int,
        today: LocalDate,
    ): Int {
        if (total <= 0) return 0
        var streak = 0
        if (dayCompleted(data[DateKeys.toDateString(today)], activeIds) >= total) streak++
        var d = today.minus(1, DateTimeUnit.DAY)
        while (true) {
            val dayData = data[DateKeys.toDateString(d)] ?: break
            if (dayCompleted(dayData, activeIds) >= total) {
                streak++
                d = d.minus(1, DateTimeUnit.DAY)
            } else break
        }
        return streak
    }
}
