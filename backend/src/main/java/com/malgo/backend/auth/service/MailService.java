package com.malgo.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(
            String email,
            String verificationCode
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[Malgo] 이메일 인증번호 안내");
        message.setText(
                "이메일 인증번호는 "
                        + verificationCode
                        + "입니다.\n"
                        + "인증번호는 3분 동안 유효합니다."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetVerificationCode(
            String email,
            String verificationCode
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[Malgo] 비밀번호 재설정 인증번호");
        message.setText(
                "비밀번호 재설정 인증번호는 "
                        + verificationCode
                        + "입니다.\n"
                        + "인증번호는 3분 동안 유효합니다."
        );

        mailSender.send(message);
    }
}
