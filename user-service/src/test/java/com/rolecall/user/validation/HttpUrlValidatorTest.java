package com.rolecall.user.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlValidatorTest {

    private final HttpUrlValidator validator = new HttpUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://cdn.example.com/resume.pdf",
            "http://localhost:9000/bucket/resume.pdf",
            "https://example.com/path?query=1#fragment"
    })
    void acceptsWellFormedHttpAndHttpsUrls(String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "file:///etc/passwd",
            "ftp://example.com/resume.pdf",
            "not a url at all",
            "http://user:pass@evil.com/resume.pdf",
            "http://",
            "//example.com/resume.pdf"
    })
    void rejectsNonHttpSchemesAndMalformedUrls(String url) {
        assertThat(validator.isValid(url, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void treatsBlankAsValidSincePresenceIsCheckedSeparately(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void treatsNullAsValidSincePresenceIsCheckedSeparately() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
