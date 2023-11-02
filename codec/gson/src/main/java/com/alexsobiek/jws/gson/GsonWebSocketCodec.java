package com.alexsobiek.jws.gson;

import com.alexsobiek.jws.WebSocketMessageCodec;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class GsonWebSocketCodec implements WebSocketMessageCodec<JsonObject, JsonObject> {
    @Override
    public TextWebSocketFrame encode(JsonObject msg) {
        return new TextWebSocketFrame(msg.toString());
    }

    @Override
    public JsonObject decode(TextWebSocketFrame frame) {
        return JsonParser.parseString(frame.text()).getAsJsonObject();
    }
}
