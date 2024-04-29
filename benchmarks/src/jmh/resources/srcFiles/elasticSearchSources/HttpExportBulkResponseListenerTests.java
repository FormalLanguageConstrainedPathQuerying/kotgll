/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.core.Strings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContent;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParser.Token;
import org.elasticsearch.xcontent.XContentType;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HttpExportBulkResponseListener}.
 */
public class HttpExportBulkResponseListenerTests extends ESTestCase {

    public void testOnSuccess() throws IOException {
        final Response response = mock(Response.class);
        final StringEntity entity = new StringEntity("{\"took\":5,\"errors\":false}", ContentType.APPLICATION_JSON);

        when(response.getEntity()).thenReturn(entity);

        new WarningsHttpExporterBulkResponseListener().onSuccess(response);
    }

    public void testOnSuccessParsing() throws IOException {
        final Response response = mock(Response.class);
        final XContent xContent = mock(XContent.class);
        final XContentParser parser = mock(XContentParser.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = mock(InputStream.class);

        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);
        when(xContent.createParser(Mockito.any(NamedXContentRegistry.class), Mockito.any(DeprecationHandler.class), Mockito.eq(stream)))
            .thenReturn(parser);

        when(parser.nextToken()).thenReturn(
            Token.START_OBJECT,
            Token.FIELD_NAME,
            Token.VALUE_NUMBER,
            Token.FIELD_NAME,
            Token.VALUE_BOOLEAN
        );
        when(parser.currentName()).thenReturn("took", "errors");
        when(parser.booleanValue()).thenReturn(false);

        new HttpExportBulkResponseListener(xContent).onSuccess(response);

        verify(parser, times(5)).nextToken();
        verify(parser, times(2)).currentName();
        verify(parser).booleanValue();
    }

    public void testOnSuccessWithInnerErrors() {
        final String[] expectedErrors = new String[] { randomAlphaOfLengthBetween(4, 10), randomAlphaOfLengthBetween(5, 9) };
        final AtomicInteger counter = new AtomicInteger(0);
        final Response response = mock(Response.class);
        final StringEntity entity = new StringEntity(Strings.format("""
            {
              "took": 4,
              "errors": true,
              "items": [
                {
                  "index": {
                    "_index": ".monitoring-data-2",
                    "_type": "node",
                    "_id": "123"
                  }
                },
                {
                  "index": {
                    "_index": ".monitoring-data-2",
                    "_type": "node",
                    "_id": "456",
                    "error": "%s"
                  }
                },
                {
                  "index": {
                    "_index": ".monitoring-data-2",
                    "_type": "node",
                    "_id": "789"
                  }
                },
                {
                  "index": {
                    "_index": ".monitoring-data-2",
                    "_type": "node",
                    "_id": "012",
                    "error": "%s"
                  }
                }
              ]
            }""", expectedErrors[0], expectedErrors[1]), ContentType.APPLICATION_JSON);

        when(response.getEntity()).thenReturn(entity);

        new WarningsHttpExporterBulkResponseListener() {
            @Override
            void onItemError(final String text) {
                assertEquals(expectedErrors[counter.getAndIncrement()], text);
            }
        }.onSuccess(response);

        assertEquals(expectedErrors.length, counter.get());
    }

    public void testOnSuccessParsingWithInnerErrors() throws IOException {
        final Response response = mock(Response.class);
        final XContent xContent = mock(XContent.class);
        final XContentParser parser = mock(XContentParser.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = mock(InputStream.class);

        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);
        when(xContent.createParser(Mockito.any(NamedXContentRegistry.class), Mockito.any(DeprecationHandler.class), Mockito.eq(stream)))
            .thenReturn(parser);

        when(parser.nextToken()).thenReturn(               
            Token.START_OBJECT,                            
            Token.FIELD_NAME, Token.VALUE_NUMBER,          
            Token.FIELD_NAME, Token.VALUE_BOOLEAN,         
            Token.FIELD_NAME, Token.START_ARRAY,           
                Token.START_OBJECT,                        
                    Token.FIELD_NAME, Token.START_OBJECT,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                Token.END_OBJECT,                          
                Token.START_OBJECT,                        
                    Token.FIELD_NAME, Token.START_OBJECT,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                    Token.FIELD_NAME, Token.VALUE_STRING,  
                Token.END_OBJECT,                          
            Token.END_ARRAY);                              
        when(parser.currentName()).thenReturn(
            "took",
            "errors",
            "items",
            "index",
            "_index",
            "_type",
            "_id",
            "index",
            "_index",
            "_type",
            "_id",
            "error"
        );
        when(parser.booleanValue()).thenReturn(true);
        when(parser.text()).thenReturn("this is the error");

        new HttpExportBulkResponseListener(xContent).onSuccess(response);

        verify(parser, times(30)).nextToken();
        verify(parser, times(12)).currentName();
        verify(parser).booleanValue();
        verify(parser).text();
    }

    public void testOnSuccessMalformed() {
        final AtomicInteger counter = new AtomicInteger(0);
        final Response response = mock(Response.class);

        if (randomBoolean()) {
            when(response.getEntity()).thenReturn(new StringEntity("{", ContentType.APPLICATION_JSON));
        }

        new WarningsHttpExporterBulkResponseListener() {
            @Override
            void onError(final String msg, final Throwable cause) {
                counter.getAndIncrement();
            }
        }.onSuccess(response);

        assertEquals(1, counter.get());
    }

    public void testOnFailure() {
        final Exception exception = randomBoolean() ? new Exception() : new RuntimeException();

        new WarningsHttpExporterBulkResponseListener() {
            @Override
            void onError(final String msg, final Throwable cause) {
                assertSame(exception, cause);
            }
        }.onFailure(exception);
    }

    private static class WarningsHttpExporterBulkResponseListener extends HttpExportBulkResponseListener {

        WarningsHttpExporterBulkResponseListener() {
            super(XContentType.JSON.xContent());
        }

        @Override
        void onItemError(final String msg) {
            fail("There should be no errors within the response!");
        }

        @Override
        void onError(final String msg, final Throwable cause) {
            super.onError(msg, cause); 

            fail("There should be no errors!");
        }

    }

}
