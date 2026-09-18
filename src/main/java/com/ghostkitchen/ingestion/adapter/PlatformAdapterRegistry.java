package com.ghostkitchen.ingestion.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring-managed Map<platformCode, SettlementReportAdapter>. Adding a new platform is one new
 * @Component implementing SettlementReportAdapter — zero changes here.
 */
@Component
public class PlatformAdapterRegistry {

    private final Map<String, SettlementReportAdapter> adaptersByPlatformCode;
    private final SettlementReportAdapter fallbackAdapter;

    public PlatformAdapterRegistry(List<SettlementReportAdapter> adapters, GenericCsvAdapter fallbackAdapter) {
        this.adaptersByPlatformCode = adapters.stream()
                .collect(Collectors.toMap(SettlementReportAdapter::supportedPlatformCode, Function.identity()));
        this.fallbackAdapter = fallbackAdapter;
    }

    /** Falls back to the generic CSV adapter for any platform without a dedicated one yet. */
    public SettlementReportAdapter resolve(String platformCode) {
        return adaptersByPlatformCode.getOrDefault(platformCode, fallbackAdapter);
    }
}
