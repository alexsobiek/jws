# JWS
JWS is a very simple & modern Java websocket client using Netty. 

## Usage
In this example, we're going to connect to the Postman Echo Websocket service, send a message to it, and wait for the
echoed message to be returned. This example makes use of the [Gson Codec](https://github.com/alexsobiek/jws/tree/main/codec/gson)
to keep things simple.

```java
import com.alexsobiek.jws.WebSocketClient;
import com.alexsobiek.jws.WebSocketClientHandler;
import com.alexsobiek.jws.gson.GsonWebSocketCodec;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandlerContext;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

class Example {
    public static void main(String[] args) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject message = new JsonObject();
        message.addProperty("event", "ping");

        try (WebSocketClient<JsonObject, JsonObject> client = new WebSocketClient<>(new URI("wss://ws.postman-echo.com/raw"), new GsonWebSocketCodec(), new WebSocketClientHandler<>() {
            // Here we've created an inline WebSocketClientHandler instance, but you can create your own class that extends WebSocketClientHandler
            // and override the onMessage and onException methods to handle messages and exceptions respectively. This is just a simple example.
            public void onMessage(ChannelHandlerContext ctx, JsonObject message) { // Called when a message is received
                future.complete(message); // Complete future with message
            }

            public void onException(ChannelHandlerContext ctx, Throwable exception) { // Called when an exception is thrown
                future.completeExceptionally(exception); // Complete future with exception
            }
        })) {
            try {
                client.open().sync(); // Open and wait for connection
                client.writeAndFlush(message); // Send message
                System.out.printf("Sent message: %s", message.toString());
                while (!future.isDone()) Thread.onSpinWait(); // Keep connection open until message is received
                client.close(); // Close connection
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            JsonObject response = future.get(); // Wait for message to be received
            System.out.printf("Received message: %s", response.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

## Depending on JWS
JWS can currently be found at [https://jitpack.io/#com.alexsobiek/JWS](https://jitpack.io/#com.alexsobiek/JWS).
Instructions for your build tool can be found there.