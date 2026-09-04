package com.tdp.dsp.gateway.layer.compliance;

import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;

import java.util.Locale;
import java.util.Set;

/**
 * 预留扩展：目的国/目的数据空间白名单。空白名单表示尚未启用。
 */
public class DestinationAllowlistHook implements ComplianceHook {

    private final Set<String> allowedDestinations;

    public DestinationAllowlistHook(Set<String> allowedDestinations) {
        this.allowedDestinations = Set.copyOf(allowedDestinations);
    }

    @Override
    public String name() {
        return "destination-allowlist";
    }

    @Override
    public ComplianceDecision check(InteropEnvelope envelope) {
        if (envelope.direction() != Direction.OUTBOUND || allowedDestinations.isEmpty()) {
            return ComplianceDecision.allow();
        }
        String destination = Jsons.text(envelope.metadata(), "destinationCountry", "idsParticipantId");
        if (destination == null || destination.isBlank()) {
            return ComplianceDecision.allow();
        }
        String normalized = destination.toLowerCase(Locale.ROOT);
        boolean matched = allowedDestinations.stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> normalized.contains(item) || item.contains(normalized));
        if (!matched) {
            return ComplianceDecision.deny("CB-DESTINATION", "目的地不在跨境白名单: " + destination);
        }
        return ComplianceDecision.allow();
    }
}
