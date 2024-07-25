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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    final MemberRepository memberRepository;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final AuthenticationManagerBuilder authenticationManagerBuilder;
    final WebClient.Builder webClientBuilder;
    final JwtUtil jwtUtil;
    final CommonUtil commonUtil;
    final MemberService memberService;

    @Value("${token.verify.prefix}")
    String prefix;
    @Value("${twitter.consumer.key}")
    String twitterKey;
    @Value("${twitter.consumer.secret}")
    String twitterSecret;

    @GetMapping("is-logined")
    boolean isLogined() {
        return true;
    }

    @PostMapping("/public/member-join")
    HashMap<String, String> memberJoin(
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(value = "nickname") String nickname,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "profile", required = false) MultipartFile profile,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        String email = emailTokenValidation(request);
        MemberEntity memberEntity = MemberEntity.builder()
                .email(email)
                .createdAt(LocalDateTime.now())
                .credit(0)
                .nickname(nickname)
                .isDeleted(false)
                .role("USER")
                .build();

        String serialNumber = bCryptPasswordEncoder.encode(memberEntity.getEmail());
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
        memberRepository.save(memberEntity);

        String token = jwtUtil.createJwt(email, memberEntity.getRole(), 7 * 24 * 60 * 60 * 1000L);
        registerCookie("Authorization", token, -1, response);

        HashMap<String, String> dto = new HashMap<>();
        dto.put("email", memberEntity.getEmail());
        dto.put("profile", memberEntity.getProfile());
        dto.put("role", memberEntity.getRole());
        dto.put("level", memberEntity.getLevel());
        dto.put("nickname", memberEntity.getNickname());
        return dto;
    }

    @GetMapping("/public/emailCheck/{email}")
    boolean emailCheck(@PathVariable String email) {
        return memberService.emailCheck(email);
    }

    @GetMapping("/public/nicknameCheck/{nickname}")
    boolean nicknameCheck(@PathVariable String nickname) throws Exception {
        return memberService.nicknameCheck(nickname);
    }

    @GetMapping("/info")
    MemberDto readMember(HttpServletRequest request) {
        String email = request.getAttribute("email").toString();
        return memberService.readMember(email);
    }

    @GetMapping("credit")
    int credit(HttpServletRequest request) {
        String email = request.getAttribute("email").toString();
        return memberService.readCredit(email);
    }

    @GetMapping("/public/logout")
    void logout(HttpServletResponse response) {
        Cookie myCookie = new Cookie("Authorization", null);  // 쿠키 값을 null로 설정
        myCookie.setPath("/");
        myCookie.setHttpOnly(true);
        myCookie.setMaxAge(0);  // 남은 만료시간을 0으로 설정
        response.addCookie(myCookie);
    }

    @GetMapping("/public/levelList")
    String getLevelList() {
        return "[{\"value\": \"newbie\",\"display\": \"입문\",\"color\": \"rgb(245,125,125)\"},{\"value\": \"chobo\",\"display\": \"초보\",\"color\": \"rgb(214, 189, 81)\"},{\"value\": \"intermediate\",\"display\": \"중수\",\"color\": \"rgb(82, 227, 159)\"},{\"value\": \"gosu\",\"display\": \"고수\",\"color\": \"rgb(70, 104, 227)\"}]";
    }

    String getRole(String email) {
        MemberEntity me = memberRepository.findByEmail(email);
        return (me == null) ? null : me.getRole();
    }

    @GetMapping("/public/oauth-login/google/{idToken}")
    boolean oauthGoogleLogin(@PathVariable String idToken, HttpServletResponse response) throws UnsupportedEncodingException {
        GoogleIdToken.Payload payload = webClientBuilder.build()
                .get()
                .uri("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + idToken)
                .retrieve()
                .bodyToMono(GoogleIdToken.Payload.class)
                .block();
        String email = null;
        if (payload != null) {
            email = payload.getEmail();
            String role = getRole(email);
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

    @GetMapping("/public/oauth-login/x/{accessToken}/{tokenSecret}/{uid}")
    boolean oauthXLogin(@PathVariable("accessToken") String accessToken,
                        @PathVariable("tokenSecret") String tokenSecret,
                        @PathVariable String uid,
                        HttpServletResponse response) {

        try {
            validateTwitterLogin(accessToken, tokenSecret);
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            String email = (userRecord.getProviderData())[0].getEmail();

            if (!email.contains("@")) {
                email = email + "@twitter.com";
            }

            String role = getRole(email);
            if (role == null) {
                return false;
            } else {
                String token = jwtUtil.createJwt(email, role, 7 * 24 * 60 * 60 * 1000L);
                registerCookie("Authorization", token, -1, response);
                return true;
            }
        } catch (Exception e) {
            response.setStatus(401);
        }
        return false;
    }


    @GetMapping("/public/temp-token/{email}")
    void tempToken(@PathVariable String email, HttpServletResponse response) throws UnsupportedEncodingException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, FirebaseAuthException {
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

    String emailTokenValidation(HttpServletRequest request) {
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

    void registerCookie(String key, String token, int maxAge, HttpServletResponse response) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(token, "UTF-8");
        Cookie cookie = new Cookie(key, encodedValue);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    void validateTwitterLogin(String accessToken, String tokenSecret) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
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
}
