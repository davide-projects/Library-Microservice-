package com.apulia.memberservice.service;

import com.apulia.memberservice.exception.MemberNotFoundException;
import com.apulia.memberservice.model.Member;
import com.apulia.memberservice.repository.MemberRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Bulkhead(name = "memberService")
    @RateLimiter(name = "memberService")
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Bulkhead(name = "memberService")
    @RateLimiter(name = "memberService")
    public Member getMemberById(Integer id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("Member with ID " + id + " not found"));
    }

    @Bulkhead(name = "memberService")
    @RateLimiter(name = "memberService")
    public Member createMember(Member member) {
        if (memberRepository.existsByPhone(member.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + member.getPhone());
        }
        return memberRepository.save(member);
    }

    @Bulkhead(name = "memberService")
    @RateLimiter(name = "memberService")
    public Member updateMember(Integer id, Member updatedMember) {
        Member existing = getMemberById(id);

        if (!existing.getPhone().equals(updatedMember.getPhone())
                && memberRepository.existsByPhone(updatedMember.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + updatedMember.getPhone());
        }

        existing.setFirstName(updatedMember.getFirstName());
        existing.setLastName(updatedMember.getLastName());
        existing.setCity(updatedMember.getCity());
        existing.setPhone(updatedMember.getPhone());

        return memberRepository.save(existing);
    }

    @Bulkhead(name = "memberService")
    @RateLimiter(name = "memberService")
    public void deleteMember(Integer id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException("Member with ID " + id + " not found");
        }
        memberRepository.deleteById(id);
    }
}
