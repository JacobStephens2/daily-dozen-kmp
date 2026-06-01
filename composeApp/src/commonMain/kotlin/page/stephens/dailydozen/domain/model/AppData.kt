package page.stephens.dailydozen.domain.model

import kotlinx.serialization.Serializable

/**
 * The cross-device sync payload — also exactly the locally-persisted profile
 * data. Shape is byte-compatible with the web app's `PUT /api/data` `data`
 * field, so iOS, Android, web, and the existing PWA all share one account.
 */
@Serializable
data class DataPayload(
    val profiles: Map<String, ProfileData> = emptyMap(),
)

/**
 * One profile's data. [data] maps a JS `toDateString()` date key
 * ("Fri Jan 17 2025") to a map of categoryId -> the checked serving indices.
 * [customServings] is null when the profile uses an unmodified [dietType] preset.
 */
@Serializable
data class ProfileData(
    val name: String,
    val color: String,
    val dietType: String = "standard",
    val customServings: Map<String, Int>? = null,
    val data: Map<String, Map<String, List<Int>>> = emptyMap(),
)

/**
 * The complete locally-persisted state: the syncable [payload] plus local-only
 * bits the server never sees (selected profile, last-sync time, cached email,
 * and which day-celebrations have already been shown).
 */
@Serializable
data class LocalState(
    val payload: DataPayload = DataPayload(defaultProfiles()),
    val currentProfile: String = "user",
    val lastSync: String? = null,
    val email: String? = null,
    val celebrations: Set<String> = emptySet(),
)

/** The JSON shape produced by "Export Data" (and accepted by "Import Data"). */
@Serializable
data class ExportEnvelope(
    val exportDate: String,
    val profiles: Map<String, ProfileData> = emptyMap(),
)

/** The two starter profiles the web app seeds when none exist. */
fun defaultProfiles(): Map<String, ProfileData> = linkedMapOf(
    "user" to ProfileData(name = "You", color = "#38672a"),
    "other" to ProfileData(name = "Other", color = "#7c5724"),
)
