package com.example.simpleapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.simpleapi.entity.Member;
import com.example.simpleapi.repository.MemberRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
// @Rollback(false)
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void testCreateMember() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Member newMember = new Member();
        newMember.setName("UnitTest");
        newMember.setEmail("unit@test.com");
        newMember.setBirthDate(LocalDate.parse("2000-11-01"));
        newMember.setPassword(encoder.encode("test"));
        newMember.setPhoneNumber("08123456789");
        newMember.setKtpNumber("123456789");
        newMember.setGender("Male");
        newMember.setPhoto(Base64.getEncoder().encodeToString(new byte[]{1,2,3}));
        newMember.setRole("ADMIN");

        Member savedUser = memberRepository.save(newMember);

        Member existUser = testEntityManager.find(Member.class, savedUser.getId());

        assertEquals(savedUser, existUser);
    }
}
