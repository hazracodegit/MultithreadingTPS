package com.aicareer.taskprocessor.service;

import com.aicareer.taskprocessor.dto.AuthRequest;
import com.aicareer.taskprocessor.dto.AuthResponse;
import com.aicareer.taskprocessor.dto.RegisterRequest;
import com.aicareer.taskprocessor.entity.Role;
import com.aicareer.taskprocessor.entity.UserEntity;
import com.aicareer.taskprocessor.exception.ApiException;
import com.aicareer.taskprocessor.repository.UserRepository;
import com.aicareer.taskprocessor.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? Role.USER : request.role());
        userRepository.save(user);
        return tokenFor(user);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(UserEntity user) {
        return new AuthResponse(jwtUtil.generateToken(user.getUsername(), user.getRole()), user.getUsername(), user.getRole().name());
    }
}
