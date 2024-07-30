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

        filterChain.doFilter(request, response);
    }
}