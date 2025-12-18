package com.williamcallahan.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides real-time weather data and forecasts via Open-Meteo.
 * Geocodes the city name, then fetches the forecast.
 */
public final class WeatherForecastTool implements Tool {
    public static final String NAME = "get_weather_forecast";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private static final int GEOCODE_CANDIDATES = 20;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDefinition definition() {
        Map<String, Object> props = Map.of(
            "city", Map.of("type", "string", "description", "Place name only, without comma qualifiers (e.g. \"San Mateo\" or \"San Francisco\")"),
            "days", Map.of("type", "integer", "description", "Forecast days ahead (1-16)", "minimum", 1, "maximum", 16),
            "country_code", Map.of("type", "string", "description", "Optional 2-letter country code hint (e.g. \"US\")"),
            "admin1", Map.of("type", "string", "description", "Optional state/province/region hint (e.g. \"California\")"),
            "admin2", Map.of("type", "string", "description", "Optional county/district hint"),
            "country", Map.of("type", "string", "description", "Optional country name hint (e.g. \"United States\")")
        );

        return FunctionDefinition.builder()
            .name(NAME)
            .description("Get current weather and daily forecast for the next N days for a city via Open-Meteo.")
            .parameters(FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(props))
                .putAdditionalProperty("required", JsonValue.from(List.of("city")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build())
            .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String city = (String) arguments.get("city");
        int days = arguments.containsKey("days") ? ((Number) arguments.get("days")).intValue() : 5;
        days = Math.max(1, Math.min(days, 16));
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city is required");
        }

        String countryCode = (String) arguments.get("country_code");
        String admin1 = (String) arguments.get("admin1");
        String admin2 = (String) arguments.get("admin2");
        String country = (String) arguments.get("country");

        String cityName = city.trim();
        int comma = cityName.indexOf(',');
        if (comma >= 0) cityName = cityName.substring(0, comma).trim();

        GeoHint hint = new GeoHint(countryCode, country, admin1, admin2);
        Geo geo = geocode(cityName, hint);
        JsonNode forecast = forecast(geo, days);

        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> current = new LinkedHashMap<>();
        JsonNode cw = forecast.path("current_weather");
        current.put("location", geo.name + ", " + geo.country);
        current.put("temp_F", cw.path("temperature").asDouble());
        current.put("wind_mph", Math.round(cw.path("windspeed").asDouble() / 1.60934));
        current.put("condition", cw.path("weathercode").asInt());
        out.put("current", current);

        JsonNode daily = forecast.path("daily");
        JsonNode times = daily.path("time");
        JsonNode highs = daily.path("temperature_2m_max");
        JsonNode lows = daily.path("temperature_2m_min");
        JsonNode codes = daily.path("weathercode");

        List<Map<String, Object>> fc = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("date", times.path(i).asText());
            d.put("high_F", highs.path(i).asDouble());
            d.put("low_F", lows.path(i).asDouble());
            d.put("condition", codes.path(i).asInt());
            fc.add(d);
        }
        out.put("forecast", fc);

        return out;
    }

    private static final class Geo {
        final double lat;
        final double lon;
        final String tz;
        final String country;
        final String name;

        Geo(double lat, double lon, String tz, String country, String name) {
            this.lat = lat;
            this.lon = lon;
            this.tz = tz;
            this.country = country;
            this.name = name;
        }
    }

    private record GeoHint(String countryCode, String country, String admin1, String admin2) {
        boolean any() {
            return (countryCode != null && !countryCode.isBlank())
                || (country != null && !country.isBlank())
                || (admin1 != null && !admin1.isBlank())
                || (admin2 != null && !admin2.isBlank());
        }
    }

    private static Geo geocode(String cityName, GeoHint hint) throws Exception {
        if (cityName == null || cityName.isBlank()) throw new IllegalArgumentException("city is required");

        JsonNode results = geocodeResults(cityName, GEOCODE_CANDIDATES);
        if (!results.isArray() || results.size() == 0) {
            throw new IllegalArgumentException("No geocoding result for city: " + cityName);
        }

        JsonNode r = chooseBestResult(results, hint);
        return new Geo(
            r.path("latitude").asDouble(),
            r.path("longitude").asDouble(),
            r.path("timezone").asText("UTC"),
            r.path("country").asText(""),
            r.path("name").asText(cityName)
        );
    }

    private static JsonNode geocodeResults(String name, int count) throws Exception {
        String q = URLEncoder.encode(name, StandardCharsets.UTF_8);
        int c = Math.max(1, Math.min(count, 100));
        URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=" + c);
        JsonNode root = getJson(uri);
        return root.path("results");
    }

    private static JsonNode chooseBestResult(JsonNode results, GeoHint hint) {
        if (hint == null || !hint.any()) return results.path(0);

        int bestScore = Integer.MIN_VALUE;
        int bestPopulation = -1;
        JsonNode best = results.path(0);

        for (JsonNode r : results) {
            int score = scoreCandidate(r, hint);
            int pop = r.path("population").asInt(0);
            if (score > bestScore || (score == bestScore && pop > bestPopulation)) {
                bestScore = score;
                bestPopulation = pop;
                best = r;
            }
        }
        return best;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) return false;
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private static int scoreCandidate(JsonNode r, GeoHint hint) {
        int score = 0;
        if (matches(r.path("country_code").asText(""), hint.countryCode)) score += 10;
        if (matches(r.path("country").asText(""), hint.country)) score += 6;
        if (matches(r.path("admin1").asText(""), hint.admin1)) score += 8;
        if (matches(r.path("admin2").asText(""), hint.admin2)) score += 4;
        return score;
    }

    private static boolean matches(String actual, String hint) {
        return hint != null && !hint.isBlank() && containsIgnoreCase(actual, hint.trim());
    }

    private static JsonNode forecast(Geo geo, int days) throws Exception {
        String tz = URLEncoder.encode(geo.tz, StandardCharsets.UTF_8);
        String url =
            "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + geo.lat
                + "&longitude=" + geo.lon
                + "&current_weather=true"
                + "&daily=weathercode,temperature_2m_max,temperature_2m_min"
                + "&forecast_days=" + days
                + "&temperature_unit=fahrenheit"
                + "&timezone=" + tz;
        return getJson(URI.create(url));
    }

    private static JsonNode getJson(URI uri) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + res.statusCode() + " from " + uri);
        }
        return JSON.readTree(res.body());
    }
}
