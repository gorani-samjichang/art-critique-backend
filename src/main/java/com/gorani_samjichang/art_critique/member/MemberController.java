package com.gorani_samjichang.art_critique.member;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.firebase.auth.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.JwtUtil;
import io.jsonwebtoken.Jwts;
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
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

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

    @Value("${token.verify.prefix}")
    String prefix;
    @Value("${twitter.consumer.key}")
    String twitter_key;
    @Value("${twitter.consumer.secret}")
    String twitter_secret;

    @GetMapping("is-logined")
    boolean is_logined() {
        return true;
    }
    @PostMapping("/public/member-join")
    HashMap<String, String> member_join(
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(value = "nickname") String nickname,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "profile", required = false) String profile,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        String email = email_token_validation(request);
        MemberEntity memberEntity = MemberEntity.builder()
                .email(email)
                .created_at(LocalDateTime.now())
                .credit(0)
                .nickname(nickname)
                .role("USER")
                .build();
        if (password == null) {
            memberEntity.setPassword(bCryptPasswordEncoder.encode(commonUtil.generateSecureRandomString(30)));
        } else {
            memberEntity.setPassword(bCryptPasswordEncoder.encode(password));
        }
        if (level != null) memberEntity.setLevel(level);
        if (profile != null) memberEntity.setLevel(profile);
        memberRepository.save(memberEntity);

        String token = jwtUtil.createJwt(email, memberEntity.getRole(), 7*24*60*60*1000L);
        register_cookie("Authorization", token, -1, response);

        HashMap<String, String> dto = new HashMap<>();
        dto.put("email", memberEntity.getEmail());
        dto.put("profile", memberEntity.getProfile());
        dto.put("role", memberEntity.getRole());
        dto.put("level", memberEntity.getLevel());
        dto.put("nickname", memberEntity.getNickname());
        return dto;
    }

    @GetMapping("/public/emailCheck/{email}")
    boolean email_check(@PathVariable String email) {
        return memberRepository.existsByEmail(email);
    }

    @GetMapping("/public/nicknameCheck/{nickname}")
    boolean nickname_check(@PathVariable String nickname) throws Exception {
        return memberRepository.existsByNickname(nickname);
    }

    @GetMapping("/info")
    HashMap<String, String> info(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        if (me == null) return null;
        HashMap<String, String> dto = new HashMap<>();
        dto.put("email", me.getEmail());
        dto.put("profile", me.getProfile());
        dto.put("role", me.getRole());
        dto.put("level", me.getLevel());
        dto.put("nickname", me.getNickname());
        return dto;
    }

    @GetMapping("credit")
    int credit(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        return me.getCredit();
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
    String get_level_list() {
        return "[{\"value\": \"newbie\",\"display\": \"입문\",\"color\": \"rgb(245,125,125)\"},{\"value\": \"chobo\",\"display\": \"초보\",\"color\": \"rgb(214, 189, 81)\"},{\"value\": \"intermediate\",\"display\": \"중수\",\"color\": \"rgb(82, 227, 159)\"},{\"value\": \"gosu\",\"display\": \"고수\",\"color\": \"rgb(70, 104, 227)\"}]";
    }

    String get_role(String email) {
        MemberEntity me = memberRepository.findByEmail(email);
        return (me == null) ? null : me.getRole();
    }

    @GetMapping("/public/oauth-login/google/{id_token}")
    boolean oauth_google_login(@PathVariable String id_token, HttpServletResponse response) throws UnsupportedEncodingException {
        GoogleIdToken.Payload payload =  webClientBuilder.build()
                .get()
                .uri("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + id_token)
                .retrieve()
                .bodyToMono(GoogleIdToken.Payload.class)
                .block();
        String email = null;
        if (payload != null) {
            email = payload.getEmail();
            String role = get_role(email);
            if (role == null) {
                return false;
            } else {
                String token = jwtUtil.createJwt(email, role, 7*24*60*60*1000L);
                register_cookie("Authorization", token, -1, response);
                return true;
            }
        } else {
            response.setStatus(401);
        }
        return false;
    }

    @GetMapping("/public/oauth-login/x/{access_token}/{token_secret}/{uid}")
    boolean oauth_x_login(@PathVariable String access_token, @PathVariable String token_secret, @PathVariable String uid, HttpServletResponse response) {

        try {
            validate_twitter_login(access_token, token_secret);
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            String email = (userRecord.getProviderData())[0].getEmail();

            if (!email.contains("@")) {
                email = email + "@twitter.com";
            }

            String role = get_role(email);
            if (role == null) {
                return false;
            } else {
                String token = jwtUtil.createJwt(email, role, 7*24*60*60*1000L);
                register_cookie("Authorization", token, -1, response);
                return true;
            }
        } catch (Exception e) {
            response.setStatus(401);
        }
        return false;
    }


    @GetMapping("/public/temp-token/{email}")
    void temp_token(@PathVariable String email, HttpServletResponse response) throws UnsupportedEncodingException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, FirebaseAuthException {
        if (email.startsWith(prefix)) {
            String [] split = email.split("@");
            if (split[1].equals("google")) {
                GoogleIdToken.Payload payload = webClientBuilder.build()
                        .get()
                        .uri("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + split[2])
                        .retrieve()
                        .bodyToMono(GoogleIdToken.Payload.class)
                        .block();
                email = payload.getEmail();
            } else if (split[1].equals("x")) {
                String access_token = split[2];
                String token_secret = split[3];
                String uid = split[4];
                validate_twitter_login(access_token, token_secret);
                UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
                String firebase_email = (userRecord.getProviderData())[0].getEmail();

                if (!email.contains("@")) {
                    firebase_email = firebase_email + "@twitter.com";
                }
                email = firebase_email;
            }
        }
        String token = jwtUtil.createEmailJwt(email, 60*60*1000L);
        register_cookie("token", token, -1, response);
    }

    String email_token_validation(HttpServletRequest request) {
        String token = null;
        Cookie[] list = request.getCookies();
        if (list == null) {
            return null;
        }
        for(Cookie cookie : list) {
            if(cookie.getName().equals("token")) {
                token = cookie.getValue();
                break;
            }
        }
        if (token == null) {
            return null;
        }
        return jwtUtil.getEmail(token);
    }

    void register_cookie(String key, String token, int max_age, HttpServletResponse response) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode( token, "UTF-8" );
        Cookie cookie = new Cookie( key, encodedValue);
        cookie.setMaxAge(max_age);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    void validate_twitter_login(String access_token, String token_secret) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(twitter_key, twitter_secret);
        consumer.setTokenWithSecret(access_token, token_secret);

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
