package page.stephens.dailydozen.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SYNC_CONTRACT.md §6 / §9.1 — the dateKey must match JS `toDateString()`
 * exactly, including zero-padded day and the leap-day case. Any drift here
 * lands edits on the wrong day vs the web app (failure mode D6).
 */
class DateKeysTest {

    @Test
    fun matchesAllContractVectors() {
        val vectors = listOf(
            LocalDate(2025, 1, 16) to "Thu Jan 16 2025",
            LocalDate(2025, 1, 1) to "Wed Jan 01 2025",
            LocalDate(2025, 12, 25) to "Thu Dec 25 2025",
            LocalDate(2024, 2, 29) to "Thu Feb 29 2024", // leap day
            LocalDate(2025, 7, 4) to "Fri Jul 04 2025",
            LocalDate(2025, 9, 9) to "Tue Sep 09 2025",
            LocalDate(2026, 5, 31) to "Sun May 31 2026",
            LocalDate(2025, 3, 2) to "Sun Mar 02 2025",
        )
        for ((date, expected) in vectors) {
            assertEquals(expected, date.toJsDateString(), "dateKey for $date")
        }
    }

    @Test
    fun dayIsZeroPadded() {
        assertEquals("Wed Jan 01 2025", LocalDate(2025, 1, 1).toJsDateString())
    }
}
