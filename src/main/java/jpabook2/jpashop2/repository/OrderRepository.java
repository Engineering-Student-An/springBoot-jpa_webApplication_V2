package jpabook2.jpashop2.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook2.jpashop2.domain.*;
import jpabook2.jpashop2.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id){
        return em.find(Order.class, id);
    }

    // jpql로 조회
    public List<Order> findAllByString(OrderSearch orderSearch) {


        String jpql = "select o from Order o join o.member m";

        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class) .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    // jpa criteria 로 조회
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class); Join<Order, Member>m=o.join("member", JoinType.INNER);//회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //최대 1000건
        return query.getResultList();
    }

    // QueryDSL 사용
    public List<Order> findAll(OrderSearch orderSearch) {
        JPAQueryFactory query = new JPAQueryFactory(em);
        QOrder order = QOrder.order;
        QMember member = QMember.member;

        // QueryDSL 사용 : JPQL로 바뀌어서 실행됨!
        return query.select(order)
                .from(order)
                .join(order.member, member)
                // 주문 상태가 똑같은지 확인 (null이면 where 절 안씀) => 동적쿼리
                // , : and
                .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
                .limit(1000)
                .fetch();
    }

    private static BooleanExpression nameLike(String memberName) {
        if(!StringUtils.hasText(memberName)) return null;
        return QMember.member.name.like(memberName);
    }

    private BooleanExpression statusEq(OrderStatus statusCond) {
        if(statusCond == null) {
            return null;
        }
        return QOrder.order.status.eq(statusCond);
    }


    public List<Order> findAllWithMemberDelivery() {
        // 한방 쿼리로 order, member, delivery를 조인한 다음에 select 절에 다 넣고 한번에 다 땡겨옴!
        // 이 경우에는 LAZY 무시하고 (프록시 아니고) 진짜 객체값을 모두 채워서 다 가져옴!
        // ==> 페치 조인
        return em.createQuery(
                "select distinct o from Order o" +
                        // distinct : 스프링부트 3버전 미만인 경우 조인 결과로 데이터 뻥튀기 방지용
                        // distinct => DB에서의 distinct는 한 줄의 데이터 모두 동일해야 중복 제거함
                        // JPA에서 자체적으로 Order를 가지고 올때 같은 Id 값이면 중복제거!
                        // 결론 : JPA의 distinct => DB에 distinct 키워드 넣어서 쿼리 보내고 + 루트(엔티티) 가 중복인 경우에 중복을 제거하고 컬렉션에 담아줌!
                        " join fetch o.member m" +     // order 조회할 때 객체 그래프로 member 까지 한번에 조회!
                        " join fetch o.delivery d", Order.class
        ).getResultList();
    }



    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select o from Order o" +
                " join fetch o.member m" +
                " join fetch o.delivery d" +
                " join fetch o.orderItems oi" +
                " join fetch oi.item i", Order.class)
                // 컬렉션 페치 조인 : 페이징 쿼리가 DB에 전송되지 않음!
                // (에러 로그) HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
                // 뜻 : 메모리에서 sorting 한다는 경고
                // 만약 데이터가 많으면 모두 애플리케이션으로 퍼올린 다음에 페이징 처리 => 메모리 초과 가능
//                .setFirstResult(1)
//                .setMaxResults(100)
                .getResultList();
    }

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {

        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +     // order 조회할 때 객체 그래프로 member 까지 한번에 조회!
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
