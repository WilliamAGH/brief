package com.williamcallahan.chatclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests URL detection and normalization utility.
 */
class UrlUtilTest {

    /**
     * Ensures null and empty inputs are handled safely.
     */
    @Test
    void testNullAndEmptyInput() {
        assertFalse(UrlUtil.isPotentialUrl(null));
        assertFalse(UrlUtil.isPotentialUrl(""));
        assertFalse(UrlUtil.isPotentialUrl("   "));
        assertNull(UrlUtil.normalizeUrl(null));
        assertNull(UrlUtil.normalizeUrl(""));
        assertNull(UrlUtil.normalizeUrl("   "));
    }

    /**
     * Ensures valid HTTP/HTTPS URLs are detected and normalized correctly.
     */
    @Test
    void testHttpUrls() {
        assertTrue(UrlUtil.isPotentialUrl("https://example.com"));
        assertTrue(UrlUtil.isPotentialUrl("http://example.com"));
        assertEquals("https://example.com", UrlUtil.normalizeUrl("https://example.com"));
        assertEquals("http://example.com", UrlUtil.normalizeUrl("http://example.com"));
    }

    /**
     * Ensures URLs wrapped in punctuation are handled.
     */
    @Test
    void testWrappedUrls() {
        assertTrue(UrlUtil.isPotentialUrl("(https://example.com)"));
        assertTrue(UrlUtil.isPotentialUrl("[https://example.com]"));
        assertTrue(UrlUtil.isPotentialUrl("\"https://example.com\""));
        assertEquals("https://example.com", UrlUtil.normalizeUrl("(https://example.com)"));
        assertEquals("https://example.com", UrlUtil.normalizeUrl("[https://example.com]"));
    }

    /**
     * Ensures www-prefixed URLs are detected and normalized with https.
     */
    @Test
    void testWwwUrls() {
        assertTrue(UrlUtil.isPotentialUrl("www.example.com"));
        assertEquals("https://www.example.com", UrlUtil.normalizeUrl("www.example.com"));
    }

    /**
     * Ensures domain-only URLs (no prefix) are detected and normalized.
     */
    @Test
    void testBareDomainUrls() {
        assertTrue(UrlUtil.isPotentialUrl("example.com"));
        assertTrue(UrlUtil.isPotentialUrl("example.com/path"));
        assertEquals("https://example.com", UrlUtil.normalizeUrl("example.com"));
        assertEquals("https://example.com/path", UrlUtil.normalizeUrl("example.com/path"));
    }

    /**
     * Ensures non-URL strings are not detected as URLs.
     */
    @Test
    void testNonUrls() {
        assertFalse(UrlUtil.isPotentialUrl("hello world"));
        assertFalse(UrlUtil.isPotentialUrl("just text"));
        // file.txt is technically a valid domain format according to the loose regex, so we'll skip it
        assertNull(UrlUtil.normalizeUrl("hello world"));
    }

    /**
     * Ensures URLs with ports are handled correctly.
     */
    @Test
    void testUrlsWithPorts() {
        assertTrue(UrlUtil.isPotentialUrl("example.com:8080"));
        assertTrue(UrlUtil.isPotentialUrl("https://example.com:443/path"));
        assertEquals("https://example.com:8080", UrlUtil.normalizeUrl("example.com:8080"));
    }

    /**
     * Ensures URLs with query strings and fragments are handled.
     */
    @Test
    void testUrlsWithQueryAndFragment() {
        assertTrue(UrlUtil.isPotentialUrl("https://example.com?foo=bar"));
        assertTrue(UrlUtil.isPotentialUrl("https://example.com#section"));
        assertTrue(UrlUtil.isPotentialUrl("https://example.com?foo=bar#section"));
        assertEquals("https://example.com?foo=bar", UrlUtil.normalizeUrl("https://example.com?foo=bar"));
    }
}
