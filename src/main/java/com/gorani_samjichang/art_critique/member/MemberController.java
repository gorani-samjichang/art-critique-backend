package com.gorani_samjichang.art_critique.member;

import com.google.firebase.auth.FirebaseAuthException;
import com.gorani_samjichang.art_critique.common.JwtUtil;
import com.gorani_samjichang.art_critique.common.exceptions.MessagingException;
import com.gorani_samjichang.art_critique.common.exceptions.UserNotValidException;
import com.gorani_samjichang.art_critique.common.exceptions.XUserNotFoundException;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.feedback.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    final CreditRepository creditRepository;
    final MemberService memberService;
    final MemberRepository memberRepository;
    final FeedbackService feedbackService;
    private final JwtUtil jwtUtil;

    @GetMapping("is-logined")
    boolean isLogined() {
        return true;
    }

    @PostMapping("/public/join")
    MemberDto memberJoin(
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(value = "nickname") String nickname,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "profile", required = false) MultipartFile profile,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        return memberService.memberJoin(password, nickname, level, profile, request, response);
    }

    @PostMapping("/edit")
    MemberDto memberEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "nickname") String nickname,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "profile", required = false) MultipartFile profile
    ) throws IOException {
        return memberService.memberEdit(userDetails, nickname, level, profile);
    }

    @GetMapping("/public/emailCheck/{email}")
    boolean emailCheck(@PathVariable String email, HttpServletResponse response) throws MessagingException,UnsupportedEncodingException {
        return memberService.emailCheck(email, response);
    }

    @GetMapping("/public/nicknameCheck/{nickname}")
    boolean nicknameCheck(@PathVariable String nickname) throws Exception {
        return memberService.nicknameCheck(nickname);
    }

    @GetMapping("/info")
    MemberDto info(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberService.memberEntityToDto(userDetails.memberEntity);
    }

    @GetMapping("credit")
//    ResponseEntity<Integer> credit(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        return new ResponseEntity<>(memberService.readCredit(userDetails), HttpStatus.OK);
//    }
   Integer credit(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberService.readCredit(userDetails);
    }
    @GetMapping("/public/logout")
    void logout(HttpServletResponse response) {
        memberService.logout(response);
    }

    @GetMapping("/public/levelList")
    String getLevelList() {
        return "[{\"value\": \"newbie\",\"display\": \"입문\",\"color\": \"rgb(245,125,125)\"},{\"value\": \"chobo\",\"display\": \"초보\",\"color\": \"rgb(214, 189, 81)\"},{\"value\": \"intermediate\",\"display\": \"중수\",\"color\": \"rgb(82, 227, 159)\"},{\"value\": \"gosu\",\"display\": \"고수\",\"color\": \"rgb(70, 104, 227)\"}]";
    }

    @GetMapping("/public/oauth-login/google/{idToken}")
    boolean oauthGoogleLogin(@PathVariable String idToken, HttpServletResponse response) throws UnsupportedEncodingException {
        return memberService.oauthGoogleLogin(idToken, response);
    }

    @GetMapping("/public/oauth-login/x/{accessToken}/{tokenSecret}/{uid}")
    boolean oauthXLogin(@PathVariable("accessToken") String accessToken,
                        @PathVariable("tokenSecret") String tokenSecret,
                        @PathVariable String uid,
                        HttpServletResponse response) {
        try {
            return memberService.oauthXLogin(accessToken, tokenSecret, uid, response);
        } catch (Exception e) {
            throw new XUserNotFoundException("User Not Found");
        }
    }


    @GetMapping("/public/temp-token/{email}/{code}")
    void tempToken(@PathVariable String email, HttpServletResponse response, HttpServletRequest request,@PathVariable String code) throws UnsupportedEncodingException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, FirebaseAuthException, UserNotValidException {
        memberService.tempToken(email, response, request, code);
    }


}