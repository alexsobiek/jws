package com.alexsobiek.jws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface WebSocketClientHandler<I> {
    void onMessage(ChannelHandlerContext ctx, I message);

    void onException(ChannelHandlerContext ctx, Throwable exception);

    default void onOpen(ChannelHandlerContext ctx) {
    }

    default void onFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    }

    default void onClose(ChannelHandlerContext ctx) {
    }
}
