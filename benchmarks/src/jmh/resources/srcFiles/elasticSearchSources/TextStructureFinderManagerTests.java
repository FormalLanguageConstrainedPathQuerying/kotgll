/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.textstructure.structurefinder;

import com.ibm.icu.text.CharsetMatch;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.xpack.core.textstructure.structurefinder.TextStructure;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.xpack.textstructure.structurefinder.TextStructureFinderManager.DEFAULT_LINE_MERGE_SIZE_LIMIT;
import static org.elasticsearch.xpack.textstructure.structurefinder.TextStructureOverrides.EMPTY_OVERRIDES;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class TextStructureFinderManagerTests extends TextStructureTestCase {

    private ScheduledExecutorService scheduler;
    private TextStructureFinderManager structureFinderManager;

    @Before
    public void setup() {
        scheduler = new Scheduler.SafeScheduledThreadPoolExecutor(1);
        structureFinderManager = new TextStructureFinderManager(scheduler);
    }

    @After
    public void shutdownScheduler() {
        scheduler.shutdown();
    }

    public void testFindCharsetGivenCharacterWidths() throws Exception {

        for (Charset charset : Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE)) {
            CharsetMatch charsetMatch = structureFinderManager.findCharset(
                explanation,
                new ByteArrayInputStream(TEXT_SAMPLE.getBytes(charset)),
                NOOP_TIMEOUT_CHECKER
            );
            assertEquals(charset.name(), charsetMatch.getName());
        }
    }

    public void testFindCharsetGivenRandomBinary() throws Exception {

        int size = 1000;
        byte[] binaryBytes = randomByteArrayOfLength(size);
        for (int i = 0; i < 10; ++i) {
            binaryBytes[randomIntBetween(0, size - 1)] = 0;
        }

        try {
            CharsetMatch charsetMatch = structureFinderManager.findCharset(
                explanation,
                new ByteArrayInputStream(binaryBytes),
                NOOP_TIMEOUT_CHECKER
            );
            assertThat(charsetMatch.getName(), startsWith("UTF-16"));
        } catch (IllegalArgumentException e) {
            assertEquals("Could not determine a usable character encoding for the input - could it be binary data?", e.getMessage());
        }
    }

    public void testFindCharsetGivenBinaryNearUtf16() throws Exception {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (randomBoolean()) {
            stream.write(randomAlphaOfLengthBetween(3, 4).getBytes(StandardCharsets.UTF_16LE));
        }
        for (int i = 0; i < 50; ++i) {
            stream.write(randomAlphaOfLengthBetween(5, 6).getBytes(StandardCharsets.UTF_16BE));
            stream.write(randomAlphaOfLengthBetween(5, 6).getBytes(StandardCharsets.UTF_16LE));
        }
        if (randomBoolean()) {
            stream.write(randomAlphaOfLengthBetween(3, 4).getBytes(StandardCharsets.UTF_16BE));
        }

        IllegalArgumentException e = expectThrows(
            IllegalArgumentException.class,
            () -> structureFinderManager.findCharset(explanation, new ByteArrayInputStream(stream.toByteArray()), NOOP_TIMEOUT_CHECKER)
        );

        assertEquals("Could not determine a usable character encoding for the input - could it be binary data?", e.getMessage());
        assertThat(
            explanation.toString(),
            containsString("but was rejected as the distribution of zero bytes between odd and even positions in the text is very close")
        );
    }

    public void testMakeBestStructureGivenNdJson() throws Exception {
        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                NDJSON_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                EMPTY_OVERRIDES,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(NdJsonTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenNdJsonAndDelimitedOverride() throws Exception {

        TextStructureOverrides overrides = TextStructureOverrides.builder()
            .setFormat(TextStructure.Format.DELIMITED)
            .setQuote('\'')
            .build();

        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                NDJSON_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                overrides,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(DelimitedTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenXml() throws Exception {
        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                XML_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                EMPTY_OVERRIDES,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(XmlTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenXmlAndTextOverride() throws Exception {

        TextStructureOverrides overrides = TextStructureOverrides.builder().setFormat(TextStructure.Format.SEMI_STRUCTURED_TEXT).build();

        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                XML_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                overrides,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(LogTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenCsv() throws Exception {
        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                CSV_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                EMPTY_OVERRIDES,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(DelimitedTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenCsvAndJsonOverride() {

        TextStructureOverrides overrides = TextStructureOverrides.builder().setFormat(TextStructure.Format.NDJSON).build();

        IllegalArgumentException e = expectThrows(
            IllegalArgumentException.class,
            () -> structureFinderManager.makeBestStructureFinder(
                explanation,
                CSV_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                overrides,
                NOOP_TIMEOUT_CHECKER
            )
        );

        assertEquals("Input did not match the specified format [ndjson]", e.getMessage());
    }

    public void testMakeBestStructureGivenText() throws Exception {
        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                TEXT_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                TextStructureOverrides.EMPTY_OVERRIDES,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(LogTextStructureFinder.class)
        );
    }

    public void testMakeBestStructureGivenTextAndDelimitedOverride() throws Exception {

        TextStructureOverrides overrides = TextStructureOverrides.builder()
            .setFormat(TextStructure.Format.DELIMITED)
            .setDelimiter(':')
            .build();

        assertThat(
            structureFinderManager.makeBestStructureFinder(
                explanation,
                TEXT_SAMPLE,
                StandardCharsets.UTF_8.name(),
                randomBoolean(),
                DEFAULT_LINE_MERGE_SIZE_LIMIT,
                overrides,
                NOOP_TIMEOUT_CHECKER
            ),
            instanceOf(DelimitedTextStructureFinder.class)
        );
    }

    public void testFindTextStructureTimeout() throws IOException, InterruptedException {

        int linesOfJunk = 10000;
        TimeValue timeout = new TimeValue(10, TimeUnit.MILLISECONDS);

        try (PipedOutputStream generator = new PipedOutputStream()) {

            Thread junkProducer = new Thread(() -> {
                try {
                    for (int count = 0; count < linesOfJunk; ++count) {
                        generator.write(randomAlphaOfLength(100).getBytes(StandardCharsets.UTF_8));
                        generator.write(',');
                        generator.write(randomAlphaOfLength(100).getBytes(StandardCharsets.UTF_8));
                        generator.write(',');
                        generator.write(randomAlphaOfLength(100).getBytes(StandardCharsets.UTF_8));
                        generator.write('\n');
                    }
                } catch (IOException e) {
                }
            });

            try (InputStream bigInput = new PipedInputStream(generator)) {

                junkProducer.start();

                ElasticsearchTimeoutException e = expectThrows(
                    ElasticsearchTimeoutException.class,
                    () -> structureFinderManager.findTextStructure(
                        explanation,
                        DEFAULT_LINE_MERGE_SIZE_LIMIT,
                        linesOfJunk - 1,
                        bigInput,
                        TextStructureOverrides.EMPTY_OVERRIDES,
                        timeout
                    )
                );

                assertThat(e.getMessage(), startsWith("Aborting structure analysis during ["));
                assertThat(e.getMessage(), endsWith("] as it has taken longer than the timeout of [" + timeout + "]"));
            }

            junkProducer.join(10000L);
        }
    }
}
