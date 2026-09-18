package com.ghostkitchen.ingestion.adapter;

/** Bad format, missing column, unreadable file — anything that should surface as a structured parse failure, not a stack trace. */
public class ReportParseException extends Exception {

    public ReportParseException(String message) {
        super(message);
    }
}
