package com.alexsobiek.jws;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;

public class InboundHandler<I> extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClient<I, ?> client;
    private ChannelPromise handshakeFuture;
    private ChannelPromise openFuture;

    public InboundHandler(WebSocketClient<I, ?> client) {
        this.client = client;
    }

    protected ChannelPromise getOpenFuture() {
        return openFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
        openFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        client.getHandshaker().handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        client.getHandler().onClose(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!client.getHandshaker().isHandshakeComplete()) {
            try {
                client.getHandshaker().finishHandshake(ch, (FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                openFuture.setSuccess();
                client.getHandler().onOpen(ctx);
            } catch (WebSocketHandshakeException e) {
                handshakeFuture.setFailure(e);
                openFuture.setFailure(e);
                client.getHandler().onException(ctx, e);
            }
            return;
        }

        if (msg instanceof FullHttpResponse response) {
            client.getHandler().onException(ctx, new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')'));
        } else {
            WebSocketFrame frame = (WebSocketFrame) msg;
            client.getHandler().onFrame(ctx, frame);
            if (frame instanceof TextWebSocketFrame txtFrame)
                client.getHandler().onMessage(ctx, client.getCodec().decode(txtFrame));
            else if (frame instanceof CloseWebSocketFrame) ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!handshakeFuture.isDone()) handshakeFuture.setFailure(cause);
        if (!openFuture.isDone()) openFuture.setFailure(cause);
        client.getHandler().onException(ctx, cause);
        client.close();
    }
}
