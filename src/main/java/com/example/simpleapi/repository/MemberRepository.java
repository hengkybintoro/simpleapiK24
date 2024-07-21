package com.example.simpleapi.repository;

import com.example.simpleapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByName(String name);
    Member findByEmail(String email);
    List<Member> findByNameContainingIgnoreCase(String name);
}
