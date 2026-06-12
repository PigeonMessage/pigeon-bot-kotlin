package io.github.furka.pigeon.client

import io.ktor.client.request.forms.*
import io.github.furka.pigeon.config.*
import io.github.furka.pigeon.http.*
import io.github.furka.pigeon.websocket.*
import io.github.furka.pigeon.types.*
import io.github.furka.pigeon.entities.*
import kotlinx.coroutines.*

class PigeonClient(val config: ClientConfig) {
    init {
        if (config.token.isBlank()) throw IllegalArgumentException("Bot token is required")
    }

    val http = HttpClient(config)
    val ws = WebSocketClient(config, this) // Передаем this для создания Entity объектов
    private var isClosed = false

    fun isConnected() = ws.isConnected()
    fun isAuthenticated() = ws.isAuthenticated()

    // --- HTTP METHODS ---
    suspend fun getUser(id: Int) = http.getUser(id)
    suspend fun getMe() = http.getMe()
    suspend fun getChat(id: Int) = http.getChat(id)
    suspend fun getMyChats() = http.getMyChats()
    suspend fun getChatMembers(chatId: Int) = http.getChatMembers(chatId)
    suspend fun updateMemberPermissions(
        chatId: Int,
        userId: Int,
        role: String? = null,
        canSendMessages: Boolean? = null,
        canManageMessages: Boolean? = null,
        canManageMembers: Boolean? = null,
        canManageChat: Boolean? = null
    ) = http.updateMemberPermissions(
        chatId, userId,
        role, canSendMessages, canManageMessages, canManageMembers, canManageChat
    )
    suspend fun getMessages(chatId: Int, query: GetMessagesQuery? = null) = http.getMessages(chatId, query)
    suspend fun uploadMedia(chatId: Int, formData: MultiPartFormDataContent) = http.uploadMedia(chatId, formData)
    // suspend fun uploadMedia(chatId: Int, formData: io.ktor.client.plugins.websocket.Frame) = http.uploadMedia(chatId, formData) // Заглушка
    suspend fun removeMember(chatId: Int, userId: Int) = http.removeMember(chatId, userId)

    // --- WEBSOCKET METHODS ---
    suspend fun connect() = ws.connect()
    suspend fun disconnect() = ws.disconnect()
    suspend fun sendMessage(chatId: Int, content: String, replyTo: Int? = null, media: List<MessageMedia>? = null) =
        ws.sendMessage(chatId, content, replyTo, media)

    suspend fun editMessage(messageId: Int, content: String) = ws.editMessage(messageId, content)
    suspend fun deleteMessage(messageId: Int) = ws.deleteMessage(messageId)
    suspend fun addReaction(messageId: Int, emoji: String) = ws.addReaction(messageId, emoji)
    suspend fun removeReaction(messageId: Int, emoji: String) = ws.removeReaction(messageId, emoji)
    suspend fun setTyping(chatId: Int, isTyping: Boolean = true) = ws.setTyping(chatId, isTyping)
    suspend fun getOnlineList() = ws.getOnlineList()

    // --- EVENT HANDLING ---
    fun onEvent(eventName: String, handler: suspend (Any) -> Unit) {
        ws.events.on(eventName, handler)
    }

    fun addEventListener(eventName: String, handler: suspend (Any) -> Unit) {
        ws.events.on(eventName, handler)
    }

    fun removeEventListener(eventName: String, handler: suspend (Any) -> Unit) {
        ws.events.removeListener(eventName, handler)
    }

    fun removeAllEventListeners(eventName: String? = null) {
        ws.events.removeAllListeners(eventName)
    }

    // --- CONVENIENCE METHODS ---
    fun createMessageEntity(messageData: Message): MessageEntity {
        return MessageEntity(this, messageData)
    }

    fun createUserEntity(userData: UserPublic): UserEntity {
        return UserEntity(this, userData)
    }

    fun createChatEntity(chatData: Chat): ChatEntity {
        return ChatEntity(this, chatData)
    }

    suspend fun start() = connect()

    suspend fun close() {
        if (isClosed) return
        try {
            disconnect()
        } catch (e: Exception) {
            println("Error disconnecting WebSocket: $e")
        }
        try {
            http.close()
        } catch (e: Exception) {
            println("Error closing HTTP client: $e")
        }
        isClosed = true
    }
}