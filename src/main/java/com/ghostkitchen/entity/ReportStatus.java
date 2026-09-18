package com.ghostkitchen.entity;

/** Shared by settlement_report and bank_statement, which go through the same upload -> parse flow. */
public enum ReportStatus {
    PENDING,
    PARSED,
    FAILED
}
