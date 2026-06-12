package io.github.furka.pigeon.websocket

import io.github.furka.pigeon.config.*
import io.github.furka.pigeon.types.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import io.github.furka.pigeon.client.PigeonClient

class EventManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handlers = mutableMapOf<String, MutableList<suspend (Any) -> Unit>>()

    fun on(event: String, handler: suspend (Any) -> Unit) {
        handlers.getOrPut(event) { mutableListOf() }.add(handler)
    }

    fun emit(event: String, data: Any) {
        handlers[event]?.forEach { handler ->
            scope.launch {
                try {
                    handler(data)
                } catch (e: Exception) {
                    println("Handler error for '$event': ${e.message}")
                }
            }
        }
    }

    fun removeListener(event: String, handler: suspend (Any) -> Unit) {
        handlers[event]?.remove(handler)
    }

    fun removeAllListeners(event: String? = null) {
        if (event == null) handlers.clear() else handlers.remove(event)
    }
}

class WebSocketClient(
    private val config: ClientConfig,
    private val pigeonClient: PigeonClient? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient { install(WebSockets) }
    private var webSocketSession: ClientWebSocketSession? = null
    private var isConnected = false
    private var isAuthenticated = false
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<List<WsOnlineListUser>>>()

    val events = EventManager()

    fun isConnected() = isConnected
    fun isAuthenticated() = isAuthenticated

    suspend fun connect() {
        if (isConnected) throw Exception("Client is already connected")
        val url = resolveWsUrl(config)
        connectWithRetry(url)
    }

    private suspend fun connectWithRetry(url: String) {
        while (true) {
            try {
                client.webSocket(urlString = url) {
                    webSocketSession = this
                    isConnected = true
                    isAuthenticated = false
                    events.emit("connect", Unit)
                    authenticate()
                    listen()
                }
            } catch (e: Exception) {
                println("WebSocket connection error: ${e.message}")
                isConnected = false
                isAuthenticated = false
                events.emit("disconnect", e)
                pendingRequests.values.forEach { if (!it.isCompleted) it.cancel() }
                pendingRequests.clear()

                if (config.autoReconnect) {
                    delay(config.reconnectIntervalMs)
                    continue
                } else break
            }
        }
    }

    private suspend fun authenticate() {
        val session = webSocketSession ?: throw Exception("Session not initialized")
        sendRaw(session, "authenticate", buildJsonObject {
            put("token", "Bot ${config.token}")
        })
    }

    private suspend fun listen() {
        val session = webSocketSession ?: return
        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        try {
                            val jsonString = frame.readText()
                            val root = json.parseToJsonElement(jsonString).jsonObject
                            val typeStr = root["type"]?.jsonPrimitive?.content ?: continue
                            val data = root["data"] ?: continue

                            val type = WsMessageType.entries.find { it.name.lowercase() == typeStr }
                            if (type != null) {
                                handleMessage(type, data)
                            } else {
                                events.emit(typeStr, data)
                            }
                        } catch (e: Exception) {
                            println("Failed to process WS frame: ${e.message}")
                            events.emit("error", e)
                        }
                    }
                    is Frame.Close -> {
                        println("WebSocket closed by server")
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            println("WebSocket listen loop error: ${e.message}")
            events.emit("error", e)
        }
    }

    private suspend fun handleMessage(type: WsMessageType, data: JsonElement) {
        val dataObj = data as? JsonObject ?: return
        val eventName = type.name.lowercase()

        events.emit("raw", mapOf("type" to eventName, "data" to dataObj))

        when (type) {
            WsMessageType.AUTHENTICATED -> {
                isAuthenticated = true
                val authData = json.decodeFromJsonElement<WsAuthenticatedData>(data)
                events.emit(eventName, authData)
                events.emit("ready", Unit)
            }
            WsMessageType.ERROR -> {
                val err = json.decodeFromJsonElement<WsErrorData>(data)
                if (!isAuthenticated && err.message == "Please authenticate first") return
                events.emit(eventName, Exception(err.message))
            }
            WsMessageType.NEW_MESSAGE, WsMessageType.MESSAGE_EDITED -> {
                val msgData = dataObj["message"] ?: run {
                    println("Server sent '$eventName' without 'message' field")
                    return
                }
                try {
                    val msg = deserializeMessage(msgData)
                    if (pigeonClient != null) {
                        val entity = pigeonClient.createMessageEntity(msg)
                        events.emit(eventName, entity)
                    } else {
                        events.emit(eventName, msg)
                    }
                } catch (e: Exception) {
                    println("Failed to deserialize message: ${e.message}")
                    e.printStackTrace()
                }
            }
            WsMessageType.MESSAGE_DELETED -> events.emit(eventName, dataObj)
            WsMessageType.REACTION_ADDED -> {
                val reaction = dataObj["reaction"] ?: return
                val reactionObj = reaction.jsonObject
                val unifiedData = buildJsonObject {
                    reactionObj["message_id"]?.let { put("message_id", it) } ?: dataObj["message_id"]?.let { put("message_id", it) }
                    reactionObj["user_id"]?.let { put("user_id", it) }
                    reactionObj["emoji"]?.let { put("emoji", it) }
                    reactionObj["id"]?.let { put("reaction_id", it) }
                    reactionObj["created_at"]?.let { put("created_at", it) }
                }
                events.emit(eventName, unifiedData)
            }
            WsMessageType.REACTION_REMOVED -> events.emit(eventName, dataObj)
            WsMessageType.USER_ONLINE, WsMessageType.USER_OFFLINE, WsMessageType.USER_TYPING -> {
                events.emit(eventName, dataObj)
            }
            WsMessageType.ONLINE_LIST -> {
                val onlineData = json.decodeFromJsonElement<WsOnlineListData>(data)
                events.emit(eventName, onlineData)
                val reqId = dataObj["request_id"]?.jsonPrimitive?.content
                if (reqId != null && reqId in pendingRequests) {
                    val deferred = pendingRequests.remove(reqId)!!
                    if (!deferred.isCompleted) deferred.complete(onlineData.users)
                }
            }
            WsMessageType.POLL_VOTED, WsMessageType.POLL_CLOSED, WsMessageType.POLL_CREATED -> {
                events.emit(eventName, dataObj)
            }
            else -> events.emit(eventName, dataObj)
        }
    }

    private fun deserializeMessage(msgDict: JsonElement): Message {
        val md = msgDict.jsonObject.toMutableMap()

        md["reactions"]?.let { el ->
            if (el is JsonArray) {
                val processed = if (el.isNotEmpty()) {
                    json.encodeToJsonElement(el.map { json.decodeFromJsonElement<MessageReaction>(it) })
                } else {
                    JsonArray(emptyList())
                }
                md["reactions"] = processed
            }
        }

        md["new_chat_members"]?.let { el ->
            if (el is JsonArray) {
                val processed = if (el.isNotEmpty()) {
                    json.encodeToJsonElement(el.map { json.decodeFromJsonElement<UserPublic>(it) })
                } else {
                    JsonArray(emptyList())
                }
                md["new_chat_members"] = processed
            }
        }

        md["left_chat_member"]?.let { el ->
            md["left_chat_member"] = json.encodeToJsonElement(
                json.decodeFromJsonElement<UserPublic>(el)
            )
        }

        md["pinned_message"]?.let { el ->
            md["pinned_message"] = json.encodeToJsonElement(deserializeMessage(el))
        }

        return json.decodeFromJsonElement(Message.serializer(), buildJsonObject {
            md.forEach { (k, v) -> put(k, v) }
        })
    }

    private suspend fun sendRaw(session: ClientWebSocketSession, type: String, data: JsonElement) {
        if (!isAuthenticated && type != "authenticate") throw Exception("Please authenticate first.")

        val payload = buildJsonObject {
            put("type", type)
            put("data", data)
        }

        session.outgoing.send(Frame.Text(payload.toString()))
    }

    // ========== PUBLIC METHODS ==========
    suspend fun sendMessage(chatId: Int, content: String, replyTo: Int? = null, media: List<JsonObject>? = null) {
        val session = webSocketSession ?: throw Exception("Not connected")
        val data = buildJsonObject {
            put("chat_id", chatId)
            put("content", content)
            put("reply_to", replyTo?.let { JsonPrimitive(it) } ?: JsonNull)
            put("media", media?.let { JsonArray(it) } ?: JsonNull)
        }
        sendRaw(session, "send_message", data)
    }

    suspend fun editMessage(messageId: Int, content: String) {
        val session = webSocketSession ?: throw Exception("Not connected")
        sendRaw(session, "edit_message", buildJsonObject {
            put("message_id", messageId)
            put("content", content)
        })
    }

    suspend fun deleteMessage(messageId: Int) {
        val session = webSocketSession ?: throw Exception("Not connected")
        sendRaw(session, "delete_message", buildJsonObject {
            put("message_id", messageId)
        })
    }

    suspend fun addReaction(messageId: Int, emoji: String) {
        val session = webSocketSession ?: throw Exception("Not connected")
        sendRaw(session, "add_reaction", buildJsonObject {
            put("message_id", messageId)
            put("emoji", emoji)
        })
    }

    suspend fun removeReaction(messageId: Int, emoji: String) {
        val session = webSocketSession ?: throw Exception("Not connected")
        sendRaw(session, "remove_reaction", buildJsonObject {
            put("message_id", messageId)
            put("emoji", emoji)
        })
    }

    suspend fun setTyping(chatId: Int, isTyping: Boolean = true) {
        val session = webSocketSession ?: throw Exception("Not connected")
        sendRaw(session, "typing", buildJsonObject {
            put("chat_id", chatId)
            put("is_typing", isTyping)
        })
    }

    suspend fun getOnlineList(): List<WsOnlineListUser> {
        val session = webSocketSession ?: throw Exception("Not connected")
        val reqId = java.util.UUID.randomUUID().toString()
        val deferred = CompletableDeferred<List<WsOnlineListUser>>()
        pendingRequests[reqId] = deferred

        try {
            sendRaw(session, "get_online_list", buildJsonObject {
                put("request_id", reqId)
            })
            return withTimeout(5000L) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(reqId)
            throw Exception("Timed out waiting for online list")
        } catch (e: Exception) {
            pendingRequests.remove(reqId)
            throw e
        }
    }

    suspend fun disconnect() {
        pendingRequests.values.forEach { if (!it.isCompleted) it.cancel() }
        pendingRequests.clear()
        webSocketSession?.close()
        isConnected = false
        isAuthenticated = false
        client.close()
    }
}