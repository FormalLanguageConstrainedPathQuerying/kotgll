/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.datastreams.lifecycle;

import org.elasticsearch.cluster.metadata.DataStreamLifecycle;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ExplainIndexDataStreamLifecycleTests extends AbstractWireSerializingTestCase<ExplainIndexDataStreamLifecycle> {

    public void testGetGenerationTime() {
        long now = System.currentTimeMillis();
        {
            ExplainIndexDataStreamLifecycle explainIndexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                randomAlphaOfLengthBetween(10, 30),
                true,
                now,
                randomBoolean() ? now + TimeValue.timeValueDays(1).getMillis() : null,
                null,
                new DataStreamLifecycle(),
                randomBoolean()
                    ? new ErrorEntry(
                        System.currentTimeMillis(),
                        new NullPointerException("bad times").getMessage(),
                        System.currentTimeMillis(),
                        randomIntBetween(0, 30)
                    )
                    : null
            );
            assertThat(explainIndexDataStreamLifecycle.getGenerationTime(() -> now + 50L), is(nullValue()));
            explainIndexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                randomAlphaOfLengthBetween(10, 30),
                true,
                now,
                randomBoolean() ? now + TimeValue.timeValueDays(1).getMillis() : null,
                TimeValue.timeValueMillis(now + 100),
                new DataStreamLifecycle(),
                randomBoolean()
                    ? new ErrorEntry(
                        System.currentTimeMillis(),
                        new NullPointerException("bad times").getMessage(),
                        System.currentTimeMillis(),
                        randomIntBetween(0, 30)
                    )
                    : null
            );
            assertThat(explainIndexDataStreamLifecycle.getGenerationTime(() -> now + 500L), is(TimeValue.timeValueMillis(400)));
        }
        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                false,
                null,
                null,
                null,
                null,
                null
            );
            assertThat(indexDataStreamLifecycle.getGenerationTime(() -> now), is(nullValue()));
        }

        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                true,
                now,
                now + 80L, 
                TimeValue.timeValueMillis(now + 100L),
                new DataStreamLifecycle(),
                null
            );
            assertThat(indexDataStreamLifecycle.getGenerationTime(() -> now), is(TimeValue.ZERO));
        }
    }

    public void testGetTimeSinceIndexCreation() {
        long now = System.currentTimeMillis();
        {
            ExplainIndexDataStreamLifecycle randomIndexDataStreamLifecycleExplanation = createManagedIndexDataStreamLifecycleExplanation(
                now,
                new DataStreamLifecycle()
            );
            assertThat(
                randomIndexDataStreamLifecycleExplanation.getTimeSinceIndexCreation(() -> now + 75L),
                is(TimeValue.timeValueMillis(75))
            );
        }
        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                false,
                null,
                null,
                null,
                null,
                null
            );
            assertThat(indexDataStreamLifecycle.getTimeSinceIndexCreation(() -> now), is(nullValue()));
        }

        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                true,
                now + 80L, 
                null,
                null,
                new DataStreamLifecycle(),
                null
            );
            assertThat(indexDataStreamLifecycle.getTimeSinceIndexCreation(() -> now), is(TimeValue.ZERO));
        }
    }

    public void testGetTimeSinceRollover() {
        long now = System.currentTimeMillis();
        {
            ExplainIndexDataStreamLifecycle randomIndexDataStreamLifecycleExplanation = createManagedIndexDataStreamLifecycleExplanation(
                now,
                new DataStreamLifecycle()
            );
            if (randomIndexDataStreamLifecycleExplanation.getRolloverDate() == null) {
                assertThat(randomIndexDataStreamLifecycleExplanation.getTimeSinceRollover(() -> now + 50L), is(nullValue()));
            } else {
                assertThat(
                    randomIndexDataStreamLifecycleExplanation.getTimeSinceRollover(
                        () -> randomIndexDataStreamLifecycleExplanation.getRolloverDate() + 75L
                    ),
                    is(TimeValue.timeValueMillis(75))
                );
            }
        }
        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                false,
                null,
                null,
                null,
                null,
                null
            );
            assertThat(indexDataStreamLifecycle.getTimeSinceRollover(() -> now), is(nullValue()));
        }

        {
            ExplainIndexDataStreamLifecycle indexDataStreamLifecycle = new ExplainIndexDataStreamLifecycle(
                "my-index",
                true,
                now - 50L,
                now + 100L, 
                TimeValue.timeValueMillis(now),
                new DataStreamLifecycle(),
                null
            );
            assertThat(indexDataStreamLifecycle.getTimeSinceRollover(() -> now), is(TimeValue.ZERO));
        }
    }

    @Override
    protected Writeable.Reader<ExplainIndexDataStreamLifecycle> instanceReader() {
        return ExplainIndexDataStreamLifecycle::new;
    }

    @Override
    protected ExplainIndexDataStreamLifecycle createTestInstance() {
        return createManagedIndexDataStreamLifecycleExplanation(System.nanoTime(), randomBoolean() ? new DataStreamLifecycle() : null);
    }

    @Override
    protected ExplainIndexDataStreamLifecycle mutateInstance(ExplainIndexDataStreamLifecycle instance) throws IOException {
        return createManagedIndexDataStreamLifecycleExplanation(System.nanoTime(), randomBoolean() ? new DataStreamLifecycle() : null);
    }

    private static ExplainIndexDataStreamLifecycle createManagedIndexDataStreamLifecycleExplanation(
        long now,
        @Nullable DataStreamLifecycle lifecycle
    ) {
        return new ExplainIndexDataStreamLifecycle(
            randomAlphaOfLengthBetween(10, 30),
            true,
            now,
            randomBoolean() ? now + TimeValue.timeValueDays(1).getMillis() : null,
            TimeValue.timeValueMillis(now),
            lifecycle,
            randomBoolean()
                ? new ErrorEntry(
                    System.currentTimeMillis(),
                    new NullPointerException("bad times").getMessage(),
                    System.currentTimeMillis(),
                    randomIntBetween(0, 30)
                )
                : null
        );
    }

}
