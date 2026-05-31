package page.stephens.dailydozen.net

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import page.stephens.dailydozen.domain.model.DataPayload

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val email: String)

@Serializable
data class DataGetResponse(val data: DataPayload? = null, val updatedAt: String? = null)

@Serializable
data class DataPutRequest(val data: DataPayload)

@Serializable
data class DataPutResponse(val success: Boolean = false, val updatedAt: String? = null)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class ResetRequest(val token: String, val password: String)

@Serializable
data class MessageResponse(val message: String? = null, val error: String? = null)

/** Raised on a non-2xx API response, carrying the server's error message. */
class ApiException(val status: Int, message: String) : Exception(message)

/**
 * Thin Ktor client for the Daily Dozen sync API. Talks to the same backend as
 * the web PWA, so accounts and data are shared across web, iOS, and Android.
 */
class ApiClient(
    private val baseUrl: String = "https://dailydozen.stephens.page/api",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    suspend fun register(email: String, password: String): AuthResponse =
        call(HttpMethod.Post, "/register", AuthRequest(email, password)).body()

    suspend fun login(email: String, password: String): AuthResponse =
        call(HttpMethod.Post, "/login", AuthRequest(email, password)).body()

    suspend fun refreshToken(token: String): AuthResponse =
        call(HttpMethod.Post, "/refresh-token", body = null, token = token).body()

    suspend fun getData(token: String): DataGetResponse =
        call(HttpMethod.Get, "/data", body = null, token = token).body()

    suspend fun putData(token: String, payload: DataPayload): DataPutResponse =
        call(HttpMethod.Put, "/data", DataPutRequest(payload), token).body()

    suspend fun forgotPassword(email: String): MessageResponse =
        call(HttpMethod.Post, "/forgot-password", EmailRequest(email)).body()

    suspend fun resetPassword(token: String, password: String): MessageResponse =
        call(HttpMethod.Post, "/reset-password", ResetRequest(token, password)).body()

    private suspend fun call(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        token: String? = null,
    ): HttpResponse {
        val response = client.request("$baseUrl$path") {
            this.method = method
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<MessageResponse>().error }.getOrNull()
            throw ApiException(response.status.value, message ?: "Request failed (${response.status.value})")
        }
        return response
    }
}
