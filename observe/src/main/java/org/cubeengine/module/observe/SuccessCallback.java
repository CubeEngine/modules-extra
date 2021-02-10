package org.cubeengine.module.observe;

import io.netty.handler.codec.http.FullHttpResponse;

public interface SuccessCallback {
    void succeed(FullHttpResponse response);
}
