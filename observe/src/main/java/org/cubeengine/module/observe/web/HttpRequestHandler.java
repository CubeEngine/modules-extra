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
package org.cubeengine.module.observe.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentMap;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    private final Logger logger;
    private final ConcurrentMap<String, WebHandler> handlerMap;

    public HttpRequestHandler(Logger logger, ConcurrentMap<String, WebHandler> handlerMap)
    {
        this.logger = logger;
        this.handlerMap = handlerMap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable t)
    {
        error(context, INTERNAL_SERVER_ERROR, t);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest message) throws Exception
    {
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(message.uri());

        final WebHandler handler = handlerMap.get(queryStringDecoder.path());
        if (handler == null) {
            logger.warn("Received request for {}", message.uri());
            error(ctx, HttpResponseStatus.NOT_FOUND, null);
        } else {
            handler.handleRequest((response) -> writeSuccessful(ctx, response), (code, t) -> error(ctx, code, t), message, queryStringDecoder);
        }
    }

    private void writeSuccessful(ChannelHandlerContext ctx, FullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(CLOSE).addListener(CLOSE_ON_FAILURE);
    }

    private void error(ChannelHandlerContext context, HttpResponseStatus error, Throwable t)
    {
        if (t != null) {
            logger.error("Error occurred while handling a request!", t);
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, error);
        context.writeAndFlush(response).addListener(CLOSE).addListener(CLOSE_ON_FAILURE);
    }

}
