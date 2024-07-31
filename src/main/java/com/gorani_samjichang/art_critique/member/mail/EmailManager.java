package com.gorani_samjichang.art_critique.member.mail;

import com.gorani_samjichang.art_critique.appConstant.EmailTemplate;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.JwtUtil;
import com.gorani_samjichang.art_critique.common.exceptions.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class EmailManager {

    private static final int CODE_LENGTH = 8;
    private static final String URL = "http://localhost:9100/";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private CommonUtil commonUtil;
    @Autowired
    private JavaMailSender mailSender;
    @Value("${Spring.mail.salt}")
    String salt;
    JwtUtil jwtUtil;

    public boolean validCode(String code, String email) {
        try {
            if (!jwtUtil.getEmail(code).equals(email)) {
                return false;
            }
            if (jwtUtil.isExpired(code)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public String createCode(String email) {
        return jwtUtil.createEmailRandomJwt(email, 1800000L, commonUtil.generateSecureRandomString(8));
    }

    public void sendVerifyingMessage(String to) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        String code = createCode(to);
        try {
            helper.setTo(to);
            String link = String.format("%sverify?code=%s", URL, code);
            helper.setText(String.format(EmailTemplate.WELCOME.getTemplate(), link));
            helper.setSubject(EmailTemplate.WELCOME.getSubject());
            mailSender.send(message);
        } catch (Exception e) {
            throw new MessagingException(e.getMessage());
        }
    }
}
