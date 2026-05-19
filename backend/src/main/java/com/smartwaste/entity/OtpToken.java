package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data                    // Getter + Setter + toString + equals
@NoArgsConstructor       // Default constructor
@AllArgsConstructor      // All args constructor
@Builder                 // Builder pattern
@Entity
@Table(name = "otp_tokens")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean used = false;    // Warning bhi fix ho gayi

    // -------------------------
    // Helper methods
    // -------------------------

    /** Returns true if the OTP has passed its expiry time. */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Returns true if the OTP is still usable — not used and not expired. */
    public boolean isValid() {
        return !used && !isExpired();
    }
}