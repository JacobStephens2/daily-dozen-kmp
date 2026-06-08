package page.stephens.dailydozen.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The synced payload, modeling SYNC_CONTRACT.md §3 exactly.
 *
 * Forward-compat is a hard requirement (§3/§8): any field the web app adds that
 * this client doesn't know about MUST round-trip untouched, or a KMP re-PUT
 * would silently drop the web's data (failure mode D7). Both this type and
 * [Profile] therefore carry an [unknown] bag of every JSON key they don't model,
 * and re-emit it verbatim on serialize.
 */
@Serializable(with = SyncBlobSerializer::class)
data class SyncBlob(
    val profiles: Map<String, Profile> = emptyMap(),
    val unknown: Map<String, JsonElement> = emptyMap(),
) {
    companion object {
        fun empty(): SyncBlob = SyncBlob()
    }
}

/**
 * One profile (§3). `data[dateKey][categoryId]` is an array of *checked serving
 * indices* (a set), not a count — this is what makes the union merge lossless
 * (§1). [customServings] is nullable: null means "derive targets from
 * [dietType] preset" (§5).
 */
@Serializable(with = ProfileSerializer::class)
data class Profile(
    val name: String = "You",
    val color: String = "#38672a",
    val dietType: String = "standard",
    val customServings: Map<String, Int>? = null,
    val data: Map<String, Map<String, List<Int>>> = emptyMap(),
    val unknown: Map<String, JsonElement> = emptyMap(),
)

/** The single Json instance for the blob. Custom serializers control output shape. */
val DailyDozenJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private object ProfileSerializer : KSerializer<Profile> {
    private val known = setOf("name", "color", "dietType", "customServings", "data")
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): Profile {
        val obj = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val customServings = obj["customServings"]?.let { el ->
            if (el is JsonNull) null else el.jsonObject.mapValues { it.value.jsonPrimitive.int }
        }
        val data = obj["data"]?.jsonObject?.mapValues { (_, dayEl) ->
            dayEl.jsonObject.mapValues { (_, arrEl) -> arrEl.jsonArray.map { it.jsonPrimitive.int } }
        } ?: emptyMap()
        return Profile(
            name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "You",
            color = obj["color"]?.jsonPrimitive?.contentOrNull ?: "#38672a",
            dietType = obj["dietType"]?.jsonPrimitive?.contentOrNull ?: "standard",
            customServings = customServings,
            data = data,
            unknown = obj.filterKeys { it !in known },
        )
    }

    override fun serialize(encoder: Encoder, value: Profile) {
        val obj = buildJsonObject {
            put("name", value.name)
            put("color", value.color)
            put("dietType", value.dietType)
            if (value.customServings == null) {
                put("customServings", JsonNull)
            } else {
                putJsonObject("customServings") { value.customServings.forEach { (k, v) -> put(k, v) } }
            }
            putJsonObject("data") {
                value.data.forEach { (day, cats) ->
                    putJsonObject(day) {
                        cats.forEach { (cat, indices) ->
                            // Canonical output: sorted + de-duped index set (§1.2).
                            putJsonArray(cat) { indices.distinct().sorted().forEach { add(it) } }
                        }
                    }
                }
            }
            value.unknown.forEach { (k, v) -> put(k, v) }
        }
        (encoder as JsonEncoder).encodeJsonElement(obj)
    }
}

private object SyncBlobSerializer : KSerializer<SyncBlob> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): SyncBlob {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val profiles = obj["profiles"]?.jsonObject?.mapValues { (_, v) ->
            jsonDecoder.json.decodeFromJsonElement(ProfileSerializer, v)
        } ?: emptyMap()
        return SyncBlob(profiles = profiles, unknown = obj.filterKeys { it != "profiles" })
    }

    override fun serialize(encoder: Encoder, value: SyncBlob) {
        val jsonEncoder = encoder as JsonEncoder
        val obj = buildJsonObject {
            putJsonObject("profiles") {
                value.profiles.forEach { (id, p) ->
                    put(id, jsonEncoder.json.encodeToJsonElement(ProfileSerializer, p))
                }
            }
            value.unknown.forEach { (k, v) -> put(k, v) }
        }
        jsonEncoder.encodeJsonElement(obj)
    }
}
