package com.example.simpleapi.service;

import com.example.simpleapi.entity.Member;
import com.example.simpleapi.entity.MemberDto;
import com.example.simpleapi.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    @Autowired
    private MemberRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Member> findAll() {
        return repository.findAll();
    }

    public Optional<Member> findById(Long id) {
        return repository.findById(id);
    }

    public Member findByName(String name) {
        return repository.findByName(name);
    }

    public Member findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Member save(MemberDto member, long userid) {
        Member user = new Member();
        user.setName(member.getName());
        user.setEmail(member.getEmail());
        user.setPassword(passwordEncoder.encode(member.getPassword()));
        user.setGender(member.getGender());
        user.setKtpNumber(member.getKtpNumber());
        user.setPhoneNumber(member.getPhoneNumber());
        user.setBirthDate(member.getBirthDate());

        if (member.getRole().equals("ADMIN")) {
            user.setRole(member.getRole() + ",MEMBER");
        } else {
            user.setRole(member.getRole());
        }

        if (userid > 0) {
            user.setId(userid);
            user.setPassword(member.getPassword());
        }

        try {
            String encodedImage = Base64.getEncoder().encodeToString(member.getPhoto().getBytes());
            user.setPhoto(encodedImage);
            repository.save(user);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return user;
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Member> findUserContainsName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public String validateAuthentication(String username, String password) {
		if (username == null || password == null) {
			return "";
		}

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        Member user = findByName(username);

        if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return user.getRole();
        } else {
            user = findByEmail(username);

            if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
                return user.getRole();
            } else {
                return "";
            }
        }
    }

    public boolean emailExists(String email) {
        return repository.findByEmail(email) != null;
    }

    public MemberDto parseMemberToDto(Member inputMember) {
        MemberDto parsed = new MemberDto();

        parsed.setId(inputMember.getId());
        parsed.setName(inputMember.getName());
        parsed.setPassword(inputMember.getPassword());
        parsed.setEmail(inputMember.getEmail());
        parsed.setKtpNumber(inputMember.getKtpNumber());
        parsed.setPhoneNumber(inputMember.getPhoneNumber());
        parsed.setBirthDate(inputMember.getBirthDate());
        parsed.setGender(inputMember.getGender());
        parsed.setRole(inputMember.getRole());
        parsed.setPhoto(parsePhoto(inputMember.getName(), inputMember.getPhoto()));

        return parsed;
    }

    public MultipartFile parsePhoto(String fileName, String base64Input) {
        byte[] bytePhoto = Base64.getDecoder().decode(base64Input);
        MultipartFile filePhoto = new MockMultipartFile(fileName, fileName, "image/jpeg", bytePhoto);
        return filePhoto;
    }
}
