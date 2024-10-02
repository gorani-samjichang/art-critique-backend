package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        System.out.println();
        System.out.println(requestURI + "에 대한 요청을 처리합니다");
        if (isPublicEndpoint(requestURI)) {
            System.out.println("public을 포함한 엔드포인트");
            filterChain.doFilter(request, response); // JWT 검사를 건너뛰고 다음 필터로 전달
            return;
        }

        String token = getJwtFromCookies(request.getCookies());

        if (token == null) {
            System.out.println("public을 포함하지 않는 엔드포인트가 토큰을 가지지 않음");
            filterChain.doFilter(request, response);
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT가 필요합니다.");
            return;
        }

        try {
            String email = jwtUtil.getEmail(token);
            Long uid = jwtUtil.getUid(token);
            String serialNumber = jwtUtil.getSerialNumber(token);
            String role = jwtUtil.getRole(token);

            MemberEntity memberEntity = MemberEntity.builder()
                    .email(email)
                    .uid(uid)
                    .serialNumber(serialNumber)
                    .role(role)
                    .build();

            CustomUserDetails customUserDetails = new CustomUserDetails(memberEntity);
            Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // @AuthenticationPrincipal 로 바꿀 필요가 있어보임
            request.setAttribute("email", email);
            System.out.println("public을 포함하지 않는 요청이 필터를 통과함!");
            filterChain.doFilter(request, response);
        } catch(Exception e) {
            System.out.println("public을 포함하지 않는 요청이 유효하지 않는 jwt를 갖고 있음");
            Cookie myCookie = new Cookie("Authorization", null);  // 쿠키 값을 null로 설정
            myCookie.setPath("/");
            myCookie.setHttpOnly(true);
            myCookie.setMaxAge(0);  // 남은 만료시간을 0으로 설정
            response.addCookie(myCookie);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "잘못된 jwt");
            return;
        }
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.contains("/public/") || requestURI.contains("/test/") || requestURI.equals("/api/custom-login");
    }
    private String getJwtFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) { // 쿠키 이름이 "Authorization"일 경우
                    return cookie.getValue(); // JWT 토큰 반환
                }
            }
        }
        return null; // JWT 쿠키가 없으면 null 반환
    }


//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String token = null;
//        Cookie[] list = request.getCookies();
//        if (list == null) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//        for(Cookie cookie : list) {
//            if(cookie.getName().equals("Authorization")) {
//                token = cookie.getValue();
//                break;
//            }
//        }
//        if (token == null) {
////            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            filterChain.doFilter(request, response);
//            return;
//        }
//        try {
//            String email = jwtUtil.getEmail(token);
//            Long uid = jwtUtil.getUid(token);
//            String serialNumber = jwtUtil.getSerialNumber(token);
//            String role = jwtUtil.getRole(token);
//
//            MemberEntity memberEntity = MemberEntity.builder()
//                    .email(email)
//                    .uid(uid)
//                    .serialNumber(serialNumber)
//                    .role(role)
//                    .build();
//
//            CustomUserDetails customUserDetails = new CustomUserDetails(memberEntity);
//            Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
//            SecurityContextHolder.getContext().setAuthentication(authToken);
//
//            // @AuthenticationPrincipal 로 바꿀 필요가 있어보임
//            request.setAttribute("email", email);
//        } catch(Exception e) {
//            Cookie myCookie = new Cookie("Authorization", null);  // 쿠키 값을 null로 설정
//            myCookie.setPath("/");
//            myCookie.setHttpOnly(true);
//            myCookie.setMaxAge(0);  // 남은 만료시간을 0으로 설정
//            response.addCookie(myCookie);
//        }
//        filterChain.doFilter(request, response);
//    }
}