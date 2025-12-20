package com.williamcallahan.service.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherForecastToolTest {

    @Test
    void describeWeatherCode_ReturnsHumanText() {
        assertEquals("Clear sky", WeatherForecastTool.describeWeatherCode(0));
        assertEquals("Drizzle (moderate)", WeatherForecastTool.describeWeatherCode(53));
        assertEquals("Rain showers (slight)", WeatherForecastTool.describeWeatherCode(80));
    }

    @Test
    void describeWeatherCode_UnknownFallback() {
        assertEquals("Unknown", WeatherForecastTool.describeWeatherCode(999));
    }
}
