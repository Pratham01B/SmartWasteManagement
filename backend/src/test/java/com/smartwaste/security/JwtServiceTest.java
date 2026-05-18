package com.smartwaste.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtService.
 * Uses a real HMAC-SHA key (no mocks needed — JwtService has no injected dependencies).
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    // Valid 256-bit Base64-encoded secret (same format as application.yml default)
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static final long JWT_EXPIRATION     = 86_400_000L;  // 24 h
    private static final long REFRESH_EXPIRATION = 604_800_000L; // 7 days

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Inject @Value fields via ReflectionTestUtils (no Spring context needed)
        ReflectionTestUtils.setField(jwtService, "secretKey",        TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration",    JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        userDetails = User.builder()
                .username("test@smartwaste.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }

    // -------------------------------------------------------------------------
    // generateToken(UserDetails)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateToken returns a non-blank JWT string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken embeds the correct subject (username)")
    void generateToken_embedsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token))
                .isEqualTo(userDetails.getUsername());
    }

    // -------------------------------------------------------------------------
    // generateToken(Map, UserDetails)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateToken with extra claims includes those claims in the token")
    void generateToken_withExtraClaims_includesClaims() {
        Map<String, Object> extra = Map.of("role", "CITIZEN", "userId", 42);
        String token = jwtService.generateToken(extra, userDetails);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        Integer userId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));

        assertThat(role).isEqualTo("CITIZEN");
        assertThat(userId).isEqualTo(42);
    }

    // -------------------------------------------------------------------------
    // generateRefreshToken(UserDetails)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateRefreshToken returns a non-blank JWT string")
    void generateRefreshToken_returnsNonBlankToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken embeds the correct subject")
    void generateRefreshToken_embedsCorrectUsername() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertThat(jwtService.extractUsername(token))
                .isEqualTo(userDetails.getUsername());
    }

    @Test
    @DisplayName("refresh token and access token are different strings")
    void refreshToken_isDifferentFromAccessToken() {
        String access  = jwtService.generateToken(userDetails);
        String refresh = jwtService.generateRefreshToken(userDetails);
        // They may occasionally collide within the same millisecond in theory,
        // but expiration claims differ so the compact form will differ.
        assertThat(access).isNotEqualTo(refresh);
    }

    // -------------------------------------------------------------------------
    // extractUsername(String)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractUsername returns the username embedded in the token")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token))
                .isEqualTo("test@smartwaste.com");
    }

    @Test
    @DisplayName("extractUsername throws on a malformed token")
    void extractUsername_throwsOnMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    // -------------------------------------------------------------------------
    // extractClaim(String, Function<Claims, T>)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractClaim can retrieve the issuedAt date")
    void extractClaim_retrievesIssuedAt() {
        String token = jwtService.generateToken(userDetails);
        java.util.Date issuedAt = jwtService.extractClaim(token, claims -> claims.getIssuedAt());
        assertThat(issuedAt)
                .isNotNull()
                .isBeforeOrEqualTo(new java.util.Date());
    }

    @Test
    @DisplayName("extractClaim can retrieve the expiration date")
    void extractClaim_retrievesExpiration() {
        String token = jwtService.generateToken(userDetails);
        java.util.Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());
        assertThat(expiration)
                .isNotNull()
                .isAfter(new java.util.Date());
    }

    // -------------------------------------------------------------------------
    // isTokenValid(String, UserDetails)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isTokenValid returns true for a fresh token and matching user")
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false when username does not match")
    void isTokenValid_returnsFalseForWrongUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = User.builder()
                .username("other@smartwaste.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid throws ExpiredJwtException for an already-expired token")
    void isTokenValid_throwsForExpiredToken() {
        // Set expiration to -1 ms so the token is expired immediately
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String expiredToken = jwtService.generateToken(userDetails);

        // Restore normal expiration for the validation call
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
