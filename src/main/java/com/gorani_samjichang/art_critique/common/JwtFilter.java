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
        if (isPublicEndpoint(requestURI)) {
            System.out.println(1);
            filterChain.doFilter(request, response); // JWT 검사를 건너뛰고 다음 필터로 전달
            return;
        }

        Cookie[] list = request.getCookies();
        if (list == null) {
            System.out.println(2);
            filterChain.doFilter(request, response);
            return;
        }

        String token = getJwtFromCookies(request.getCookies());

        if (token == null) {
            System.out.println(3);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT가 필요합니다.");
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
        } catch(Exception e) {
            System.out.println(4);
            Cookie myCookie = new Cookie("Authorization", null);  // 쿠키 값을 null로 설정
            myCookie.setPath("/");
            myCookie.setHttpOnly(true);
            myCookie.setMaxAge(0);  // 남은 만료시간을 0으로 설정
            response.addCookie(myCookie);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "잘못된 jwt");
            return;
        }
        System.out.println(5);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.contains("/public/");
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
}