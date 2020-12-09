/*
 * Copyright 2020 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esa.httpclient.core.netty;

import esa.commons.http.HttpHeaders;
import esa.commons.netty.core.Buffer;
import esa.commons.netty.core.BufferImpl;
import esa.commons.netty.core.Buffers;
import esa.httpclient.core.Context;
import esa.httpclient.core.Handle;
import esa.httpclient.core.HttpRequest;
import esa.httpclient.core.HttpResponse;
import esa.httpclient.core.Listener;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static esa.httpclient.core.netty.Utils.tryRelease;

public class DefaultHandle extends NettyHandle {

    private static final int MAX_COMPOSITE_BUFFER_COMPONENTS = 1024;

    private CompositeByteBuf body;

    DefaultHandle(HttpRequest request,
                  Context ctx,
                  Listener listener,
                  CompletableFuture<HttpResponse> response,
                  ByteBufAllocator alloc) {
        super(request, ctx, listener, response);

        this.data = (d) -> {
            if (d.isReadable()) {
                if (body == null) {
                    body = alloc.compositeBuffer(MAX_COMPOSITE_BUFFER_COMPONENTS);
                }
                body.addComponent(true, d.getByteBuf().retain());
            }
        };

        this.end = (v) -> {
            if (body == null) {
                super.underlying.body(Buffers.EMPTY_BUFFER);
            } else {
                super.underlying.body(new BufferImpl(Unpooled.copiedBuffer(body)));
            }
        };

        this.trailers = (trailers) -> trailers().add(trailers);
    }

    @Override
    public void onEnd() {
        super.onEnd();
        tryRelease(body);
    }

    @Override
    public void onError(Throwable cause) {
        try {
            super.onError(cause);
        } finally {
            tryRelease(body);
        }
    }

    @Override
    public Handle onStart(Consumer<Void> h) {
        // Do nothing
        return this;
    }

    @Override
    public DefaultHandle onData(Consumer<Buffer> h) {
        // Do nothing
        return this;
    }

    @Override
    public DefaultHandle onTrailer(Consumer<HttpHeaders> h) {
        // Do nothing
        return this;
    }

    @Override
    public DefaultHandle onEnd(Consumer<Void> h) {
        // Do nothing
        return this;
    }

    @Override
    public DefaultHandle onError(Consumer<Throwable> h) {
        // Do nothing
        return this;
    }
}
