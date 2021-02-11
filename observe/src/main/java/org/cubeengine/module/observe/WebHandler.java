package org.cubeengine.module.observe;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public interface WebHandler {
    void handleRequest(SuccessCallback success, FailureCallback failure, FullHttpRequest request, QueryStringDecoder queryStringDecoder);
}
