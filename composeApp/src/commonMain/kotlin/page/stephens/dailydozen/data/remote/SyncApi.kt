package page.stephens.dailydozen.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import page.stephens.dailydozen.data.auth.TokenStore
import page.stephens.dailydozen.data.model.DailyDozenJson

/** Canonical API base (SYNC_CONTRACT.md §1). */
const val SYNC_BASE_URL: String = "https://dailydozen.stephens.page/api"

/** PUT body cap (§2). We refuse oversized payloads client-side rather than lose an edit on a 413. */
private const val MAX_PAYLOAD_BYTES = 2 * 1024 * 1024

@Serializable
private data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val email: String)

@Serializable
data class DataEnvelope(val data: JsonElement? = null, val updatedAt: String? = null)

@Serializable
private data class PutDataRequest(val data: JsonElement)

@Serializable
private data class PutDataResponse(val success: Boolean = false, val updatedAt: String? = null)

@Serializable
private data class ErrorResponse(val error: String? = null)

@Serializable
private data class EmailRequest(val email: String)

@Serializable
private data class ResetRequest(val token: String, val password: String)

/** A server error carrying the `{error}` message (§2); surface it, never retry-hammer. */
class ApiException(val status: Int, val serverMessage: String?) :
    Exception(serverMessage ?: "HTTP $status")

/**
 * Auth is unrecoverable for now (refresh already failed). The caller MUST treat
 * this as "needs re-auth" and PRESERVE all local data — never wipe (ADR-2 / D1).
 */
class NeedsReauthException(message: String?) : Exception(message)

/** Payload exceeds the 2 MB server cap; the local edit is kept, the user is told (D9). */
class PayloadTooLargeException : Exception("Sync payload exceeds the 2 MB limit")

/**
 * Thin Ktor client for the existing JWT backend (ADR-1). The Bearer auth plugin
 * refreshes the token on a 401 and replays the request automatically; if refresh
 * itself fails, the request returns 401 and we raise [NeedsReauthException] —
 * we NEVER clear the token or local data here (ADR-2). Only explicit logout
 * clears, via [TokenStore.clear].
 */
class SyncApi(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val baseUrl: String = SYNC_BASE_URL,
) {
    suspend fun register(email: String, password: String): AuthResponse {
        val resp = client.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }
        if (!resp.status.isSuccess()) throw resp.toError()
        return resp.body<AuthResponse>().also { tokenStore.write(it.token) }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val resp = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }
        if (!resp.status.isSuccess()) throw resp.toError()
        return resp.body<AuthResponse>().also { tokenStore.write(it.token) }
    }

    suspend fun forgotPassword(email: String) {
        val resp = client.post("$baseUrl/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(EmailRequest(email))
        }
        if (!resp.status.isSuccess()) throw resp.toError()
    }

    suspend fun resetPassword(token: String, password: String) {
        val resp = client.post("$baseUrl/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetRequest(token, password))
        }
        if (!resp.status.isSuccess()) throw resp.toError()
    }

    /** GET the server blob (§2). `data` may be null (no server data yet). */
    suspend fun getData(): DataEnvelope {
        val resp = client.get("$baseUrl/data")
        if (!resp.status.isSuccess()) throw resp.toError()
        return resp.body()
    }

    /** PUT the whole blob (§2); returns the server's new `updatedAt`. */
    suspend fun putData(payload: JsonElement): String {
        val body = DailyDozenJson.encodeToString(JsonElement.serializer(), payload)
        if (body.length > MAX_PAYLOAD_BYTES) throw PayloadTooLargeException()
        val resp = client.put("$baseUrl/data") {
            contentType(ContentType.Application.Json)
            setBody(PutDataRequest(payload))
        }
        if (!resp.status.isSuccess()) throw resp.toError()
        return resp.body<PutDataResponse>().updatedAt
            ?: throw ApiException(resp.status.value, "PUT /data returned no updatedAt")
    }

    private suspend fun HttpResponse.toError(): Exception {
        val message = runCatching { body<ErrorResponse>().error }.getOrNull()
        return if (status.value == 401) NeedsReauthException(message) else ApiException(status.value, message)
    }
}

/**
 * Builds the sync [HttpClient]. The Bearer plugin loads the stored token, sends
 * it only on `/data`, and on 401 calls `/refresh-token` once and replays. A
 * failed refresh returns null so the request surfaces 401 (-> NeedsReauth) —
 * crucially WITHOUT touching the token or local data.
 */
fun createSyncHttpClient(tokenStore: TokenStore, baseUrl: String = SYNC_BASE_URL): HttpClient =
    HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(DailyDozenJson) }
        install(Auth) {
            bearer {
                loadTokens { tokenStore.read()?.let { BearerTokens(it, "") } }
                refreshTokens {
                    val current = tokenStore.read() ?: return@refreshTokens null
                    val resp = client.post("$baseUrl/refresh-token") {
                        header(HttpHeaders.Authorization, "Bearer $current")
                    }
                    if (resp.status.isSuccess()) {
                        resp.body<AuthResponse>().token
                            .also { tokenStore.write(it) }
                            .let { BearerTokens(it, "") }
                    } else {
                        null // do NOT clear; surfaces as 401 -> NeedsReauth
                    }
                }
                sendWithoutRequest { request -> request.url.pathSegments.lastOrNull() == "data" }
            }
        }
    }
