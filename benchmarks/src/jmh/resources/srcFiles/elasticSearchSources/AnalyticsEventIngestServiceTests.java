/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.application.analytics;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.application.analytics.action.PostAnalyticsEventAction;
import org.elasticsearch.xpack.application.analytics.ingest.AnalyticsEventEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnalyticsEventIngestServiceTests extends ESTestCase {
    public void testAddEventWhenCollectionExists() {
        PostAnalyticsEventAction.Request request = mock(PostAnalyticsEventAction.Request.class);

        @SuppressWarnings("unchecked")
        ActionListener<PostAnalyticsEventAction.Response> listener = mock(ActionListener.class);

        AnalyticsEventEmitter eventEmitterMock = mock(AnalyticsEventEmitter.class);

        AnalyticsEventIngestService eventIngestService = new AnalyticsEventIngestService(
            eventEmitterMock,
            mock(AnalyticsCollectionResolver.class)
        );

        eventIngestService.addEvent(request, listener);

        verify(listener, never()).onFailure(any());

        verify(eventEmitterMock).emitEvent(request, listener);
    }

    public void testAddEventWhenCollectionDoesNotExists() {
        PostAnalyticsEventAction.Request request = mock(PostAnalyticsEventAction.Request.class);
        when(request.eventCollectionName()).thenReturn(randomIdentifier());

        AnalyticsEventEmitter eventEmitterMock = mock(AnalyticsEventEmitter.class);

        AnalyticsCollectionResolver analyticsCollectionResolver = mock(AnalyticsCollectionResolver.class);
        when(analyticsCollectionResolver.collection(eq(request.eventCollectionName()))).thenThrow(ResourceNotFoundException.class);

        AnalyticsEventIngestService eventIngestService = new AnalyticsEventIngestService(eventEmitterMock, analyticsCollectionResolver);

        @SuppressWarnings("unchecked")
        ActionListener<PostAnalyticsEventAction.Response> listener = mock(ActionListener.class);

        eventIngestService.addEvent(request, listener);

        verify(listener, never()).onResponse(any());
        verify(listener).onFailure(any(ResourceNotFoundException.class));

        verify(eventEmitterMock, never()).emitEvent(any(), any());
    }
}
