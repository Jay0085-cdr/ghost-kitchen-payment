package com.ghostkitchen.ingestion.adapter;

public record RawReportFile(String fileName, byte[] content, String contentType) {
}
