package com.ghostkitchen.platform;

import com.ghostkitchen.entity.Platform;

import java.util.UUID;

public record PlatformResponse(UUID id, String code, String displayName) {

    public static PlatformResponse from(Platform platform) {
        return new PlatformResponse(platform.getId(), platform.getCode(), platform.getDisplayName());
    }
}
