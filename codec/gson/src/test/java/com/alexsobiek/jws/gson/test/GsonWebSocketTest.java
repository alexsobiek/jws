package com.alexsobiek.jws.gson.test;

import com.alexsobiek.jws.WebSocketClient;
import com.alexsobiek.jws.WebSocketClientHandler;
import com.alexsobiek.jws.gson.GsonWebSocketCodec;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class GsonWebSocketTest {
    @Test
    public void connectAndSend() {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject message = new JsonObject();
        message.addProperty("event", "ping");

        try (WebSocketClient<JsonObject, JsonObject> client = new WebSocketClient<>(new URI("wss://ws.postman-echo.com/raw"), new GsonWebSocketCodec(), new WebSocketClientHandler<>() {
            @Override
            public void onMessage(ChannelHandlerContext ctx, JsonObject message) {
                future.complete(message);
            }

            @Override
            public void onException(ChannelHandlerContext ctx, Throwable exception) {
                future.completeExceptionally(exception);
            }
        })) {
            new Thread(() -> {
                try {
                    client.open().sync();
                    client.writeAndFlush(message); // Send message
                    while (!future.isDone()) Thread.onSpinWait(); // Keep connection open until message is received
                    client.close(); // Close connection
                    Assertions.assertFalse(client.isOpen());
                } catch (Exception e) {
                    Assertions.fail(e);
                }
            }).start();
            Assertions.assertEquals(future.get(), message);
        } catch(Exception e) {
            Assertions.fail(e);
        };
    }
}
