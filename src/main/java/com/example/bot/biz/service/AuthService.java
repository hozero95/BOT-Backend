package com.example.bot.biz.service;

import com.example.bot.biz.dto.JoinDTO;
import com.example.bot.biz.entity.User;
import com.example.bot.biz.repository.RefreshRepository;
import com.example.bot.biz.repository.UserRepository;
import com.example.bot.core.config.ResponseResult;
import com.example.bot.core.security.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, JwtUtil jwtUtil, RefreshRepository refreshRepository) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    /**
     * 회원 가입
     *
     * @param joinDTO p1
     */
    public void signup(JoinDTO joinDTO) {
        String email = joinDTO.getEmail();
        String password = joinDTO.getPassword();

        Boolean isExist = userRepository.existsByEmail(email);

        if (isExist) {
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setName(joinDTO.getName());
        user.setRole("ROLE_ADMIN");

        userRepository.save(user);
    }

    /**
     * Access Token 재발급
     *
     * @param request  p1
     * @param response p2
     * @return ResponseResult<?>
     */
    public ResponseResult<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        // get refresh token
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("Refresh-Token")) {
                refreshToken = cookie.getValue();
            }
        }

        // 토큰 유무 검증
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseResult.ofFailure(HttpStatus.BAD_REQUEST, "refresh token is empty");
        }

        // 토큰 만료 검증
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            return ResponseResult.ofFailure(HttpStatus.BAD_REQUEST, "refresh token is expired");
        }

        // Refresh Token 맞는지 확인
        String category = jwtUtil.getCategory(refreshToken);

        if (!category.equals("Refresh-Token")) {
            return ResponseResult.ofFailure(HttpStatus.BAD_REQUEST, "refresh token is invalid");
        }

        // DB에 저장되어 있는지 확인
        Boolean isExist = refreshRepository.existsByToken(refreshToken);
        if (!isExist) {
            return ResponseResult.ofFailure(HttpStatus.BAD_REQUEST, "refresh token is invalid");
        }

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccess = jwtUtil.createJwt("Access-Token", username, role, JwtUtil.ACCESS_TOKEN_EXPIRE);
        String newRefresh = jwtUtil.createJwt("Access-Token", username, role, JwtUtil.REFRESH_TOKEN_EXPIRE);

        // 기존 Refresh 토큰 삭제 및 신규 Refresh 토큰 저장
        refreshRepository.deleteByToken(refreshToken);
        jwtUtil.addRefreshToken(username, newRefresh, JwtUtil.REFRESH_TOKEN_EXPIRE);

        // Response
        response.setHeader("Access-Token", newAccess);
        response.addCookie(jwtUtil.createCookie(newRefresh));

        return ResponseResult.ofSuccess("success", null);
    }
}
