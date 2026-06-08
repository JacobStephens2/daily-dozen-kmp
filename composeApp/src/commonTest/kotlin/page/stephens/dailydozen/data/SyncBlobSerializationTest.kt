package page.stephens.dailydozen.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import page.stephens.dailydozen.data.model.DailyDozenJson
import page.stephens.dailydozen.data.model.SyncBlob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYNC_CONTRACT.md §3/§9.3 — the blob must round-trip losslessly, including
 * fields this client doesn't model (forward-compat / failure mode D7).
 */
class SyncBlobSerializationTest {

    private val json = DailyDozenJson

    // A realistic §3 blob with: a populated "user" profile, an "other" profile
    // with customServings=null, an UNKNOWN profile field ("avatarUrl"), and an
    // UNKNOWN top-level key ("schemaVersion").
    private val webBlob = """
        {
          "profiles": {
            "user": {
              "name": "You",
              "color": "#38672a",
              "dietType": "standard",
              "customServings": null,
              "data": {
                "Thu Jan 16 2025": { "beans": [0, 2], "greens": [0] },
                "Fri Jan 17 2025": { "beans": [0, 1, 2] }
              },
              "avatarUrl": "https://example.com/a.png"
            },
            "other": {
              "name": "Other",
              "color": "#7c5724",
              "dietType": "standard",
              "customServings": null,
              "data": {}
            }
          },
          "schemaVersion": 7
        }
    """.trimIndent()

    private fun parse(s: String) = json.parseToJsonElement(s).jsonObject

    @Test
    fun roundTripDropsNoFields() {
        val blob = json.decodeFromString(SyncBlob.serializer(), webBlob)
        val out = json.encodeToString(SyncBlob.serializer(), blob)
        val reparsed = parse(out)

        // Unknown top-level key preserved.
        assertEquals(7, reparsed["schemaVersion"]!!.jsonPrimitive.int)
        // Unknown profile field preserved.
        val user = reparsed["profiles"]!!.jsonObject["user"]!!.jsonObject
        assertEquals(
            "https://example.com/a.png",
            user["avatarUrl"]!!.jsonPrimitive.content,
        )
        // customServings null preserved (not dropped, not defaulted to a map).
        val other = reparsed["profiles"]!!.jsonObject["other"]!!.jsonObject
        assertTrue(other.containsKey("customServings"))
        assertNull(other["customServings"]!!.jsonPrimitive.contentOrNullSafe())
    }

    @Test
    fun indexSetsArePreserved() {
        val blob = json.decodeFromString(SyncBlob.serializer(), webBlob)
        val user = blob.profiles.getValue("user")
        assertEquals(listOf(0, 2), user.data.getValue("Thu Jan 16 2025").getValue("beans"))
        assertEquals(listOf(0, 1, 2), user.data.getValue("Fri Jan 17 2025").getValue("beans"))
    }

    @Test
    fun outputIsCanonicalSortedDeduped() {
        // Build a blob from messy input; serialized arrays must be sorted + de-duped.
        val messy = """
            {"profiles":{"user":{"data":{"Thu Jan 16 2025":{"beans":[2,0,2,1]}}}}}
        """.trimIndent()
        val blob = json.decodeFromString(SyncBlob.serializer(), messy)
        val out = parse(json.encodeToString(SyncBlob.serializer(), blob))
        val arr = out["profiles"]!!.jsonObject["user"]!!.jsonObject["data"]!!
            .jsonObject["Thu Jan 16 2025"]!!.jsonObject["beans"]!!.jsonArray
            .map { it.jsonPrimitive.int }
        assertEquals(listOf(0, 1, 2), arr)
    }

    @Test
    fun emptyBlobRoundTrips() {
        val out = json.encodeToString(SyncBlob.serializer(), SyncBlob.empty())
        val blob = json.decodeFromString(SyncBlob.serializer(), out)
        assertTrue(blob.profiles.isEmpty())
    }
}

// Helper: JsonNull's content is the literal "null" string in kotlinx; treat it as absent.
private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
    if (this is kotlinx.serialization.json.JsonNull) null else content
