package cn.hongt.monitor.server.common.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilsTest {

    @Test
    void getDateByHour_shouldNotWriteToStdout() throws Exception {
        Date baseTime = DateUtils.stringToDate("2026-04-14 10:00:00", DateUtils.dateType5);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8.name()));

            Date actual = DateUtils.getDateByHour(baseTime, 2);

            assertEquals("2026-04-14 12", DateUtils.dateToString(actual, DateUtils.dateType2));
            assertEquals("", outputStream.toString(StandardCharsets.UTF_8.name()).trim());
        } finally {
            System.setOut(originalOut);
        }
    }
}
