package org.cubeengine.module.observe;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WebServer {

    private final InetSocketAddress bindAddress;
    private final ThreadFactory threadFactory;
    private final Logger logger;
    private final ConcurrentHashMap<String, WebHandler> handlerMap = new ConcurrentHashMap<>();

    private NioEventLoopGroup eventLoopGroup;
    private Channel channel;

    public WebServer(InetSocketAddress bindAddress, ThreadFactory threadFactory, Logger logger) {
        this.bindAddress = bindAddress;
        this.threadFactory = threadFactory;
        this.logger = logger;
    }

    public boolean registerHandler(String route, WebHandler handler) {
        return handlerMap.putIfAbsent(route, handler) == null;
    }

    public boolean registerHandlerAndStart(String route, WebHandler handler) {
        if (registerHandler(route, handler)) {
            start();
            return true;
        }
        return false;
    }

    synchronized void start() {
        if (this.eventLoopGroup != null) {
            return;
        }

        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        try
        {
            this.eventLoopGroup = new NioEventLoopGroup(threadFactory);
            serverBootstrap.group(this.eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast("aggregator", new HttpObjectAggregator(1000))
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("httpHandler", new HttpRequestHandler(logger, handlerMap));
                        }
                    })
                    .localAddress(this.bindAddress);

            this.channel = serverBootstrap.bind().sync().channel();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("The API server failed to start!", e);
        }
    }

    synchronized void stop() {
        this.channel = null;
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
            this.eventLoopGroup = null;
        }
    }
    public synchronized InetSocketAddress getBoundAddress() {
        if (this.channel == null) {
            throw new IllegalStateException("Server seems not to be running!");
        }
        return ((InetSocketAddress) this.channel.localAddress());
    }

}
