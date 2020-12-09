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

import esa.httpclient.core.Context;
import esa.httpclient.core.ContextImpl;
import esa.httpclient.core.FileRequest;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static esa.httpclient.core.ContextNames.EXPECT_CONTINUE_CALLBACK;
import static esa.httpclient.core.ContextNames.EXPECT_CONTINUE_ENABLED;
import static org.assertj.core.api.BDDAssertions.then;

class FileWriterTest extends Http2ConnectionHelper {

    ////////*********************************HTTP1 FILE WRITER**************************************////////

    @Test
    void testWriteAndFlushHttp1() throws IOException {
        final FileWriter writer = FileWriter.singleton();
        final EmbeddedChannel channel = new EmbeddedChannel();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                final byte[] data = new byte[1024 * 1024];
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            final Context ctx = new ContextImpl();
            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    HttpVersion.HTTP_1_1,
                    false);
            channel.flush();

            HttpRequest req = channel.readOutbound();
            then(req.method()).isSameAs(HttpMethod.POST);
            then(req.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(req.headers().get(HttpHeaderNames.CONTENT_TYPE)
                    .contentEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM)).isTrue();
            then(req.headers().get(HttpHeaderNames.HOST)).isEqualTo("127.0.0.1");
            then(req.protocolVersion()).isSameAs(HttpVersion.HTTP_1_1);

            DefaultFileRegion fileRegion = channel.readOutbound();
            then(fileRegion).isNotNull();

            LastHttpContent last = channel.readOutbound();
            then(last).isSameAs(LastHttpContent.EMPTY_LAST_CONTENT);
            then(end.isDone()).isTrue();
            then(end.isSuccess()).isTrue();
        } finally {
            file.delete();
        }
    }

    @Test
    void test100ExpectContinue1() throws IOException {
        final FileWriter writer = FileWriter.singleton();
        final EmbeddedChannel channel = new EmbeddedChannel();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                final byte[] data = new byte[1024 * 1024];
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            final Context ctx = new ContextImpl();
            ctx.setAttr(EXPECT_CONTINUE_ENABLED, true);

            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    HttpVersion.HTTP_1_1,
                    false);
            channel.flush();

            HttpRequest req = channel.readOutbound();
            then(req.method()).isSameAs(HttpMethod.POST);
            then(req.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(req.headers().get(HttpHeaderNames.CONTENT_TYPE)
                    .contentEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM)).isTrue();
            then(req.headers().get(HttpHeaderNames.HOST)).isEqualTo("127.0.0.1");
            then(req.protocolVersion()).isSameAs(HttpVersion.HTTP_1_1);
            then(end.isDone()).isFalse();

            DefaultFileRegion fileRegion = channel.readOutbound();
            then(fileRegion).isNull();

            // continue write
            ((Runnable) ctx.removeUncheckedAttr(EXPECT_CONTINUE_CALLBACK)).run();
            fileRegion = channel.readOutbound();
            then(fileRegion).isNotNull();

            LastHttpContent last = channel.readOutbound();
            then(last).isSameAs(LastHttpContent.EMPTY_LAST_CONTENT);
            then(end.isDone()).isTrue();
            then(end.isSuccess()).isTrue();
        } finally {
            file.delete();
        }
    }

    @Test
    void testWriteContent1Error() throws Exception {
        final FileWriter writer = FileWriter.singleton();
        final EmbeddedChannel channel = new EmbeddedChannel();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                final byte[] data = new byte[1024 * 1024];
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            file.delete();

            final Context ctx = new ContextImpl();
            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    HttpVersion.HTTP_1_1,
                    false);
            channel.flush();

            HttpRequest req = channel.readOutbound();
            then(req.method()).isSameAs(HttpMethod.POST);
            then(req.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(req.headers().get(HttpHeaderNames.CONTENT_TYPE)
                    .contentEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM)).isTrue();
            then(req.headers().get(HttpHeaderNames.HOST)).isEqualTo("127.0.0.1");
            then(req.protocolVersion()).isSameAs(HttpVersion.HTTP_1_1);

            DefaultFileRegion fileRegion = channel.readOutbound();
            then(fileRegion).isNull();

            then(end.isSuccess()).isFalse();
            then(end.cause()).isInstanceOf(FileNotFoundException.class);
        } finally {
            file.delete();
        }
    }

    ////////*********************************HTTP2 FILE WRITER**************************************////////

    @Test
    void testWriteAndFlush2() throws Exception {
        setUp();
        final FileWriter writer = FileWriter.singleton();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            final byte[] data = new byte[1024 * 1024];
            try (FileOutputStream out = new FileOutputStream(file)) {
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            final Context ctx = new ContextImpl();
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), STREAM_ID);

            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    null,
                    true);
            channel.flush();
            // Ignore preface
            channel.readOutbound();

            Helper.HeaderFrame header = channel.readOutbound();
            then(header).isNotNull();
            then(header.streamId).isEqualTo(STREAM_ID);
            then(header.headers.method()).isEqualTo(HttpMethod.POST.asciiName());
            then(header.headers.getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(header.headers.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(HttpHeaderValues.APPLICATION_OCTET_STREAM);
            then(header.headers.authority().toString()).isEqualTo("127.0.0.1");

            Helper.DataFrame content = null;
            for (int i = 0; i < data.length / 8192; i++) {
                content = channel.readOutbound();
                then(content.data.readableBytes()).isEqualTo(8192);
            }
            then(content.endStream).isTrue();
            then(end.isDone()).isTrue();
        } finally {
            file.delete();
        }
    }

    @Test
    void test100ExpectContinue2() throws Exception {
        setUp();
        final FileWriter writer = FileWriter.singleton();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            final byte[] data = new byte[1024 * 1024];
            try (FileOutputStream out = new FileOutputStream(file)) {
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            final Context ctx = new ContextImpl();
            ctx.setAttr(EXPECT_CONTINUE_ENABLED, true);

            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), STREAM_ID);

            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    null,
                    true);
            channel.flush();
            // Ignore preface
            channel.readOutbound();

            Helper.HeaderFrame header = channel.readOutbound();
            then(header).isNotNull();
            then(header.streamId).isEqualTo(STREAM_ID);
            then(header.headers.method()).isEqualTo(HttpMethod.POST.asciiName());
            then(header.headers.getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(header.headers.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(HttpHeaderValues.APPLICATION_OCTET_STREAM);
            then(header.headers.authority().toString()).isEqualTo("127.0.0.1");

            Object obj = channel.readOutbound();
            then(obj).isNull();

            then(end.isDone()).isFalse();
            ((Runnable) ctx.removeUncheckedAttr(EXPECT_CONTINUE_CALLBACK)).run();

            Helper.DataFrame content = null;
            for (int i = 0; i < data.length / 8192; i++) {
                content = channel.readOutbound();
                then(content.data.readableBytes()).isEqualTo(8192);
            }
            then(content.endStream).isTrue();
            then(end.isDone()).isTrue();
        } finally {
            file.delete();
        }
    }

    @Test
    void testWriteContent2Error() throws Exception {
        setUp();
        final FileWriter writer = FileWriter.singleton();

        final File file = File.createTempFile("httpclient-", ".tmp");
        file.deleteOnExit();

        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                final byte[] data = new byte[1024 * 1024];
                ThreadLocalRandom.current().nextBytes(data);
                out.write(data);
            }

            final esa.httpclient.core.FileRequest request =
                    (FileRequest) esa.httpclient.core.HttpRequest.post("http://127.0.0.1/abc")
                            .file(file).build();
            file.delete();

            final Context ctx = new ContextImpl();
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), STREAM_ID);

            final ChannelFuture end = writer.writeAndFlush(request,
                    channel,
                    ctx,
                    false,
                    null,
                    true);
            channel.flush();
            // Ignore preface
            channel.readOutbound();

            Helper.HeaderFrame header = channel.readOutbound();
            then(header).isNotNull();
            then(header.streamId).isEqualTo(STREAM_ID);
            then(header.headers.method()).isEqualTo(HttpMethod.POST.asciiName());
            then(header.headers.getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(file.length());
            then(header.headers.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(HttpHeaderValues.APPLICATION_OCTET_STREAM);
            then(header.headers.authority().toString()).isEqualTo("127.0.0.1");

            Object content = channel.readOutbound();
            then(content).isNull();
            then(end.isDone()).isTrue();
            then(end.isSuccess()).isFalse();
            then(end.cause()).isInstanceOf(IOException.class);
        } finally {
            file.delete();
        }
    }

}
