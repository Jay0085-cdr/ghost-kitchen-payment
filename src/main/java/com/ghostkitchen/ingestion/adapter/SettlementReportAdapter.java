package com.ghostkitchen.ingestion.adapter;

import java.util.List;

/**
 * Each platform implements this. Nothing downstream of parse() — ingestion, reconciliation,
 * matching — is ever aware of platform-specific fields; they operate only on ParsedTransaction /
 * platform_transaction. Adding a new platform is one new adapter + one registry entry.
 */
public interface SettlementReportAdapter {

    /** Matches platform.code (e.g. "DIRECT") — not the Platform entity, so an adapter never needs DB access to identify itself. */
    String supportedPlatformCode();

    List<ParsedTransaction> parse(RawReportFile file) throws ReportParseException;
}
