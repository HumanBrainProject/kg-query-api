package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.junit.Test;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReconciliationTest {

    @Test
    public void testDateParsing() throws ParseException {
        LocalDateTime parse = LocalDateTime.parse("2018-11-08T07:15:11.289Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
        System.out.println(parse);
    }


}