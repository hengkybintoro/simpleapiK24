package com.example.simpleapi.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.simpleapi.entity.Member;
import com.example.simpleapi.repository.MemberRepository;

@Service
public class CustomUserDetailService implements UserDetailsService {
    private final MemberRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomUserDetailService(MemberRepository userRepository) {
        this.userRepository = userRepository;
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member user = userRepository.findByEmail(email);

        // Hardcoded admin account
        if (email.equals("defaultadmin")) {
            return org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password(passwordEncoder.encode(email))
                .roles("ADMIN", "MEMBER")
                .build();
        }

        if (user != null) {
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(parseRole(user.getRole()))
                .build();
        } else {
            throw new UsernameNotFoundException("Invalid username or password.");
        }
    }
    
    private String[] parseRole(String roles) {
        String[] result = roles.split(",");

        return result;
    }
}
