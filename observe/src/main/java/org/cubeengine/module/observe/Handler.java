package org.cubeengine.module.observe;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public interface Handler {
    void handle(SuccessCallback success, FailureCallback failure, FullHttpRequest request, QueryStringDecoder queryStringDecoder);
}
