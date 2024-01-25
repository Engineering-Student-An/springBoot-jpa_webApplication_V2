package jpabook2.jpashop2.service;

import jpabook2.jpashop2.domain.Member;
import jpabook2.jpashop2.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    @Test
    public void 회원가입() throws Exception {
        //given
        Member member = new Member();
        member.setName("회원1");

        //when
        Long id = memberService.join(member);

        //then
        Assertions.assertEquals(member, memberRepository.findOne(id));
    }

    @Test // (expected = IllegalStateException.class) << JUnit4 에서 가능
    public void 중복_회원_예외() throws Exception {
        //given
        Member member1 = new Member();
        member1.setName("user1");

        Member member2 = new Member();
        member2.setName("user1");

        //when
        memberService.join(member1);
        try {
            memberService.join(member2);
        } catch (IllegalStateException e){
            return;
        }

        //then
        fail("예외가 발생해야 합니다.");
    }

}