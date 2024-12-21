package com.report.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    // Method to convert LocalDate to String based on a given format
    public static String getLocalDateToString(LocalDate date, String format) {

       // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }
}
