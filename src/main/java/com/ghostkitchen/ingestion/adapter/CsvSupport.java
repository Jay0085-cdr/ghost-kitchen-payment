package com.ghostkitchen.ingestion.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Shared row-reading and field-parsing helpers for every CSV-based adapter/parser. */
public final class CsvSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private CsvSupport() {
    }

    /** Header names are lower-cased and trimmed so column matching is case-insensitive. */
    public static List<Map<String, String>> readAsMaps(byte[] content) throws ReportParseException {
        try (CSVReaderHeaderAware reader =
                     new CSVReaderHeaderAware(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            List<Map<String, String>> rows = new ArrayList<>();
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rows.add(normalizeKeys(row));
            }
            return rows;
        } catch (IOException | CsvValidationException e) {
            throw new ReportParseException("Could not parse CSV file: " + e.getMessage());
        }
    }

    private static Map<String, String> normalizeKeys(Map<String, String> row) {
        Map<String, String> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key == null ? "" : key.trim().toLowerCase(Locale.ROOT), value));
        return normalized;
    }

    public static Optional<String> findColumn(Map<String, String> row, List<String> aliases) {
        for (String alias : aliases) {
            String value = row.get(alias);
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    public static String requireColumn(Map<String, String> row, List<String> aliases, int lineNumber, String fieldLabel)
            throws ReportParseException {
        return findColumn(row, aliases)
                .orElseThrow(() -> new ReportParseException(
                        "Line " + lineNumber + ": missing required column '" + fieldLabel + "' (expected one of " + aliases + ")"));
    }

    public static LocalDate parseDate(String value, int lineNumber) throws ReportParseException {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ReportParseException("Line " + lineNumber + ": invalid date '" + value + "', expected YYYY-MM-DD");
        }
    }

    public static BigDecimal parseAmount(String value, int lineNumber) throws ReportParseException {
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ReportParseException("Line " + lineNumber + ": invalid amount '" + value + "'");
        }
    }

    public static BigDecimal optionalAmount(Map<String, String> row, List<String> aliases, int lineNumber) throws ReportParseException {
        Optional<String> value = findColumn(row, aliases);
        return value.isPresent() ? parseAmount(value.get(), lineNumber) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static String toJson(Map<String, String> row) {
        try {
            return MAPPER.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
