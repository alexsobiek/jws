package com.alexsobiek.jws;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.URI;

public class WebSocketClient<I, O> implements AutoCloseable {
    private final URI uri;
    private final SslContext sslContext;
    private final WebSocketClientHandshaker handshaker;
    private final InboundHandler<?> inboundHandler;
    private final WebSocketClientHandler<I> clientHandler;
    private final WebSocketMessageCodec<I, O> codec;

    private Channel channel;

    public WebSocketClient(String host, int port, String path, SslContext sslContext, WebSocketMessageCodec<I, O> codec, WebSocketClientHandler<I> clientHandler) {
        this.uri = URI.create((sslContext != null ? "wss" : "ws") + "://" + host + ":" + port + path);
        this.sslContext = sslContext;
        this.clientHandler = clientHandler;
        this.codec = codec;
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
        this.inboundHandler = new InboundHandler<>(this);
    }

    public WebSocketClient(String host, int port, String path, WebSocketMessageCodec<I, O> codec, WebSocketClientHandler<I>  clientHandler) {
        this(host, port, path, null, codec, clientHandler);
    }

    public WebSocketClient(URI uri, WebSocketMessageCodec<I, O> codec, WebSocketClientHandler<I> clientHandler) throws Exception {
        this(uri.getHost(), (short) (uri.getPort() == -1 ? (uri.getScheme().equals("wss") ? 443 : 80) : uri.getPort()), uri.getPath(), uri.getScheme().equals("wss") ? SslContextBuilder.forClient().build() : null, codec, clientHandler);
    }

    public ChannelFuture open() throws InterruptedException {
        if (channel != null) throw new IllegalStateException("Client is already open!");
        EventLoopGroup group = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        Class<? extends SocketChannel> channel = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(channel).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (sslContext != null)
                    pipeline.addLast(sslContext.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                pipeline.addLast(new ReadTimeoutHandler(2))
                        .addLast(new HttpClientCodec())
                        .addLast(new HttpObjectAggregator(8192))
                        .addLast(WebSocketClientCompressionHandler.INSTANCE)
                        .addLast(inboundHandler);
            }
        });
        this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
        return inboundHandler.getOpenFuture();
    }

    public void writeAndFlush(O message) {
        if (channel == null) throw new IllegalStateException("Client is not open!");
        channel.writeAndFlush(codec.encode(message)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void close() throws Exception {
        if (channel == null) throw new IllegalStateException("Client is not open!");
        channel.close().sync();
    }

    public boolean isOpen() {
        return channel != null && channel.isActive();
    }

    protected WebSocketClientHandshaker getHandshaker() {
        return handshaker;
    }

    protected WebSocketClientHandler<I> getHandler() {
        return clientHandler;
    }

    protected WebSocketMessageCodec<I, O> getCodec() {
        return codec;
    }
}

