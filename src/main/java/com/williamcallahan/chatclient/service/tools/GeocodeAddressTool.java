package com.williamcallahan.chatclient.service.tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.williamcallahan.applemaps.AppleMaps;
import com.williamcallahan.applemaps.domain.model.Location;
import com.williamcallahan.applemaps.domain.model.Place;
import com.williamcallahan.applemaps.domain.model.PlaceResults;
import com.williamcallahan.applemaps.domain.model.StructuredAddress;
import com.williamcallahan.applemaps.domain.request.GeocodeInput;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts street addresses to coordinates using Apple Maps geocoding.
 * Use for full or partial street addresses like "880 Harrison St, San Francisco, CA".
 */
public final class GeocodeAddressTool implements Tool {

    public static final String NAME = "geocode_address";

    private final AppleMaps client;

    public GeocodeAddressTool(AppleMaps client) {
        this.client = client;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDefinition definition() {
        Map<String, Object> props = Map.of(
            "address",
            Map.of(
                "type", "string",
                "description",
                "Street address to geocode (e.g. \"880 Harrison St, San Francisco, CA 94107\")"
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
                "Convert a street address to geographic coordinates using Apple Maps geocoding. " +
                "Returns structured address components and precise latitude/longitude."
            )
            .parameters(
                FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(props))
                    .putAdditionalProperty("required", JsonValue.from(List.of("address")))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build()
            )
            .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String address = (String) arguments.get("address");
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address is required");
        }

        String countryCode = (String) arguments.get("country_code");

        GeocodeInput.Builder builder = GeocodeInput.builder(address.trim())
            .language("en-US");

        if (countryCode != null && !countryCode.isBlank()) {
            builder.limitToCountries(List.of(countryCode.trim().toUpperCase()));
        }

        PlaceResults response = client.geocode(builder.build());

        if (response == null || response.results() == null || response.results().isEmpty()) {
            return Map.of(
                "results", List.of(),
                "message", "No geocoding results found for \"" + address + "\""
            );
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Place place : response.results()) {
            results.add(toResultMap(place));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("query", address);
        out.put("count", results.size());
        out.put("results", results);
        return out;
    }

    private Map<String, Object> toResultMap(Place place) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Name (if present)
        if (place.name() != null && !place.name().isBlank()) {
            result.put("name", place.name());
        }

        // Formatted address
        String formatted = formatAddress(place);
        if (!formatted.isBlank()) {
            result.put("formatted_address", formatted);
        }

        // Structured address components
        place.structuredAddress().ifPresent(addr -> {
            Map<String, String> components = new LinkedHashMap<>();
            addr.fullThoroughfare().filter(s -> !s.isBlank())
                .ifPresent(v -> components.put("street", v));
            addr.locality().filter(s -> !s.isBlank())
                .ifPresent(v -> components.put("city", v));
            addr.administrativeArea().filter(s -> !s.isBlank())
                .ifPresent(v -> components.put("state", v));
            addr.postCode().filter(s -> !s.isBlank())
                .ifPresent(v -> components.put("postal_code", v));
            if (!components.isEmpty()) {
                result.put("address_components", components);
            }
        });

        // Coordinates
        Location coord = place.coordinate();
        if (coord != null) {
            result.put("latitude", coord.latitude());
            result.put("longitude", coord.longitude());
        }

        // Place ID
        place.id().ifPresent(id -> result.put("place_id", id));

        return result;
    }

    private String formatAddress(Place place) {
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
}
