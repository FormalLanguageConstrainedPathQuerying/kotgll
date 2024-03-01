/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.expression.function.scalar.datetime;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.xpack.sql.AbstractSqlWireSerializingTestCase;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.NonIsoDateTimeProcessor.NonIsoDateTimeExtractor;

import java.time.ZoneId;

import static org.elasticsearch.xpack.sql.expression.function.scalar.datetime.DateTimeTestUtils.dateTime;
import static org.elasticsearch.xpack.sql.util.DateUtils.UTC;

public class NonIsoDateTimeProcessorTests extends AbstractSqlWireSerializingTestCase<NonIsoDateTimeProcessor> {

    public static NonIsoDateTimeProcessor randomNonISODateTimeProcessor() {
        return new NonIsoDateTimeProcessor(randomFrom(NonIsoDateTimeExtractor.values()), UTC);
    }

    @Override
    protected NonIsoDateTimeProcessor createTestInstance() {
        return randomNonISODateTimeProcessor();
    }

    @Override
    protected Reader<NonIsoDateTimeProcessor> instanceReader() {
        return NonIsoDateTimeProcessor::new;
    }

    @Override
    protected NonIsoDateTimeProcessor mutateInstance(NonIsoDateTimeProcessor instance) {
        NonIsoDateTimeExtractor replaced = randomValueOtherThan(instance.extractor(), () -> randomFrom(NonIsoDateTimeExtractor.values()));
        return new NonIsoDateTimeProcessor(replaced, UTC);
    }

    @Override
    protected ZoneId instanceZoneId(NonIsoDateTimeProcessor instance) {
        return instance.zoneId();
    }

    public void testNonISOWeekOfYearInUTC() {
        NonIsoDateTimeProcessor proc = new NonIsoDateTimeProcessor(NonIsoDateTimeExtractor.WEEK_OF_YEAR, UTC);
        assertEquals(2, proc.process(dateTime(568372930000L)));  
        assertEquals(6, proc.process(dateTime(981278530000L)));  
        assertEquals(7, proc.process(dateTime(224241730000L)));  

        assertEquals(12, proc.process(dateTime(132744130000L))); 
        assertEquals(17, proc.process(dateTime(230376130000L))); 
        assertEquals(17, proc.process(dateTime(766833730000L))); 
        assertEquals(29, proc.process(dateTime(79780930000L)));  
        assertEquals(33, proc.process(dateTime(902913730000L))); 

        assertEquals(2, proc.process(dateTime(1988, 1, 5, 0, 0, 0, 0)));
        assertEquals(6, proc.process(dateTime(2001, 2, 4, 0, 0, 0, 0)));
        assertEquals(7, proc.process(dateTime(1977, 2, 8, 0, 0, 0, 0)));
        assertEquals(12, proc.process(dateTime(1974, 3, 17, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(1977, 4, 20, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(1994, 4, 20, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(2002, 4, 27, 0, 0, 0, 0)));
        assertEquals(18, proc.process(dateTime(1974, 5, 3, 0, 0, 0, 0)));
        assertEquals(22, proc.process(dateTime(1997, 5, 30, 0, 0, 0, 0)));
        assertEquals(23, proc.process(dateTime(1995, 6, 4, 0, 0, 0, 0)));
        assertEquals(29, proc.process(dateTime(1972, 7, 12, 0, 0, 0, 0)));
        assertEquals(30, proc.process(dateTime(1980, 7, 26, 0, 0, 0, 0)));
        assertEquals(33, proc.process(dateTime(1998, 8, 12, 0, 0, 0, 0)));
        assertEquals(36, proc.process(dateTime(1995, 9, 3, 0, 0, 0, 0)));
        assertEquals(37, proc.process(dateTime(1976, 9, 9, 0, 0, 0, 0)));
        assertEquals(38, proc.process(dateTime(1997, 9, 19, 0, 0, 0, 0)));
        assertEquals(45, proc.process(dateTime(1980, 11, 7, 0, 0, 0, 0)));
        assertEquals(1, proc.process(dateTime(2005, 1, 1, 0, 0, 0, 0)));
        assertEquals(53, proc.process(dateTime(2007, 12, 31, 0, 0, 0, 0)));
        assertEquals(53, proc.process(dateTime(2019, 12, 31, 20, 22, 33, 987654321)));
    }

    public void testNonISOWeekOfYearInNonUTCTimeZone() {
        NonIsoDateTimeProcessor proc = new NonIsoDateTimeProcessor(NonIsoDateTimeExtractor.WEEK_OF_YEAR, ZoneId.of("GMT-10:00"));
        assertEquals(2, proc.process(dateTime(568372930000L)));
        assertEquals(5, proc.process(dateTime(981278530000L)));
        assertEquals(7, proc.process(dateTime(224241730000L)));

        assertEquals(11, proc.process(dateTime(132744130000L)));
        assertEquals(17, proc.process(dateTime(230376130000L)));
        assertEquals(17, proc.process(dateTime(766833730000L)));
        assertEquals(29, proc.process(dateTime(79780930000L)));
        assertEquals(33, proc.process(dateTime(902913730000L)));

        assertEquals(2, proc.process(dateTime(1988, 1, 5, 0, 0, 0, 0)));
        assertEquals(5, proc.process(dateTime(2001, 2, 4, 0, 0, 0, 0)));
        assertEquals(7, proc.process(dateTime(1977, 2, 8, 0, 0, 0, 0)));
        assertEquals(11, proc.process(dateTime(1974, 3, 17, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(1977, 4, 20, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(1994, 4, 20, 0, 0, 0, 0)));
        assertEquals(17, proc.process(dateTime(2002, 4, 27, 0, 0, 0, 0)));
        assertEquals(18, proc.process(dateTime(1974, 5, 3, 0, 0, 0, 0)));
        assertEquals(22, proc.process(dateTime(1997, 5, 30, 0, 0, 0, 0)));
        assertEquals(22, proc.process(dateTime(1995, 6, 4, 0, 0, 0, 0)));
        assertEquals(29, proc.process(dateTime(1972, 7, 12, 0, 0, 0, 0)));
        assertEquals(30, proc.process(dateTime(1980, 7, 26, 0, 0, 0, 0)));
        assertEquals(33, proc.process(dateTime(1998, 8, 12, 0, 0, 0, 0)));
        assertEquals(35, proc.process(dateTime(1995, 9, 3, 0, 0, 0, 0)));
        assertEquals(37, proc.process(dateTime(1976, 9, 9, 0, 0, 0, 0)));
        assertEquals(38, proc.process(dateTime(1997, 9, 19, 0, 0, 0, 0)));
        assertEquals(45, proc.process(dateTime(1980, 11, 7, 0, 0, 0, 0)));
        assertEquals(53, proc.process(dateTime(2005, 1, 1, 0, 0, 0, 0)));
        assertEquals(53, proc.process(dateTime(2007, 12, 31, 0, 0, 0, 0)));
        assertEquals(53, proc.process(dateTime(2019, 12, 31, 20, 22, 33, 987654321)));
    }

    public void testNonISODayOfWeekInUTC() {
        NonIsoDateTimeProcessor proc = new NonIsoDateTimeProcessor(NonIsoDateTimeExtractor.DAY_OF_WEEK, UTC);
        assertEquals(3, proc.process(dateTime(568372930000L))); 
        assertEquals(1, proc.process(dateTime(981278530000L))); 
        assertEquals(3, proc.process(dateTime(224241730000L))); 

        assertEquals(1, proc.process(dateTime(132744130000L))); 
        assertEquals(4, proc.process(dateTime(230376130000L))); 
        assertEquals(4, proc.process(dateTime(766833730000L))); 
        assertEquals(7, proc.process(dateTime(333451330000L))); 
        assertEquals(6, proc.process(dateTime(874660930000L))); 
    }

    public void testNonISODayOfWeekInNonUTCTimeZone() {
        NonIsoDateTimeProcessor proc = new NonIsoDateTimeProcessor(NonIsoDateTimeExtractor.DAY_OF_WEEK, ZoneId.of("GMT-10:00"));
        assertEquals(2, proc.process(dateTime(568372930000L)));
        assertEquals(7, proc.process(dateTime(981278530000L)));
        assertEquals(2, proc.process(dateTime(224241730000L)));

        assertEquals(7, proc.process(dateTime(132744130000L)));
        assertEquals(3, proc.process(dateTime(230376130000L)));
        assertEquals(3, proc.process(dateTime(766833730000L)));
        assertEquals(6, proc.process(dateTime(333451330000L)));
        assertEquals(5, proc.process(dateTime(874660930000L)));
    }
}
