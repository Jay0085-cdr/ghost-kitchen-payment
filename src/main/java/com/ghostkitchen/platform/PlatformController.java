package com.ghostkitchen.platform;

import com.ghostkitchen.repository.PlatformRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reference-data lookup (Swiggy/Zomato/Direct) — used to populate platform dropdowns. */
@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private final PlatformRepository platformRepository;

    public PlatformController(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    @GetMapping
    public List<PlatformResponse> list() {
        return platformRepository.findAll().stream().map(PlatformResponse::from).toList();
    }
}
