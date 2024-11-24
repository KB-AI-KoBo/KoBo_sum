package com.kb.kobo.user.service;

import com.kb.kobo.user.dto.UserInfoDto;
import com.kb.kobo.user.dto.UserLoginReqDto;
import com.kb.kobo.user.domain.User;
import com.kb.kobo.user.repository.UserRepository;
import com.kb.kobo.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User signUp(UserInfoDto userInfoDto) {
        checkUserDuplication(userInfoDto.getUsername(), userInfoDto.getEmail());
        User user = createUserFromDto(userInfoDto);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(Long id, UserInfoDto updatedUserInfoDto) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            updateUserFromDto(user, updatedUserInfoDto);
            return userRepository.save(user);
        } else {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + id);
        }
    }

    @Transactional
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public Optional<User> findByRegistrationNumber(String registrationNumber) {
        return userRepository.findByRegistrationNumber(registrationNumber);
    }

    @Transactional
    public Optional<User> findByCompanyEmail(String companyEmail) {
        return userRepository.findByCompanyEmail(companyEmail);
    }

    @Transactional
    public boolean login(UserLoginReqDto userLoginReqDto) {
        Optional<User> userOptional = userRepository.findByUsername(userLoginReqDto.getUsername());
        return userOptional.isPresent() && passwordEncoder.matches(userLoginReqDto.getPassword(), userOptional.get().getPassword());
    }

    @Transactional
    public String authenticate(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            return jwtUtil.generateToken(email); // JWT 토큰 생성
        }
        throw new RuntimeException("사용자 이름 또는 비밀번호가 잘못되었습니다.");
    }


    @Transactional
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다."));
        }
        throw new RuntimeException("인증된 사용자가 없습니다.");
    }

    private void checkUserDuplication(String username, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("사용자 이름이 이미 존재합니다.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("이메일이 이미 존재합니다.");
        }
    }

    private User createUserFromDto(UserInfoDto userInfoDto) {
        String encodedPassword = encodePassword(userInfoDto.getPassword());
        return new User(
                userInfoDto.getUsername(),
                encodedPassword,
                userInfoDto.getEmail(),
                userInfoDto.getCompanyName(),
                userInfoDto.getCompanySize(),
                userInfoDto.getRegistrationNumber(),
                userInfoDto.getCompanyEmail(),
                userInfoDto.getIndustry(),
                LocalDateTime.now()
        );
    }

    private void updateUserFromDto(User user, UserInfoDto userInfoDto) {
        user.setEmail(userInfoDto.getEmail());
        user.setCompanyName(userInfoDto.getCompanyName());
        user.setCompanySize(userInfoDto.getCompanySize());
        user.setRegistrationNumber(userInfoDto.getRegistrationNumber());
        user.setCompanyEmail(userInfoDto.getCompanyEmail());
        user.setIndustry(userInfoDto.getIndustry());
        user.setUsername(userInfoDto.getUsername());
        if (userInfoDto.getPassword() != null && !userInfoDto.getPassword().isEmpty()) {
            user.setPassword(encodePassword(userInfoDto.getPassword()));
        }
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Transactional
    public void deleteUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        userRepository.delete(user);
    }
}
