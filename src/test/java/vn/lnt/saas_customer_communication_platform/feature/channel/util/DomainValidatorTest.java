package vn.lnt.saas_customer_communication_platform.feature.channel.util;

import org.junit.jupiter.api.Test;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidatorTest {

    @Test
    void shouldNormalizeValidDomains() {
        String input = "  example.com , shop.my-store.vn , localhost:3000 , *.company.org  ";
        String normalized = DomainValidator.validateAndNormalize(input);

        assertEquals("example.com,shop.my-store.vn,localhost:3000,*.company.org", normalized);
    }

    @Test
    void shouldHandleEmptyOrNullInput() {
        assertNull(DomainValidator.validateAndNormalize(null));
        assertNull(DomainValidator.validateAndNormalize("   "));
    }

    @Test
    void shouldThrowExceptionWhenProtocolOrPathIncluded() {
        InvalidOperationException ex1 = assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize("https://shop.my-store.vn"));
        assertTrue(ex1.getMessage().contains("Không nhập giao thức (http/https) hoặc đường dẫn (/)"));

        InvalidOperationException ex2 = assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize("shop.my-store.vn/checkout"));
        assertTrue(ex2.getMessage().contains("Không nhập giao thức (http/https) hoặc đường dẫn (/)"));
    }

    @Test
    void shouldThrowExceptionForInvalidDomainFormat() {
        assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize("invalid domain with spaces.com"));

        assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize("domain..double-dot.com"));

        assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize("-invalid-prefix.com"));
    }

    @Test
    void shouldThrowExceptionWhenAllowedDomainsTooLong() {
        String longInput = "a".repeat(501);
        assertThrows(InvalidOperationException.class, () ->
                DomainValidator.validateAndNormalize(longInput));
    }

    @Test
    void shouldAllowAllWhenAllowedDomainsIsEmptyOrWildcard() {
        assertTrue(DomainValidator.isOriginAllowed("https://any-site.com", null));
        assertTrue(DomainValidator.isOriginAllowed("https://any-site.com", ""));
        assertTrue(DomainValidator.isOriginAllowed("https://any-site.com", "*"));
    }

    @Test
    void shouldRejectWhenOriginIsEmpty() {
        assertFalse(DomainValidator.isOriginAllowed(null, "example.com"));
        assertFalse(DomainValidator.isOriginAllowed("", "example.com"));
    }

    @Test
    void shouldMatchExactDomain() {
        String allowed = "example.com,my-store.vn";

        assertTrue(DomainValidator.isOriginAllowed("https://example.com", allowed));
        assertTrue(DomainValidator.isOriginAllowed("http://example.com/products/123", allowed));
        assertTrue(DomainValidator.isOriginAllowed("https://my-store.vn", allowed));

        assertFalse(DomainValidator.isOriginAllowed("https://sub.example.com", allowed));
        assertFalse(DomainValidator.isOriginAllowed("https://otherdomain.com", allowed));
    }

    @Test
    void shouldMatchWildcardSubdomain() {
        String allowed = "*.company.com";

        assertTrue(DomainValidator.isOriginAllowed("https://app.company.com", allowed));
        assertTrue(DomainValidator.isOriginAllowed("https://blog.company.com", allowed));
        assertTrue(DomainValidator.isOriginAllowed("https://company.com", allowed));

        assertFalse(DomainValidator.isOriginAllowed("https://fakecompany.com", allowed));
        assertFalse(DomainValidator.isOriginAllowed("https://company.vn", allowed));
    }

    @Test
    void shouldMatchDomainWithPort() {
        String allowed = "localhost:3000,dev.local:8080";

        assertTrue(DomainValidator.isOriginAllowed("http://localhost:3000", allowed));
        assertTrue(DomainValidator.isOriginAllowed("http://dev.local:8080/dashboard", allowed));

        assertFalse(DomainValidator.isOriginAllowed("http://localhost:5000", allowed));
        assertFalse(DomainValidator.isOriginAllowed("http://dev.local:9000", allowed));
    }
}
