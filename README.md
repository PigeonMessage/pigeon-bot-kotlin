# Pigeon Bot Kotlin

[![Kotlin](https://img.shields.io/badge/kotlin-%3E%3D1.9-blue.svg)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A Kotlin library for building chat bots on the Pigeon Messenger.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.furka.pigeon:pigeon-bot:1.0.0")
}
```

Or for Maven:

```xml
<dependency>
    <groupId>io.github.furka.pigeon</groupId>
    <artifactId>pigeon-bot</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```kotlin
import io.github.furka.pigeon.client.PigeonClient
import io.github.furka.pigeon.config.ClientConfig
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val token = System.getenv("BOT_TOKEN") ?: ""
    if (token.isEmpty()) {
        println("Please set your bot token in the BOT_TOKEN environment variable")
        return@runBlocking
    }

    val config = ClientConfig(
        token = token,
        baseUrl = "http://localhost:8080",
        wsUrl = "ws://localhost:8080/api/v1/ws"
    )
    val client = PigeonClient(config)

    client.onEvent("ready") {
        println("Bot is ready!")
    }

    client.onEvent("new_message") { data ->
        if (data is io.github.furka.pigeon.entities.MessageEntity) {
            if ("hello" in data.content.lowercase()) {
                data.reply("hi")
            }
        }
    }

    try {
        client.start()
    } catch (e: Exception) {
        println("Fatal: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
```

## Configuration

```kotlin
data class ClientConfig(
    val token: String,
    val baseUrl: String? = null,           // Default: "http://localhost:8080"
    val wsUrl: String? = null,             // Default: derived from baseUrl
    val autoReconnect: Boolean = true,    // Auto-reconnect on disconnect
    val reconnectIntervalMs: Long = 5000  // Reconnect delay in milliseconds
)
```

## Base Events

- `ready`: Fires when the bot connects successfully
- `authenticated`: Fires after successful authentication
- `new_message`: Triggered on new messages
- `message_edited`: Triggered when a message is edited
- `message_deleted`: Triggered when a message is deleted
- `reaction_added`: Triggered when a reaction is added
- `reaction_removed`: Triggered when a reaction is removed
- `user_online`: Triggered when a user comes online
- `user_offline`: Triggered when a user goes offline
- `user_typing`: Triggered when a user is typing
- `error`: Emitted on connection/authentication errors

## HTTP API Methods

```kotlin
// User methods
suspend fun getUser(id: Int): UserPublic
suspend fun getMe(): UserPublic

// Chat methods
suspend fun getChat(id: Int): Chat
suspend fun getMyChats(): List<ChatPreview>
suspend fun getChatMembers(chatId: Int): List<ChatMember>
suspend fun updateMemberPermissions(...)
suspend fun removeMember(chatId: Int, userId: Int)

// Message methods
suspend fun getMessages(chatId: Int, query: GetMessagesQuery?): List<Message>
suspend fun uploadMedia(chatId: Int, formData: MultiPartFormDataContent): MessageMedia
```

## WebSocket Methods

```kotlin
suspend fun sendMessage(chatId: Int, content: String, replyTo: Int?, media: List<MessageMedia>?)
suspend fun editMessage(messageId: Int, content: String)
suspend fun deleteMessage(messageId: Int)
suspend fun addReaction(messageId: Int, emoji: String)
suspend fun removeReaction(messageId: Int, emoji: String)
suspend fun setTyping(chatId: Int, isTyping: Boolean)
suspend fun getOnlineList(): List<WsOnlineListUser>
```

## Entity Classes

The library provides entity classes for convenient interaction with objects:

```kotlin
class MessageEntity : BaseEntity {
    suspend fun edit(newContent: String)
    suspend fun delete()
    suspend fun addReaction(emoji: String)
    suspend fun removeReaction(emoji: String)
    suspend fun reply(content: String, media: List<MessageMedia>?)
}

class UserEntity : BaseEntity {
    suspend fun fetch(): UserEntity
}

class ChatEntity : BaseEntity {
    suspend fun fetchFull(): ChatEntity
    suspend fun fetchMembers(): List<ChatMember>
    suspend fun fetchMessages(...): List<Message>
    suspend fun sendMessage(content: String, replyTo: Int?, media: List<MessageMedia>?)
    suspend fun removeMember(userId: Int)
    suspend fun uploadMedia(formData: MultiPartFormDataContent): MessageMedia
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
