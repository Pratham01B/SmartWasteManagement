package com.smartwaste.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the User entity.
 * Covers helper methods and UserDetails contract.
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@example.com")
                .passwordHash("hashed_password")
                .role(Role.CITIZEN)
                .isActive(true)
                .isEmailVerified(false)
                .rewardPoints(0)
                .build();
    }

    // -------------------------
    // getFullName()
    // -------------------------

    @Nested
    @DisplayName("getFullName()")
    class GetFullName {

        @Test
        @DisplayName("returns first and last name concatenated with a space")
        void returnsFullName() {
            assertThat(user.getFullName()).isEqualTo("Rahul Sharma");
        }

        @Test
        @DisplayName("works when names contain extra characters")
        void worksWithSpecialNames() {
            user.setFirstName("Priya");
            user.setLastName("Singh-Verma");
            assertThat(user.getFullName()).isEqualTo("Priya Singh-Verma");
        }
    }

    // -------------------------
    // addRewardPoints()
    // -------------------------

    @Nested
    @DisplayName("addRewardPoints()")
    class AddRewardPoints {

        @Test
        @DisplayName("adds positive points to current balance")
        void addsPositivePoints() {
            user.addRewardPoints(10);
            assertThat(user.getRewardPoints()).isEqualTo(10);
        }

        @Test
        @DisplayName("accumulates points across multiple calls")
        void accumulatesPoints() {
            user.addRewardPoints(10);
            user.addRewardPoints(5);
            assertThat(user.getRewardPoints()).isEqualTo(15);
        }

        @Test
        @DisplayName("ignores zero — balance stays unchanged")
        void ignoresZero() {
            user.addRewardPoints(0);
            assertThat(user.getRewardPoints()).isEqualTo(0);
        }

        @Test
        @DisplayName("ignores negative values — balance stays unchanged")
        void ignoresNegativeValues() {
            user.addRewardPoints(20);
            user.addRewardPoints(-5);
            assertThat(user.getRewardPoints()).isEqualTo(20);
        }
    }

    // -------------------------
    // UserDetails — getUsername() / getPassword()
    // -------------------------

    @Nested
    @DisplayName("UserDetails — credentials")
    class UserDetailsCredentials {

        @Test
        @DisplayName("getUsername() returns the email address")
        void getUsernameReturnsEmail() {
            assertThat(user.getUsername()).isEqualTo("rahul@example.com");
        }

        @Test
        @DisplayName("getPassword() returns the password hash")
        void getPasswordReturnsHash() {
            assertThat(user.getPassword()).isEqualTo("hashed_password");
        }
    }

    // -------------------------
    // UserDetails — authorities
    // -------------------------

    @Nested
    @DisplayName("UserDetails — getAuthorities()")
    class GetAuthorities {

        @Test
        @DisplayName("returns a single authority prefixed with ROLE_")
        void returnsSingleRoleAuthority() {
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_CITIZEN");
        }

        @Test
        @DisplayName("authority reflects the user's actual role")
        void authorityMatchesRole() {
            user.setRole(Role.ADMIN);
            assertThat(user.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("WORKER role produces ROLE_WORKER authority")
        void workerRoleAuthority() {
            user.setRole(Role.WORKER);
            assertThat(user.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_WORKER");
        }
    }

    // -------------------------
    // UserDetails — account status flags
    // -------------------------

    @Nested
    @DisplayName("UserDetails — account status")
    class AccountStatus {

        @Test
        @DisplayName("isEnabled() returns true when isActive is true")
        void enabledWhenActive() {
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEnabled() returns false when isActive is false")
        void disabledWhenInactive() {
            user.setIsActive(false);
            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("isAccountNonLocked() returns true when isActive is true")
        void nonLockedWhenActive() {
            assertThat(user.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("isAccountNonLocked() returns false when isActive is false")
        void lockedWhenInactive() {
            user.setIsActive(false);
            assertThat(user.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("isAccountNonExpired() always returns true")
        void accountNeverExpires() {
            assertThat(user.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isCredentialsNonExpired() always returns true")
        void credentialsNeverExpire() {
            assertThat(user.isCredentialsNonExpired()).isTrue();
        }
    }

    // -------------------------
    // Builder defaults
    // -------------------------

    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("rewardPoints defaults to 0")
        void rewardPointsDefaultsToZero() {
            User fresh = User.builder()
                    .firstName("A")
                    .lastName("B")
                    .email("a@b.com")
                    .passwordHash("x")
                    .role(Role.CITIZEN)
                    .build();
            assertThat(fresh.getRewardPoints()).isEqualTo(0);
        }

        @Test
        @DisplayName("isActive defaults to true")
        void isActiveDefaultsToTrue() {
            User fresh = User.builder()
                    .firstName("A")
                    .lastName("B")
                    .email("a@b.com")
                    .passwordHash("x")
                    .role(Role.CITIZEN)
                    .build();
            assertThat(fresh.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEmailVerified defaults to false")
        void isEmailVerifiedDefaultsToFalse() {
            User fresh = User.builder()
                    .firstName("A")
                    .lastName("B")
                    .email("a@b.com")
                    .passwordHash("x")
                    .role(Role.CITIZEN)
                    .build();
            assertThat(fresh.getIsEmailVerified()).isFalse();
        }
    }
}
