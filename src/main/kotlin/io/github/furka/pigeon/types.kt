package io.github.furka.pigeon.types

import kotlinx.serialization.*

@Serializable
data class ApiError(val code: Int, val message: String)

@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val error: ApiError? = null,
    val success: Boolean? = null
)

@Serializable
data class UserPublic(
    val id: Int,
    val username: String,
    val name: String,
    @SerialName("is_bot") val isBot: Boolean,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

@Serializable
enum class ChatType {
    @SerialName("DM") DM,
    @SerialName("GROUP") GROUP,
    @SerialName("CHANNEL") CHANNEL
}

@Serializable
data class ChatMember(
    @SerialName("chat_id") val chatId: Int,
    @SerialName("user_id") val userId: Int,
    val role: String,
    @SerialName("custom_nickname") val customNickname: String? = null,
    @SerialName("can_send_messages") val canSendMessages: Boolean,
    @SerialName("can_manage_messages") val canManageMessages: Boolean,
    @SerialName("can_manage_members") val canManageMembers: Boolean,
    @SerialName("can_manage_chat") val canManageChat: Boolean,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_read_message_id") val lastReadMessageId: Int? = null
)

@Serializable
data class Chat(
    val id: Int,
    @SerialName("chat_type") val chatType: ChatType,
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("owner_id") val ownerId: Int? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<ChatMember>,
    @SerialName("member_count") val memberCount: Int
)

@Serializable
data class ChatPreview(
    val id: Int,
    @SerialName("chat_type") val chatType: ChatType,
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("last_message") val lastMessage: Message? = null,
    @SerialName("last_user") val lastUser: UserPublic? = null,
    @SerialName("other_user") val otherUser: UserPublic? = null,
    @SerialName("last_read_message_id") val lastReadMessageId: Int? = null,
    @SerialName("unread_count") val unreadCount: Int
)

@Serializable
@SerialName("Photo")
data class PhotoMedia(
    val type: String = "Photo",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("file_size") val fileSize: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val spoiler: Boolean = false
)

@Serializable
@SerialName("Document")
data class DocumentMedia(
    val type: String = "Document",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("file_size") val fileSize: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

@Serializable
@SerialName("Video")
data class VideoMedia(
    val type: String = "Video",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val duration: Double? = null,
    @SerialName("file_size") val fileSize: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("supports_streaming") val supportsStreaming: Boolean = true
)

@Serializable
@SerialName("Audio")
data class AudioMedia(
    val type: String = "Audio",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val duration: Double? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("file_size") val fileSize: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

@Serializable
@SerialName("Voice")
data class VoiceMedia(
    val type: String = "Voice",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val duration: Double? = null,
    @SerialName("file_size") val fileSize: Int = 0,
    val waveform: List<Int>? = null
)

@Serializable
@SerialName("Gif")
data class GifMedia(
    val type: String = "Gif",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val duration: Double? = null,
    @SerialName("file_size") val fileSize: Int = 0,
    @SerialName("preview_url") val previewUrl: String? = null
)

@Serializable
@SerialName("Sticker")
data class StickerMedia(
    val type: String = "Sticker",
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val emoji: String? = null,
    @SerialName("set_name") val setName: String? = null
)

@Serializable
@SerialName("Geo")
data class GeoMedia(
    val type: String = "Geo",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val title: String? = null,
    val address: String? = null
)

@Serializable
@SerialName("Contact")
data class ContactMedia(
    val type: String = "Contact",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String? = null,
    val vcard: String? = null
)

@Serializable
data class PollOption(
    val text: String,
    val id: Int? = null,
    @SerialName("poll_id") val pollId: Int? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("votes_count") val votesCount: Int? = null,
    val voters: List<UserPublic>? = null
)

@Serializable
@SerialName("Poll")
data class PollMedia(
    val type: String = "Poll",
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    @SerialName("allows_multiple") val allowsMultiple: Boolean = false,
    val anonymous: Boolean = true,
    @SerialName("is_quiz") val isQuiz: Boolean = false,
    @SerialName("has_voted") val hasVoted: Boolean? = null,
    @SerialName("user_voted_options") val userVotedOptions: List<Int>? = null,
    val explanation: String? = null,
    @SerialName("close_period") val closePeriod: Int? = null,
    @SerialName("correct_option_indexes") val correctOptionIndexes: List<Int>? = null,
    @SerialName("allow_revote") val allowRevote: Boolean = true
)

typealias MessageMedia = kotlinx.serialization.json.JsonObject

@Serializable
data class MessageReaction(
    val id: Int,
    @SerialName("message_id") val messageId: Int,
    @SerialName("user_id") val userId: Int,
    val emoji: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Message(
    val id: Int,
    @SerialName("chat_id") val chatId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("reply_to_message_id") val replyToMessageId: Int? = null,
    val content: String,
    val media: List<MessageMedia>? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val reactions: List<MessageReaction>? = null,
    @SerialName("new_chat_members") val newChatMembers: List<UserPublic>? = null,
    @SerialName("left_chat_member") val leftChatMember: UserPublic? = null,
    @SerialName("left_chat_member_id") val leftChatMemberId: Int? = null,
    @SerialName("new_chat_title") val newChatTitle: String? = null,
    @SerialName("delete_chat_photo") val deleteChatPhoto: Boolean? = null,
    @SerialName("chat_created_type") val chatCreatedType: String? = null,
    @SerialName("migrate_to_chat_id") val migrateToChatId: Int? = null,
    @SerialName("migrate_from_chat_id") val migrateFromChatId: Int? = null,
    @SerialName("pinned_message") val pinnedMessage: Message? = null
)


@Serializable
enum class WsMessageType {
    // client -> server
    @SerialName("ping") PING,
    @SerialName("authenticate") AUTHENTICATE,
    @SerialName("subscribe") SUBSCRIBE,
    @SerialName("unsubscribe") UNSUBSCRIBE,
    @SerialName("send_message") SEND_MESSAGE,
    @SerialName("edit_message") EDIT_MESSAGE,
    @SerialName("delete_message") DELETE_MESSAGE,
    @SerialName("add_reaction") ADD_REACTION,
    @SerialName("remove_reaction") REMOVE_REACTION,
    @SerialName("vote_poll") VOTE_POLL,
    @SerialName("unvote_poll") UNVOTE_POLL,
    @SerialName("mark_as_read") MARK_AS_READ,
    @SerialName("mark_all_as_read") MARK_ALL_AS_READ,
    @SerialName("typing") TYPING,
    @SerialName("get_online_list") GET_ONLINE_LIST,

    // server -> client
    @SerialName("pong") PONG,
    @SerialName("authenticated") AUTHENTICATED,
    @SerialName("error") ERROR,
    @SerialName("new_message") NEW_MESSAGE,
    @SerialName("message_edited") MESSAGE_EDITED,
    @SerialName("message_deleted") MESSAGE_DELETED,
    @SerialName("reaction_added") REACTION_ADDED,
    @SerialName("reaction_removed") REACTION_REMOVED,
    @SerialName("user_online") USER_ONLINE,
    @SerialName("user_offline") USER_OFFLINE,
    @SerialName("user_typing") USER_TYPING,
    @SerialName("message_read") MESSAGE_READ,
    @SerialName("all_messages_read") ALL_MESSAGES_READ,
    @SerialName("poll_created") POLL_CREATED,
    @SerialName("poll_voted") POLL_VOTED,
    @SerialName("poll_closed") POLL_CLOSED,
    @SerialName("online_list") ONLINE_LIST
}

@Serializable
data class WsEnvelope<T>(
    val type: WsMessageType,
    val data: T
)

@Serializable
data class WsAuthenticatedData(
    @SerialName("user_id") val userId: Int
)

@Serializable
data class WsErrorData(val message: String)

@Serializable
data class WsOnlineListUser(val id: Int)

@Serializable
data class WsOnlineListData(val users: List<WsOnlineListUser>)