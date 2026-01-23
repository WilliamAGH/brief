package com.williamcallahan.chatclient.service;

import com.williamcallahan.applemaps.AppleMaps;
import com.williamcallahan.applemaps.domain.model.AutocompleteResult;
import com.williamcallahan.applemaps.domain.model.Location;
import com.williamcallahan.applemaps.domain.model.Place;
import com.williamcallahan.applemaps.domain.model.PoiCategory;
import com.williamcallahan.applemaps.domain.model.SearchAutocompleteResponse;
import com.williamcallahan.applemaps.domain.model.SearchResponse;
import com.williamcallahan.applemaps.domain.model.SearchResponsePlace;
import com.williamcallahan.applemaps.domain.model.StructuredAddress;
import com.williamcallahan.applemaps.domain.request.SearchAutocompleteInput;
import com.williamcallahan.applemaps.domain.request.SearchInput;
import com.williamcallahan.chatclient.Config;
import java.util.ArrayList;
import java.util.List;

/**
 * Service wrapper for Apple Maps API providing search and place lookup functionality.
 * Token resolved via Config (APPLE_MAPS_TOKEN env var or apple_maps.token config property).
 */
public final class AppleMapsService {

    private final AppleMaps client;

    public AppleMapsService(Config config) {
        String token = config.resolveAppleMapsToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Apple Maps token not configured. Set APPLE_MAPS_TOKEN environment variable or apple_maps.token in config."
            );
        }
        this.client = new AppleMaps(token);
    }

    public static boolean isConfigured(Config config) {
        return config.hasAppleMapsToken();
    }

    /**
     * Search for places matching the query.
     * Returns a list of place results with name, address, and coordinates.
     */
    public List<PlaceResult> search(String query) {
        if (query == null || query.isBlank()) return List.of();

        SearchInput input = SearchInput.builder(query)
            .language("en-US")
            .build();

        SearchResponse response = client.search(input);
        return toPlaceResults(response);
    }

    /**
     * Search with location context for more relevant results.
     */
    public List<PlaceResult> searchNear(
        String query,
        double latitude,
        double longitude
    ) {
        if (query == null || query.isBlank()) return List.of();

        // For now, just do a regular search - location-based search requires
        // constructing a SearchLocation object which is more complex
        SearchInput input = SearchInput.builder(query)
            .language("en-US")
            .build();

        SearchResponse response = client.search(input);
        return toPlaceResults(response);
    }

    /**
     * Get autocomplete suggestions for partial queries.
     */
    public List<AutocompleteSuggestion> autocomplete(String query) {
        if (query == null || query.isBlank()) return List.of();

        SearchAutocompleteInput input = SearchAutocompleteInput.builder(query)
            .language("en-US")
            .build();

        SearchAutocompleteResponse response = client.autocomplete(input);
        if (response == null || response.results() == null) return List.of();

        List<AutocompleteSuggestion> suggestions = new ArrayList<>();
        for (AutocompleteResult result : response.results()) {
            suggestions.add(
                new AutocompleteSuggestion(
                    result.displayLines() != null &&
                        !result.displayLines().isEmpty()
                        ? String.join(", ", result.displayLines())
                        : "",
                    result.completionUrl()
                )
            );
        }
        return suggestions;
    }

    /**
     * Resolve an autocomplete suggestion to full place details.
     */
    public List<PlaceResult> resolveAutocomplete(String completionUrl) {
        if (completionUrl == null || completionUrl.isBlank()) return List.of();
        SearchResponse response = client.resolveCompletionUrl(completionUrl);
        return toPlaceResults(response);
    }

    /**
     * Look up detailed information about a specific place by ID.
     */
    public PlaceResult lookupPlace(String placeId) {
        if (placeId == null || placeId.isBlank()) return null;
        Place place = client.lookupPlace(placeId);
        if (place == null) return null;
        return toPlaceResultFromPlace(place);
    }

    private List<PlaceResult> toPlaceResults(SearchResponse response) {
        if (response == null || response.results() == null) return List.of();
        List<PlaceResult> results = new ArrayList<>();
        for (SearchResponsePlace place : response.results()) {
            PlaceResult pr = toPlaceResultFromSearchPlace(place);
            if (pr != null) results.add(pr);
        }
        return results;
    }

    private PlaceResult toPlaceResultFromSearchPlace(
        SearchResponsePlace place
    ) {
        if (place == null) return null;

        String name = place.name() != null ? place.name() : "";
        String formattedAddress = formatAddressFromSearchPlace(place);
        String category = place
            .poiCategory()
            .map(PoiCategory::name)
            .map(this::formatCategory)
            .orElse("");

        double lat = 0.0;
        double lon = 0.0;
        Location coord = place.coordinate();
        if (coord != null) {
            lat = coord.latitude();
            lon = coord.longitude();
        }

        String placeId = place.id().orElse(null);

        return new PlaceResult(
            name,
            formattedAddress,
            category,
            lat,
            lon,
            placeId,
            null,
            null
        );
    }

    private PlaceResult toPlaceResultFromPlace(Place place) {
        if (place == null) return null;

        String name = place.name() != null ? place.name() : "";
        String formattedAddress = formatAddressFromPlace(place);

        double lat = 0.0;
        double lon = 0.0;
        Location coord = place.coordinate();
        if (coord != null) {
            lat = coord.latitude();
            lon = coord.longitude();
        }

        String placeId = place.id().orElse(null);

        return new PlaceResult(
            name,
            formattedAddress,
            "",
            lat,
            lon,
            placeId,
            null,
            null
        );
    }

    private String formatAddressFromSearchPlace(SearchResponsePlace place) {
        if (place == null) return "";

        // Try structured address first
        var addrOpt = place.structuredAddress();
        if (addrOpt != null && addrOpt.isPresent()) {
            return formatStructuredAddress(addrOpt.get());
        }

        // Fall back to formatted address lines
        if (
            place.formattedAddressLines() != null &&
            !place.formattedAddressLines().isEmpty()
        ) {
            return String.join(", ", place.formattedAddressLines());
        }

        return "";
    }

    private String formatAddressFromPlace(Place place) {
        if (place == null) return "";

        // Try structured address first
        var addrOpt = place.structuredAddress();
        if (addrOpt != null && addrOpt.isPresent()) {
            return formatStructuredAddress(addrOpt.get());
        }

        // Fall back to formatted address lines
        if (
            place.formattedAddressLines() != null &&
            !place.formattedAddressLines().isEmpty()
        ) {
            return String.join(", ", place.formattedAddressLines());
        }

        return "";
    }

    private String formatStructuredAddress(StructuredAddress addr) {
        if (addr == null) return "";

        StringBuilder sb = new StringBuilder();
        String thoroughfare = addr.fullThoroughfare().orElse(null);
        if (thoroughfare != null && !thoroughfare.isBlank()) {
            sb.append(thoroughfare);
        }
        String locality = addr.locality().orElse(null);
        if (locality != null && !locality.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(locality);
        }
        String adminArea = addr.administrativeArea().orElse(null);
        if (adminArea != null && !adminArea.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(adminArea);
        }
        String postCode = addr.postCode().orElse(null);
        if (postCode != null && !postCode.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(postCode);
        }
        return sb.toString();
    }

    private String formatCategory(String poiCategory) {
        if (poiCategory == null || poiCategory.isBlank()) return "";
        // Convert "Restaurant" or "CoffeeShop" to friendly format
        return poiCategory
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .replaceAll("_", " ");
    }

    /** Represents a place search result. */
    public record PlaceResult(
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        String placeId,
        String phone,
        String url
    ) {
        public boolean hasCoordinates() {
            return latitude != 0.0 || longitude != 0.0;
        }
    }

    /** Represents an autocomplete suggestion. */
    public record AutocompleteSuggestion(
        String displayText,
        String completionUrl
    ) {}
}
