/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.plugin.PluginContainer;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class PrometheusMetricsService
{
    private final CollectorRegistry syncRegistry;
    private final CollectorRegistry asyncRegistry;
    private final ThreadFactory tf;
    private final TaskManager tm;
    private final InetSocketAddress bindAddress;
    private final Logger logger;
    private NioEventLoopGroup eventLoopGroup = null;
    private Channel channel;

    public PrometheusMetricsService(ThreadFactory tf, TaskManager tm, InetSocketAddress bindAddress, Logger logger)
    {
        this.tf = tf;
        this.tm = tm;
        this.bindAddress = bindAddress;
        this.logger = logger;
        this.syncRegistry = new CollectorRegistry();
        this.asyncRegistry = new CollectorRegistry();
    }

    public synchronized void registerSync(PluginContainer plugin, Collector collector)
    {
        this.syncRegistry.register(collector);
    }

    public synchronized void registerAsync(PluginContainer plugin, Collector collector)
    {
        this.asyncRegistry.register(collector);
    }

    synchronized void startExporter() {
        if (this.eventLoopGroup != null) {
            return;
        }

        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        try
        {
            this.eventLoopGroup = new NioEventLoopGroup(tf);
            serverBootstrap.group(this.eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast("aggregator", new HttpObjectAggregator(1000))
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("httpHandler", new HttpRequestHandler(logger, syncRegistry, asyncRegistry, tm));
                        }
                    })
                    .localAddress(this.bindAddress);

            this.channel = serverBootstrap.bind().sync().channel();
        }
        catch (Exception e)
        {
            stopExporter();
            throw new RuntimeException("The API server failed to start!", e);
        }
    }

    synchronized void stopExporter() {
        this.channel = null;
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
            this.eventLoopGroup = null;
        }
    }

    public synchronized int getPort() {
        return ((InetSocketAddress) this.channel.localAddress()).getPort();
    }
}
