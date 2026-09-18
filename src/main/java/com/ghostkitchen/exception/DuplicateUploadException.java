package com.ghostkitchen.exception;

import java.util.UUID;

/** Idempotent-upload guard (ARCHITECTURE.md Section 9): same file content re-uploaded for the same scope. */
public class DuplicateUploadException extends RuntimeException {

    public DuplicateUploadException(String resourceType, UUID existingId) {
        super("This " + resourceType + " has already been uploaded (existing id: " + existingId + ")");
    }
}
