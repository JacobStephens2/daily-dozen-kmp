package page.stephens.dailydozen.data

import kotlinx.datetime.LocalDate
import page.stephens.dailydozen.data.model.Profile
import page.stephens.dailydozen.data.model.SyncBlob
import page.stephens.dailydozen.domain.toJsDateString

/** A row from the legacy count-based `servingLog` table. */
data class LegacyServing(val day: String, val categoryId: String, val count: Int)

/** Old underscored/abbreviated ids -> §4 hyphenated ids. */
private val LEGACY_ID_MAP = mapOf(
    "other_fruits" to "other-fruits",
    "other_veg" to "other-vegetables",
    "flaxseeds" to "flaxseed",
    "nuts_seeds" to "nuts-seeds",
    "herbs_spices" to "herbs-spices",
    "whole_grains" to "whole-grains",
)

/**
 * Pure, information-preserving migration of the legacy count schema to the
 * contract blob (ADR-4). `count = N` becomes indices `[0..N-1]` (N boxes
 * checked — the only thing a count can mean); old ISO date keys become JS
 * dateKeys (§6); old ids become §4 hyphenated ids; everything lands under the
 * default `"user"` profile with `dietType = "standard"`. Deterministic and
 * idempotent. Kept pure (no DB) so it is unit-testable in isolation.
 */
fun migrateCountsToBlob(rows: List<LegacyServing>, profileId: String = "user"): SyncBlob {
    val data = LinkedHashMap<String, MutableMap<String, List<Int>>>()
    for (row in rows) {
        if (row.count <= 0) continue
        val dateKey = runCatching { LocalDate.parse(row.day).toJsDateString() }.getOrElse { row.day }
        val catId = LEGACY_ID_MAP[row.categoryId] ?: row.categoryId
        data.getOrPut(dateKey) { LinkedHashMap() }[catId] = (0 until row.count).toList()
    }
    if (data.isEmpty()) return SyncBlob.empty()
    return SyncBlob(profiles = mapOf(profileId to Profile(dietType = "standard", data = data)))
}
