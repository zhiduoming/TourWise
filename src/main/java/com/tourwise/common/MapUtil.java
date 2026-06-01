package com.tourwise.common;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtil {
    private MapUtil() {
    }

    public static Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> result.put(toCamel(key), format(value)));
        return result;
    }

    public static Object format(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        if (value instanceof Time time) {
            return time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value;
    }

    public static String toCamel(String text) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : text.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
