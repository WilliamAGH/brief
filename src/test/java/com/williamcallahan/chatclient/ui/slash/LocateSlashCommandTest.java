package com.williamcallahan.chatclient.ui.slash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocateSlashCommandTest {

    @Test
    void extractQueryReturnsEmptyForNullAndBlank() {
        assertEquals("", LocateSlashCommand.extractQuery(null));
        assertEquals("", LocateSlashCommand.extractQuery(""));
        assertEquals("", LocateSlashCommand.extractQuery("   "));
    }

    @Test
    void extractQueryReturnsBareQuery() {
        assertEquals("coffee shops", LocateSlashCommand.extractQuery("/locate coffee shops"));
        assertEquals("SF", LocateSlashCommand.extractQuery("/locate SF"));
    }

    @Test
    void extractQueryStripsQuotes() {
        assertEquals("Blue Bottle", LocateSlashCommand.extractQuery("/locate \"Blue Bottle\""));
    }

    @Test
    void extractQueryReturnsEmptyForBareLocate() {
        assertEquals("", LocateSlashCommand.extractQuery("/locate"));
        assertEquals("", LocateSlashCommand.extractQuery("/locate   "));
    }

    @Test
    void extractQueryReturnsEmptyForNonLocateInput() {
        assertEquals("", LocateSlashCommand.extractQuery("/weather SF"));
        assertEquals("", LocateSlashCommand.extractQuery("hello"));
    }

    @Test
    void extractQueryHandlesCaseInsensitivePrefix() {
        assertEquals("park", LocateSlashCommand.extractQuery("/LOCATE park"));
        assertEquals("park", LocateSlashCommand.extractQuery("/Locate park"));
    }

    @Test
    void extractQueryPreservesSingleCharQuoted() {
        assertEquals("a", LocateSlashCommand.extractQuery("/locate \"a\""));
    }
}
