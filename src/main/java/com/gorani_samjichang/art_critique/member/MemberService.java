package com.gorani_samjichang.art_critique.member;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final JwtUtil jwtUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final AuthenticationManagerBuilder authenticationManagerBuilder;
    final WebClient.Builder webClientBuilder;
    final CommonUtil commonUtil;
    private final MemberManager memberManager;

    @Value("${token.verify.prefix}")
    String prefix;
    @Value("${twitter.consumer.key}")
    String twitterKey;
    @Value("${twitter.consumer.secret}")
    String twitterSecret;

    public boolean emailCheck(String email) {
        return memberManager.existsByEmail(email);
    }

    public boolean nicknameCheck(String nickname) {
        return memberManager.existsByNickname(nickname);
    }

    public MemberDto readMember(String email) {
        return memberEntityToDto(memberManager.findValidMember(email));
    }

    public int readCredit(String email) {
        return memberManager.findValidMember(email).getCredit();
    }

    public String readRole(String email) {
        return memberManager.findValidMember(email).getRole();
    }

    private String emailTokenValidation(HttpServletRequest request) {
        String token = null;
        Cookie[] list = request.getCookies();
        if (list == null) {
            return null;
        }
        for (Cookie cookie : list) {
            if (cookie.getName().equals("token")) {
                token = cookie.getValue();
                break;
            }
        }
        if (token == null) {
            return null;
        }
        return jwtUtil.getEmail(token);
    }

    public MemberDto memberEntityToDto(MemberEntity memberEntity) {
        return MemberDto.builder()
                .email(memberEntity.getEmail())
                .profile(memberEntity.getProfile())
                .role(memberEntity.getRole())
                .level(memberEntity.getLevel())
                .nickname(memberEntity.getNickname())
                .build();
    }

    public void logout(HttpServletResponse response) {
        Cookie myCookie = new Cookie("Authorization", null);  // 쿠키 값을 null로 설정
        myCookie.setPath("/");
        myCookie.setHttpOnly(true);
        myCookie.setMaxAge(0);  // 남은 만료시간을 0으로 설정
        response.addCookie(myCookie);
    }


    void registerCookie(String key, String token, int maxAge, HttpServletResponse response) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(token, "UTF-8");
        Cookie cookie = new Cookie(key, encodedValue);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }


    private void validateTwitterLogin(String accessToken, String tokenSecret) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(twitterKey, twitterSecret);
        consumer.setTokenWithSecret(accessToken, tokenSecret);

        URI uri = URI.create("https://api.twitter.com/1.1/account/verify_credentials.json");
        HttpGet request = new HttpGet(uri);

        consumer.sign(request);

        HttpHeaders headers = new HttpHeaders();
        for (org.apache.http.Header header : request.getAllHeaders()) {
            headers.add(header.getName(), header.getValue());
        }

        webClientBuilder.build()
                .get()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public MemberDto memberJoin(
            String password,
            String nickname,
            String level,
            MultipartFile profile,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String email = emailTokenValidation(request);
        MemberEntity memberEntity = MemberEntity.builder()
                .email(email)
                .createdAt(LocalDateTime.now())
                .credit(1)
                .nickname(nickname)
                .isDeleted(false)
                .role("USER")
                .build();

        String serialNumber = UUID.randomUUID().toString();
        memberEntity.setSerialNumber(serialNumber);
        if (password == null) {
            memberEntity.setPassword(bCryptPasswordEncoder.encode(commonUtil.generateSecureRandomString(30)));
        } else {
            memberEntity.setPassword(bCryptPasswordEncoder.encode(password));
        }
        if (level != null) memberEntity.setLevel(level);
        if (profile != null) {
            String profile_url = commonUtil.uploadToStorage(profile, serialNumber);
            memberEntity.setProfile(profile_url);
        }
        memberManager.save(memberEntity);

        String token = jwtUtil.createJwt(email, memberEntity.getRole(), 7 * 24 * 60 * 60 * 1000L);
        registerCookie("Authorization", token, -1, response);

        return memberEntityToDto(memberEntity);
    }

    public MemberDto memberEdit(
            String nickname,
            String level,
            MultipartFile profile,
            HttpServletRequest request
    ) throws IOException {
        String email = request.getAttribute("email").toString();
        MemberEntity memberEntity = memberManager.findValidMember(email);
        memberEntity.setNickname(nickname);
        memberEntity.setLevel(level);
        if (profile != null) {
            String profile_url = commonUtil.uploadToStorage(profile, memberEntity.getSerialNumber());
            memberEntity.setProfile(profile_url);
        } else {
            memberEntity.setProfile(null);
        }
        memberManager.save(memberEntity);

        return memberEntityToDto(memberEntity);
    }

    public boolean oauthGoogleLogin(String idToken, HttpServletResponse response) throws UnsupportedEncodingException {
        GoogleIdToken.Payload payload = webClientBuilder.build()
                .get()
                .uri("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + idToken)
                .retrieve()
                .bodyToMono(GoogleIdToken.Payload.class)
                .block();
        String email = null;
        if (payload != null) {
            email = payload.getEmail();
            String role = readRole(email);
            if (role == null) {
                return false;
            } else {
                String token = jwtUtil.createJwt(email, role, 7 * 24 * 60 * 60 * 1000L);
                registerCookie("Authorization", token, -1, response);
                return true;
            }
        } else {
            response.setStatus(401);
        }
        return false;
    }

    public boolean oauthXLogin(String accessToken, String tokenSecret, String uid, HttpServletResponse response) throws Exception {
        validateTwitterLogin(accessToken, tokenSecret);
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        String email = (userRecord.getProviderData())[0].getEmail();

        if (!email.contains("@")) {
            email = email + "@twitter.com";
        }

        String role = readRole(email);
        if (role == null) {
            return false;
        } else {
            String token = jwtUtil.createJwt(email, role, 7 * 24 * 60 * 60 * 1000L);
            registerCookie("Authorization", token, -1, response);
            return true;
        }
    }

    public void tempToken(String email, HttpServletResponse response) throws UnsupportedEncodingException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, FirebaseAuthException {
        if (email.startsWith(prefix)) {
            String[] split = email.split("@");
            if (split[1].equals("google")) {
                GoogleIdToken.Payload payload = webClientBuilder.build()
                        .get()
                        .uri("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + split[2])
                        .retrieve()
                        .bodyToMono(GoogleIdToken.Payload.class)
                        .block();
                email = payload.getEmail();
            } else if (split[1].equals("x")) {
                String accessToken = split[2];
                String tokenSecret = split[3];
                String uid = split[4];
                validateTwitterLogin(accessToken, tokenSecret);
                UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
                String firebaseEmail = (userRecord.getProviderData())[0].getEmail();

                if (!email.contains("@")) {
                    firebaseEmail = firebaseEmail + "@twitter.com";
                }
                email = firebaseEmail;
            }
        }
        String token = jwtUtil.createEmailJwt(email, 60 * 60 * 1000L);
        registerCookie("token", token, -1, response);
    }
}
