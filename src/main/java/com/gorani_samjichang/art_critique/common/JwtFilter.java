package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.common.JwtUtil;
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
import java.lang.reflect.Member;
import java.util.Arrays;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        Cookie[] list = request.getCookies();
        if (list == null) {
            filterChain.doFilter(request, response);
            return;
        }
        for(Cookie cookie : list) {
            if(cookie.getName().equals("Authorization")) {
                token = cookie.getValue();
                break;
            }
        }
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);
        MemberEntity memberEntity = MemberEntity.builder()
                .email(email)
                .password("temp")
                .role(role)
                .build();

        CustomUserDetails customUserDetails = new CustomUserDetails(memberEntity);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        request.setAttribute("email", email);

        filterChain.doFilter(request, response);
    }
}