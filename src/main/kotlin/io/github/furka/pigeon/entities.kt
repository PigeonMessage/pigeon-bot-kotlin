package io.github.furka.pigeon.entities

import io.ktor.client.request.forms.*
import io.github.furka.pigeon.http.GetMessagesQuery
import io.github.furka.pigeon.types.*
import io.github.furka.pigeon.client.PigeonClient

abstract class BaseEntity(open val client: PigeonClient)

class MessageEntity(override val client: PigeonClient, private var data: Message) : BaseEntity(client) {
    val id: Int get() = data.id
    val chatId: Int get() = data.chatId
    val senderId: Int get() = data.senderId
    val content: String get() = data.content
    val replyToMessageId: Int? get() = data.replyToMessageId
    val media: List<MessageMedia>? get() = data.media
    val reactions: List<MessageReaction>? get() = data.reactions

    suspend fun edit(newContent: String) {
        client.editMessage(id, newContent)
        data = data.copy(content = newContent, isEdited = true)
    }

    suspend fun delete() {
        client.deleteMessage(id)
    }

    suspend fun addReaction(emoji: String) {
        client.addReaction(id, emoji)
    }

    suspend fun removeReaction(emoji: String) {
        client.removeReaction(id, emoji)
    }

    suspend fun reply(content: String, media: List<MessageMedia>? = null) {
        client.sendMessage(chatId, content, replyTo = id, media = media)
    }
}

class UserEntity(override val client: PigeonClient, private var data: UserPublic) : BaseEntity(client) {
    val id: Int get() = data.id

    suspend fun fetch(): UserEntity {
        val fresh = client.getUser(id)
        data = fresh
        return this
    }
}

class ChatEntity(override val client: PigeonClient, private var data: Chat) : BaseEntity(client) {
    val id: Int get() = data.id

    suspend fun fetchFull(): ChatEntity {
        val fresh = client.getChat(id)
        data = fresh
        return this
    }

    suspend fun fetchMembers(): List<ChatMember> = client.getChatMembers(id)

    suspend fun fetchMessages(
        limit: Int? = null,
        beforeId: Int? = null,
        afterId: Int? = null
    ): List<Message> = client.getMessages(id, GetMessagesQuery(limit, beforeId, afterId))

    suspend fun sendMessage(content: String, replyTo: Int? = null, media: List<MessageMedia>? = null) {
        client.sendMessage(id, content, replyTo, media)
    }

    suspend fun removeMember(userId: Int) {
        client.removeMember(id, userId)
    }

    suspend fun uploadMedia(formData: MultiPartFormDataContent): MessageMedia {
        return client.uploadMedia(id, formData)
    }
}