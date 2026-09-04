package com.tdp.dsp.gateway.mapping;

import com.tdp.dsp.gateway.json.Jsons;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractSchemaValidatorTest {

    private final ContractSchemaValidator validator = new ContractSchemaValidator();

    @Test
    void rejectsCatalogQueryMissingIssuer() {
        List<String> errors = validator.validate(
                "tdp-catalog-query.schema.json",
                Jsons.objectOf("keyword", "交通")
        );
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(item -> item.contains("issuerId")));
    }

    @Test
    void acceptsCatalogQueryWithRequiredFields() {
        List<String> errors = validator.validate(
                "tdp-catalog-query.schema.json",
                Jsons.objectOf("issuerId", "CN-1", "issuerEntityId", "ENT-1", "keyword", "交通")
        );
        assertTrue(errors.isEmpty());
    }
}
