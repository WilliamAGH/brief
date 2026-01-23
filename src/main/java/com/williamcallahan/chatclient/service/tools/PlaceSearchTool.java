package com.williamcallahan.chatclient.service.tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.williamcallahan.applemaps.AppleMaps;
import com.williamcallahan.applemaps.domain.model.Location;
import com.williamcallahan.applemaps.domain.model.PoiCategory;
import com.williamcallahan.applemaps.domain.model.SearchResponse;
import com.williamcallahan.applemaps.domain.model.SearchResponsePlace;
import com.williamcallahan.applemaps.domain.model.StructuredAddress;
import com.williamcallahan.applemaps.domain.request.SearchInput;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches for places, businesses, and POIs using Apple Maps.
 * Uses natural language queries like "coffee shops" or "Stripe San Francisco".
 */
public final class PlaceSearchTool implements Tool {

    public static final String NAME = "search_places";

    private final AppleMaps client;

    public PlaceSearchTool(AppleMaps client) {
        this.client = client;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDefinition definition() {
        Map<String, Object> props = Map.of(
            "query",
            Map.of(
                "type", "string",
                "description",
                "Natural language search query (e.g. \"coffee shops\", \"Stripe\", \"restaurants in San Francisco\")"
            ),
            "country_code",
            Map.of(
                "type", "string",
                "description",
                "Optional ISO 3166-1 alpha-2 country code to limit results (e.g. \"US\", \"GB\")"
            )
        );

        return FunctionDefinition.builder()
            .name(NAME)
            .description(
                "Search for places, businesses, or points of interest using Apple Maps. " +
                "Returns name, address, category, and coordinates for matching places."
            )
            .parameters(
                FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(props))
                    .putAdditionalProperty("required", JsonValue.from(List.of("query")))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build()
            )
            .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String query = (String) arguments.get("query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        String countryCode = (String) arguments.get("country_code");

        SearchInput.Builder builder = SearchInput.builder(query.trim())
            .language("en-US");

        if (countryCode != null && !countryCode.isBlank()) {
            builder.limitToCountries(List.of(countryCode.trim().toUpperCase()));
        }

        SearchResponse response = client.search(builder.build());

        if (response == null || response.results() == null || response.results().isEmpty()) {
            return Map.of(
                "results", List.of(),
                "message", "No places found for \"" + query + "\""
            );
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchResponsePlace place : response.results()) {
            results.add(toResultMap(place));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("query", query);
        out.put("count", results.size());
        out.put("results", results);
        return out;
    }

    private Map<String, Object> toResultMap(SearchResponsePlace place) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("name", place.name() != null ? place.name() : "");

        // Format address
        String address = formatAddress(place);
        if (!address.isBlank()) {
            result.put("address", address);
        }

        // Category
        place.poiCategory()
            .map(PoiCategory::name)
            .map(this::formatCategory)
            .filter(c -> !c.isBlank())
            .ifPresent(c -> result.put("category", c));

        // Coordinates
        Location coord = place.coordinate();
        if (coord != null) {
            result.put("latitude", coord.latitude());
            result.put("longitude", coord.longitude());
        }

        // Place ID for potential follow-up lookups
        place.id().ifPresent(id -> result.put("place_id", id));

        return result;
    }

    private String formatAddress(SearchResponsePlace place) {
        if (place == null) return "";

        // Try structured address first
        var addrOpt = place.structuredAddress();
        if (addrOpt != null && addrOpt.isPresent()) {
            return formatStructuredAddress(addrOpt.get());
        }

        // Fall back to formatted address lines
        if (place.formattedAddressLines() != null && !place.formattedAddressLines().isEmpty()) {
            return String.join(", ", place.formattedAddressLines());
        }

        return "";
    }

    private String formatStructuredAddress(StructuredAddress addr) {
        if (addr == null) return "";

        StringBuilder sb = new StringBuilder();
        addr.fullThoroughfare().filter(s -> !s.isBlank()).ifPresent(sb::append);

        addr.locality().filter(s -> !s.isBlank()).ifPresent(locality -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(locality);
        });

        addr.administrativeArea().filter(s -> !s.isBlank()).ifPresent(admin -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(admin);
        });

        addr.postCode().filter(s -> !s.isBlank()).ifPresent(postCode -> {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(postCode);
        });

        return sb.toString();
    }

    private String formatCategory(String poiCategory) {
        if (poiCategory == null || poiCategory.isBlank()) return "";
        return poiCategory
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .replaceAll("_", " ");
    }
}
