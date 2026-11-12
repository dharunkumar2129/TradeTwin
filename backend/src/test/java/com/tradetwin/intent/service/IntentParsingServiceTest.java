package com.tradetwin.intent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.service.CatalogSearchService;
import com.tradetwin.intent.model.BuyerIntent;
import com.tradetwin.intent.repository.BuyerIntentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IntentParsingServiceTest {

    @Mock
    private BuyerIntentRepository buyerIntentRepository;

    private IntentParsingService parsingService;

    static class StubCatalogSearchService extends CatalogSearchService {
        public StubCatalogSearchService() { super(null, null, null, null); }
    }

    @BeforeEach
    void setUp() {
        parsingService = new IntentParsingService(new StubCatalogSearchService(), buyerIntentRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("Heuristic parser accurately extracts Sony headphones request with budget, warranty, delivery constraints")
    void testHeuristicParserWithUserExample() {
        String query = "Find me Sony wireless noise-cancelling headphones. My maximum total budget is $100. I prefer at least one year warranty and delivery within four days.";

        BuyerIntent intent = parsingService.parseHeuristic(query);

        assertNotNull(intent);
        assertTrue(intent.getProductType().toLowerCase().contains("headphones"));
        assertTrue(intent.getBrandPreferences().contains("Sony"));
        assertEquals(new BigDecimal("100"), intent.getMaxBudget());
        assertNotNull(intent.getTargetBudget());
        assertTrue(intent.getTargetBudget().compareTo(intent.getMaxBudget()) < 0, "Target budget must be lower than ceiling max budget");
        assertEquals(12, intent.getMinWarrantyMonths());
        assertEquals(4, intent.getMaxDeliveryDays());
        assertTrue(intent.getRequiredSpecifications().containsKey("anc"));
        assertFalse(intent.isAmbiguous());
        assertEquals("Brand New", intent.getPreferredConditions().get(0));
    }

    @Test
    @DisplayName("Heuristic parser detects ambiguity and formulates follow-up question when required info is missing")
    void testAmbiguousQueryHandling() {
        String vagueQuery = "get stuff";

        BuyerIntent intent = parsingService.parseHeuristic(vagueQuery);

        assertNotNull(intent);
        assertTrue(intent.isAmbiguous());
        assertNotNull(intent.getFollowUpQuestion());
        assertTrue(intent.getFollowUpQuestion().contains("clarify"));
    }
}
