package io.github.furka.pigeon.http

import io.github.furka.pigeon.config.*
import io.github.furka.pigeon.types.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class GetMessagesQuery(
    val limit: Int? = null,
    val beforeId: Int? = null,
    val afterId: Int? = null
)

class HttpClient(private val config: ClientConfig) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(DefaultRequest) {
            header(HttpHeaders.Authorization, "Bot ${config.token}")
        }
    }

    suspend fun close() {
        client.close()
    }

    private suspend inline fun <reified R> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        params: Map<String, Any?> = emptyMap()
    ): R {
        return withContext(Dispatchers.IO) {
            val url = resolveApiUrl(config, path)
            val filteredParams = params.filterValues { it != null }

            client.request(url) {
                this.method = method
                if (filteredParams.isNotEmpty()) {
                    filteredParams.forEach { (key, value) -> parameter(key, value.toString()) }
                }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("data" to body))
                }
            }.body()
        }
    }

    suspend fun uploadMedia(chatId: Int, formData: MultiPartFormDataContent): MessageMedia {
        return withContext(Dispatchers.IO) {
            val url = resolveApiUrl(config, "/chats/$chatId/upload")
            val response: HttpResponse = client.post(url) {
                setBody(formData)
            }
            val respJson: JsonObject = response.body()
            val apiResp = Json.decodeFromJsonElement<ApiResponse<JsonObject>>(respJson)
            if (apiResp.success == false || apiResp.error != null) {
                throw Exception(apiResp.error?.message ?: "Upload failed")
            }
            Json.decodeFromJsonElement<MessageMedia>(apiResp.data ?: throw Exception("No data received"))
        }
    }

    // ========== USERS ==========
    suspend fun getUser(id: Int): UserPublic {
        val response: ApiResponse<UserPublic> = request(HttpMethod.Get, "/users/$id")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: throw Exception("No data received")
    }

    suspend fun getMe(): UserPublic {
        val response: ApiResponse<UserPublic> = request(HttpMethod.Get, "/users/me")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: throw Exception("No data received")
    }

    // ========== CHATS ==========
    suspend fun getChat(id: Int): Chat {
        val response: ApiResponse<Chat> = request(HttpMethod.Get, "/chats/$id")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: throw Exception("No data received")
    }

    suspend fun getMyChats(): List<ChatPreview> {
        val response: ApiResponse<List<ChatPreview>> = request(HttpMethod.Get, "/chats")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: emptyList()
    }

    suspend fun getChatMembers(chatId: Int): List<ChatMember> {
        val response: ApiResponse<List<ChatMember>> = request(HttpMethod.Get, "/chats/$chatId/members")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: emptyList()
    }

    suspend fun updateMemberPermissions(
        chatId: Int, userId: Int,
        role: String? = null,
        canSendMessages: Boolean? = null,
        canManageMessages: Boolean? = null,
        canManageMembers: Boolean? = null,
        canManageChat: Boolean? = null
    ) {
        val body = mutableMapOf<String, Any>()
        role?.let { body["role"] = it }
        canSendMessages?.let { body["can_send_messages"] = it }
        canManageMessages?.let { body["can_manage_messages"] = it }
        canManageMembers?.let { body["can_manage_members"] = it }
        canManageChat?.let { body["can_manage_chat"] = it }

        val response: ApiResponse<Unit> = request(HttpMethod.Put, "/chats/$chatId/members/$userId", body)
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
    }

    suspend fun removeMember(chatId: Int, userId: Int) {
        val response: ApiResponse<Unit> = request(HttpMethod.Delete, "/chats/$chatId/members/$userId")
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
    }

    // ========== MESSAGES ==========
    suspend fun getMessages(chatId: Int, query: GetMessagesQuery? = null): List<Message> {
        val q = query ?: GetMessagesQuery()
        val params = mapOf(
            "limit" to q.limit,
            "before_id" to q.beforeId,
            "after_id" to q.afterId
        ).filterValues { it != null }

        val response: ApiResponse<List<Message>> = request(HttpMethod.Get, "/chats/$chatId/messages", params = params)
        if (response.success == false || response.error != null) throw Exception(response.error?.message ?: "Request failed")
        return response.data ?: emptyList()
    }
}