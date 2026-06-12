package io.github.furka.pigeon

import io.github.furka.pigeon.config.ClientConfig
import io.github.furka.pigeon.entities.MessageEntity
import kotlinx.coroutines.*
import io.github.furka.pigeon.client.PigeonClient
import io.github.furka.pigeon.types.WsAuthenticatedData

suspend fun main() {
    val config = ClientConfig(token = "")
    val client = PigeonClient(config)

    client.onEvent("ready") {
        println("Bot is ready!")
        client.sendMessage(chatId = 42, content = "Hello!")
    }

    client.onEvent("authenticated") { data ->
        when (data) {
            is WsAuthenticatedData -> {
                println("Authenticated as user ID: ${data.userId}")
            }
            else -> {
                println("Authenticated (raw): $data")
            }
        }
    }

    client.onEvent("error") { error ->
        val msg = when (error) {
            is Throwable -> error.message ?: error.toString()
            else -> error.toString()
        }
        println("Error: $msg")
        if (error is Throwable) {
            error.printStackTrace()
        }
    }

    client.onEvent("new_message") { data ->
        when (data) {
            is MessageEntity -> {
                println("[${data.chatId}] ${data.senderId}: ${data.content}")
            }
            else -> println("Raw message: $data")
        }
    }

    println("Connecting...")
    try {
        client.start()
        println("Connected!")
        awaitCancellation()
    } catch (e: Exception) {
        println("Fatal: ${e.message}")
        e.printStackTrace()
    } finally {
        println("Shutting down...")
        client.close()
    }
}