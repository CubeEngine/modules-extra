package org.cubeengine.module.observe;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface FailureCallback {
    void fail(HttpResponseStatus status, Throwable t);
}
