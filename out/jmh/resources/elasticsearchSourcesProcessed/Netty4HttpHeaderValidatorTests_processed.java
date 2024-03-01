/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.http.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AsciiString;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.http.netty4.internal.HttpValidator;
import org.elasticsearch.test.ESTestCase;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.http.netty4.Netty4HttpHeaderValidator.State.DROPPING_DATA_UNTIL_NEXT_REQUEST;
import static org.elasticsearch.http.netty4.Netty4HttpHeaderValidator.State.FORWARDING_DATA_UNTIL_NEXT_REQUEST;
import static org.elasticsearch.http.netty4.Netty4HttpHeaderValidator.State.QUEUEING_DATA;
import static org.elasticsearch.http.netty4.Netty4HttpHeaderValidator.State.WAITING_TO_START;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class Netty4HttpHeaderValidatorTests extends ESTestCase {

    private final AtomicReference<Object> header = new AtomicReference<>();
    private final AtomicReference<ActionListener<Void>> listener = new AtomicReference<>();
    private EmbeddedChannel channel;
    private Netty4HttpHeaderValidator netty4HttpHeaderValidator;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        reset();
    }

    private void reset() {
        channel = new EmbeddedChannel();
        header.set(null);
        listener.set(null);
        HttpValidator validator = (httpRequest, channel, validationCompleteListener) -> {
            header.set(httpRequest);
            listener.set(validationCompleteListener);
        };
        netty4HttpHeaderValidator = new Netty4HttpHeaderValidator(validator, new ThreadContext(Settings.EMPTY));
        channel.pipeline().addLast(netty4HttpHeaderValidator);
    }

    public void testValidationPausesAndResumesData() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request);
        channel.writeInbound(content);

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        listener.get().onResponse(null);
        channel.runPendingTasks();

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(FORWARDING_DATA_UNTIL_NEXT_REQUEST));
        assertThat(channel.readInbound(), sameInstance(request));
        assertThat(channel.readInbound(), sameInstance(content));
        assertThat(channel.readInbound(), nullValue());
        assertThat(content.refCnt(), equalTo(1));

        DefaultLastHttpContent lastContent = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(lastContent);
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertThat(channel.readInbound(), sameInstance(lastContent));
        assertThat(lastContent.refCnt(), equalTo(1));

        channel.writeInbound(request);
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
    }

    public void testContentForwardedAfterValidation() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        channel.writeInbound(request);

        DefaultHttpContent content1 = null;
        if (randomBoolean()) {
            content1 = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(content1);
        }

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        listener.get().onResponse(null);
        channel.runPendingTasks();

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(FORWARDING_DATA_UNTIL_NEXT_REQUEST));
        DefaultHttpContent content2 = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(content2);
        assertThat(channel.readInbound(), sameInstance(request));
        DefaultHttpContent content3 = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(content3);
        if (content1 != null) {
            assertThat(channel.readInbound(), sameInstance(content1));
            assertThat(content1.refCnt(), equalTo(1));
        }
        assertThat(channel.readInbound(), sameInstance(content2));
        assertThat(content2.refCnt(), equalTo(1));
        DefaultHttpContent content4 = null;
        if (randomBoolean()) {
            content4 = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(content4);
        }
        assertThat(channel.readInbound(), sameInstance(content3));
        assertThat(content3.refCnt(), equalTo(1));
        if (content4 != null) {
            assertThat(channel.readInbound(), sameInstance(content4));
            assertThat(content4.refCnt(), equalTo(1));
        }

        DefaultLastHttpContent lastContent = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(lastContent);

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertThat(channel.readInbound(), sameInstance(lastContent));
        assertThat(lastContent.refCnt(), equalTo(1));

        if (randomBoolean()) {
            channel.writeInbound(request);
            assertFalse(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        }
    }

    public void testContentDroppedAfterValidationFailure() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        channel.writeInbound(request);

        DefaultHttpContent content1 = null;
        if (randomBoolean()) {
            content1 = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(content1);
        }

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        listener.get().onFailure(new ElasticsearchException("Boom"));
        channel.runPendingTasks();

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(DROPPING_DATA_UNTIL_NEXT_REQUEST));
        DefaultHttpContent content2 = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(content2);
        assertThat(channel.readInbound(), sameInstance(request));
        DefaultHttpContent content3 = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(content3);
        if (content1 != null) {
            assertThat(channel.readInbound(), nullValue());
            assertThat(content1.refCnt(), equalTo(0));
        }
        assertThat(channel.readInbound(), nullValue()); 
        assertThat(content2.refCnt(), equalTo(0));
        DefaultHttpContent content4 = null;
        if (randomBoolean()) {
            content4 = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(content4);
        }
        assertThat(channel.readInbound(), nullValue()); 
        assertThat(content3.refCnt(), equalTo(0));
        if (content4 != null) {
            assertThat(channel.readInbound(), nullValue());
            assertThat(content4.refCnt(), equalTo(0));
        }

        assertThat(channel.readInbound(), nullValue()); 

        DefaultLastHttpContent lastContent = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(lastContent);

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertThat(channel.readInbound(), nullValue()); 
        assertThat(lastContent.refCnt(), equalTo(0));

        if (randomBoolean()) {
            channel.writeInbound(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri"));
            assertFalse(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        }
    }

    public void testValidationErrorForwardsAsDecoderErrorMessage() {
        for (Exception exception : List.of(
            new Exception("Failure"),
            new ElasticsearchException("Failure"),
            new ElasticsearchSecurityException("Failure")
        )) {
            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

            final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
            final DefaultHttpContent content = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(request);
            channel.writeInbound(content);

            assertThat(header.get(), sameInstance(request));
            assertThat(channel.readInbound(), nullValue());
            assertFalse(channel.config().isAutoRead());

            listener.get().onFailure(exception);
            channel.runPendingTasks();
            assertTrue(channel.config().isAutoRead());
            DefaultHttpRequest failed = channel.readInbound();
            assertThat(failed, sameInstance(request));
            assertThat(failed.headers().get(HttpHeaderNames.CONNECTION), nullValue());
            assertTrue(failed.decoderResult().isFailure());
            Exception cause = (Exception) failed.decoderResult().cause();
            assertThat(cause, equalTo(exception));
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(DROPPING_DATA_UNTIL_NEXT_REQUEST));

            assertThat(channel.readInbound(), nullValue());
            assertThat(content.refCnt(), equalTo(0));

            DefaultLastHttpContent lastContent = new DefaultLastHttpContent(Unpooled.buffer(4));
            channel.writeInbound(lastContent);
            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
            assertThat(channel.readInbound(), nullValue());
            assertThat(lastContent.refCnt(), equalTo(0));

            reset();
        }
    }

    public void testValidationHandlesMultipleQueuedUpMessages() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content1 = new DefaultHttpContent(Unpooled.buffer(4));
        DefaultLastHttpContent lastContent1 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request1);
        channel.writeInbound(content1);
        channel.writeInbound(lastContent1);
        final DefaultHttpRequest request2 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content2 = new DefaultHttpContent(Unpooled.buffer(4));
        DefaultLastHttpContent lastContent2 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request2);
        channel.writeInbound(content2);
        channel.writeInbound(lastContent2);

        assertThat(header.get(), sameInstance(request1));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));

        listener.get().onResponse(null);
        channel.runPendingTasks();
        assertThat(channel.readInbound(), sameInstance(request1));
        assertThat(channel.readInbound(), sameInstance(content1));
        assertThat(channel.readInbound(), sameInstance(lastContent1));
        assertThat(content1.refCnt(), equalTo(1));
        assertThat(lastContent1.refCnt(), equalTo(1));

        assertThat(header.get(), sameInstance(request2));

        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        assertThat(channel.readInbound(), nullValue());

        listener.get().onResponse(null);
        channel.runPendingTasks();
        assertThat(channel.readInbound(), sameInstance(request2));
        assertThat(channel.readInbound(), sameInstance(content2));
        assertThat(channel.readInbound(), sameInstance(lastContent2));
        assertThat(content2.refCnt(), equalTo(1));
        assertThat(lastContent2.refCnt(), equalTo(1));

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertThat(channel.readInbound(), nullValue());
    }

    public void testValidationFailureRecoversForEnqueued() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content1 = new DefaultHttpContent(Unpooled.buffer(4));
        DefaultLastHttpContent lastContent1 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request1);
        channel.writeInbound(content1);
        channel.writeInbound(lastContent1);
        final DefaultHttpRequest request2 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content2 = new DefaultHttpContent(Unpooled.buffer(4));
        DefaultLastHttpContent lastContent2 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request2);
        channel.writeInbound(content2);

        boolean finishSecondRequest = randomBoolean();
        if (finishSecondRequest) {
            channel.writeInbound(lastContent2);
        }

        assertThat(header.get(), sameInstance(request1));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        assertThat(content1.refCnt(), equalTo(2));
        assertThat(lastContent1.refCnt(), equalTo(2));
        assertThat(content2.refCnt(), equalTo(2));
        if (finishSecondRequest) {
            assertThat(lastContent2.refCnt(), equalTo(2));
        }

        Exception exception = new ElasticsearchException("Boom");
        listener.get().onFailure(exception);
        channel.runPendingTasks();

        assertThat(channel.readInbound(), sameInstance(request1));
        assertThat(request1.headers().get(HttpHeaderNames.CONNECTION), nullValue());
        assertTrue(request1.decoderResult().isFailure());
        Exception cause = (Exception) request1.decoderResult().cause();
        assertThat(cause, equalTo(exception));
        assertThat(content1.refCnt(), equalTo(0)); 
        assertThat(lastContent1.refCnt(), equalTo(0)); 
        assertThat(channel.readInbound(), nullValue());

        assertThat(header.get(), sameInstance(request2));
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        assertThat(channel.readInbound(), nullValue());

        listener.get().onResponse(null);
        channel.runPendingTasks();

        assertThat(channel.readInbound(), sameInstance(request2));
        assertThat(channel.readInbound(), sameInstance(content2));
        assertThat(content2.refCnt(), equalTo(1));

        if (finishSecondRequest == false) {
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(FORWARDING_DATA_UNTIL_NEXT_REQUEST));
            assertTrue(channel.config().isAutoRead());
            assertThat(channel.readInbound(), nullValue());
            if (randomBoolean()) {
                DefaultHttpContent content = new DefaultHttpContent(Unpooled.buffer(4));
                channel.writeInbound(content);
                assertThat(channel.readInbound(), sameInstance(content));
                assertThat(content.refCnt(), equalTo(1));
            }
            channel.writeInbound(lastContent2);
        }

        assertThat(channel.readInbound(), sameInstance(lastContent2));
        assertThat(lastContent2.refCnt(), equalTo(1));
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertTrue(channel.config().isAutoRead());
    }

    public void testValidationFailureRecoversForInbound() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        final DefaultHttpRequest request1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content1 = new DefaultHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request1);
        channel.writeInbound(content1);

        assertThat(header.get(), sameInstance(request1));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        assertThat(content1.refCnt(), equalTo(2));

        Exception exception = new ElasticsearchException("Boom");
        listener.get().onFailure(exception);
        channel.runPendingTasks();

        assertThat(channel.readInbound(), sameInstance(request1));
        assertThat(request1.headers().get(HttpHeaderNames.CONNECTION), nullValue());
        assertTrue(request1.decoderResult().isFailure());
        Exception cause = (Exception) request1.decoderResult().cause();
        assertThat(cause, equalTo(exception));
        assertThat(content1.refCnt(), equalTo(0)); 
        assertThat(channel.readInbound(), nullValue());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(DROPPING_DATA_UNTIL_NEXT_REQUEST));

        if (randomBoolean()) {
            channel.writeInbound(new DefaultHttpContent(Unpooled.buffer(4)));
        }
        DefaultLastHttpContent lastContent1 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(lastContent1);
        if (randomBoolean()) {
            assertThat(channel.readInbound(), nullValue());
        }
        assertThat(lastContent1.refCnt(), equalTo(0)); 

        final DefaultHttpRequest request2 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        DefaultHttpContent content2 = new DefaultHttpContent(Unpooled.buffer(4));
        DefaultLastHttpContent lastContent2 = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(request2);
        channel.writeInbound(content2);
        boolean finishSecondRequest = randomBoolean();
        if (finishSecondRequest) {
            channel.writeInbound(lastContent2);
        }

        assertThat(header.get(), sameInstance(request2));
        assertFalse(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        assertThat(channel.readInbound(), nullValue());

        listener.get().onResponse(null);
        channel.runPendingTasks();

        assertThat(channel.readInbound(), sameInstance(request2));
        assertThat(channel.readInbound(), sameInstance(content2));
        assertThat(content2.refCnt(), equalTo(1));

        if (finishSecondRequest == false) {
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(FORWARDING_DATA_UNTIL_NEXT_REQUEST));
            assertTrue(channel.config().isAutoRead());
            assertThat(channel.readInbound(), nullValue());
            if (randomBoolean()) {
                DefaultHttpContent content = new DefaultHttpContent(Unpooled.buffer(4));
                channel.writeInbound(content);
                assertThat(channel.readInbound(), sameInstance(content));
                assertThat(content.refCnt(), equalTo(1));
            }
            channel.writeInbound(lastContent2);
        }

        assertThat(channel.readInbound(), sameInstance(lastContent2));
        assertThat(lastContent2.refCnt(), equalTo(1));
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        assertTrue(channel.config().isAutoRead());
    }

    public void testValidationSuccessForLargeMessage() {
        assertTrue(channel.config().isAutoRead());

        final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        channel.writeInbound(request);

        int messageLength = randomIntBetween(32, 128);
        for (int i = 0; i < messageLength; ++i) {
            channel.writeInbound(new DefaultHttpContent(Unpooled.buffer(4)));
        }
        channel.writeInbound(new DefaultLastHttpContent(Unpooled.buffer(4)));
        boolean followupRequest = randomBoolean();
        if (followupRequest) {
            channel.writeInbound(request);
        }

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        listener.get().onResponse(null);
        channel.runPendingTasks();
        if (followupRequest) {
            assertFalse(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        } else {
            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        }
        assertThat(channel.readInbound(), sameInstance(request));
        for (int i = 0; i < messageLength; ++i) {
            Object content = channel.readInbound();
            assertThat(content, instanceOf(DefaultHttpContent.class));
            assertThat(((DefaultHttpContent) content).refCnt(), equalTo(1));
        }
        assertThat(channel.readInbound(), instanceOf(LastHttpContent.class));
        assertThat(channel.readInbound(), nullValue());
    }

    public void testValidationFailureForLargeMessage() {
        assertTrue(channel.config().isAutoRead());

        final DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        channel.writeInbound(request);

        int messageLength = randomIntBetween(32, 128);
        DefaultHttpContent[] messageContents = new DefaultHttpContent[messageLength];
        for (int i = 0; i < messageLength; ++i) {
            messageContents[i] = new DefaultHttpContent(Unpooled.buffer(4));
            channel.writeInbound(messageContents[i]);
        }
        DefaultLastHttpContent lastHttpContent = new DefaultLastHttpContent(Unpooled.buffer(4));
        channel.writeInbound(lastHttpContent);
        boolean followupRequest = randomBoolean();
        if (followupRequest) {
            channel.writeInbound(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri"));
        }

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        Exception exception = new ElasticsearchException("Boom");
        listener.get().onFailure(exception);
        channel.runPendingTasks();
        if (followupRequest) {
            assertFalse(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(QUEUEING_DATA));
        } else {
            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));
        }
        assertThat(channel.readInbound(), sameInstance(request));
        assertThat(request.headers().get(HttpHeaderNames.CONNECTION), nullValue());
        assertTrue(request.decoderResult().isFailure());
        Exception cause = (Exception) request.decoderResult().cause();
        assertThat(cause, equalTo(exception));
        for (int i = 0; i < messageLength; ++i) {
            assertThat(channel.readInbound(), nullValue());
            assertThat(messageContents[i].refCnt(), equalTo(0));
        }
        assertThat(channel.readInbound(), nullValue());
        assertThat(lastHttpContent.refCnt(), equalTo(0));
        assertThat(channel.readInbound(), nullValue());
    }

    public void testFullRequestValidationFailure() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        ByteBuf buf = channel.alloc().buffer();
        ByteBufUtil.copy(AsciiString.of("test full http request"), buf);
        final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri", buf);
        channel.writeInbound(request);

        assertThat(header.get(), sameInstance(request));
        assertThat(channel.readInbound(), nullValue());
        assertFalse(channel.config().isAutoRead());

        Exception exception = new ElasticsearchException("Boom");
        listener.get().onFailure(exception);
        channel.runPendingTasks();

        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        DefaultFullHttpRequest throughRequest = channel.readInbound();
        assertThat(throughRequest, not(sameInstance(request)));
        assertTrue(throughRequest.decoderResult().isFailure());
        assertThat(new String(ByteBufUtil.getBytes(throughRequest.content()), StandardCharsets.UTF_8), is(""));
        assertThat(buf.refCnt(), is(0));
        Exception cause = (Exception) throughRequest.decoderResult().cause();
        assertThat(cause, equalTo(exception));
    }

    public void testFullRequestValidationSuccess() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        ByteBuf buf = channel.alloc().buffer();
        try {
            ByteBufUtil.copy(AsciiString.of("test full http request"), buf);
            final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri", buf);
            channel.writeInbound(request);

            assertThat(header.get(), sameInstance(request));
            assertThat(channel.readInbound(), nullValue());
            assertFalse(channel.config().isAutoRead());

            listener.get().onResponse(null);
            channel.runPendingTasks();

            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

            DefaultFullHttpRequest throughRequest = channel.readInbound();
            assertThat(throughRequest, sameInstance(request));
            assertFalse(throughRequest.decoderResult().isFailure());
            assertThat(new String(ByteBufUtil.getBytes(throughRequest.content()), StandardCharsets.UTF_8), is("test full http request"));
            assertThat(buf.refCnt(), is(1));
            assertThat(throughRequest.decoderResult().cause(), nullValue());
        } finally {
            buf.release();
        }
    }

    public void testFullRequestWithDecoderException() {
        assertTrue(channel.config().isAutoRead());
        assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

        ByteBuf buf = channel.alloc().buffer();
        try {
            ByteBufUtil.copy(AsciiString.of("test full http request"), buf);
            final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri", buf);
            Exception cause = new ElasticsearchException("Boom");
            request.setDecoderResult(DecoderResult.failure(cause));
            channel.writeInbound(request);

            assertThat(header.get(), nullValue());
            assertThat(listener.get(), nullValue());
            assertTrue(channel.config().isAutoRead());
            assertThat(netty4HttpHeaderValidator.getState(), equalTo(WAITING_TO_START));

            DefaultFullHttpRequest throughRequest = channel.readInbound();
            assertThat(throughRequest, sameInstance(request));
            assertTrue(throughRequest.decoderResult().isFailure());
            assertThat(throughRequest.decoderResult().cause(), equalTo(cause));
            assertThat(new String(ByteBufUtil.getBytes(throughRequest.content()), StandardCharsets.UTF_8), is("test full http request"));
            assertThat(buf.refCnt(), is(1));
        } finally {
            buf.release();
        }
    }
}
