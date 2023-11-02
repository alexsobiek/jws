package com.alexsobiek.jws;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public interface WebSocketMessageCodec<I, O> {
    TextWebSocketFrame encode(O msg);
    I decode(TextWebSocketFrame frame);
}
