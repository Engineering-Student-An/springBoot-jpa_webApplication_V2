package jpabook2.jpashop2.repository.order.simplequery;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 리파지토리는 엔티티 조회 용도 + 성능최적화를 위한 페치 조인 정도로만 사용해야됨
// v4 (simple-order) 의 경우는 리파지토리에서 DTO 조회함 ( = api 스펙이 리파지토리에 들어감, 화면에 의존하는 조회 로직임)
// 따라서 새로운 패키지 (simplequery) 생성하고 새로운 리파지토리 생성!

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    public List<OrderSimpleQueryDto> findOrderDtos() {
        // JPA : 엔티티나 값 타입만 반환 가능 / DTO 는 반환 불가 => new 사용해야함!
        return em.createQuery("select new jpabook2.jpashop2.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        // OrderSimpleQueryDto에 원래처럼 Order를 넘길수 없음 (JPA에서는 엔티티를 넘길때 식별자를 넘김!)
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
