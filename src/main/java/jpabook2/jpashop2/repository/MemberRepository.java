package jpabook2.jpashop2.repository;

import jpabook2.jpashop2.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


// Spring Data JPA 로 바꿈
public interface MemberRepository extends JpaRepository<Member, Long> {

    // select m from Member m where m.name = :name
    // 위의 쿼리 자동 작성
    List<Member> findByName(String name);
}
