package com.smartwaste.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    // -------------------------
    // sendOtp tests
    // -------------------------

    @Test
    void sendOtp_shouldSendEmailToCorrectRecipient() {
        emailService.sendOtp("user@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
    }

    @Test
    void sendOtp_shouldIncludeOtpInEmailBody() {
        emailService.sendOtp("user@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("654321");
    }

    @Test
    void sendOtp_shouldHaveCorrectSubject() {
        emailService.sendOtp("user@example.com", "000000");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject()).isEqualTo("SmartWaste - OTP Verification");
    }

    @Test
    void sendOtp_shouldMentionValidityPeriodInBody() {
        emailService.sendOtp("user@example.com", "111111");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("5 minutes");
    }

    // -------------------------
    // sendPasswordResetLink tests
    // -------------------------

    @Test
    void sendPasswordResetLink_shouldSendEmailToCorrectRecipient() {
        emailService.sendPasswordResetLink("user@example.com", "https://example.com/reset?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
    }

    @Test
    void sendPasswordResetLink_shouldIncludeResetLinkInBody() {
        String resetLink = "https://example.com/reset?token=xyz123";
        emailService.sendPasswordResetLink("user@example.com", resetLink);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains(resetLink);
    }

    @Test
    void sendPasswordResetLink_shouldHaveCorrectSubject() {
        emailService.sendPasswordResetLink("user@example.com", "https://example.com/reset?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject()).isEqualTo("SmartWaste - Password Reset Request");
    }

    @Test
    void sendPasswordResetLink_shouldMentionValidityPeriodInBody() {
        emailService.sendPasswordResetLink("user@example.com", "https://example.com/reset?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("15 minutes");
    }

    @Test
    void sendPasswordResetLink_shouldCallMailSenderExactlyOnce() {
        emailService.sendPasswordResetLink("user@example.com", "https://example.com/reset?token=abc");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
