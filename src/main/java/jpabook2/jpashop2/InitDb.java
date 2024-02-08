package jpabook2.jpashop2;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jpabook2.jpashop2.domain.*;
import jpabook2.jpashop2.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 총 주문 2개
 * userA
 *      JPA1 BOOK
 *      JPA2 BOOK
 * userB
 *      SPRING1 BOOK
 *      SPRING2 BOOK
 */
@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct                  // 스프링 빈이 모두 올라오고 나면 스프링이 호출해줌!
    public void init(){
        initService.dbInit1();      // 애플리케이션 로딩시점에 호출
        initService.dbInit2();
    }

    @Component  // 별도의 빈으로 등록!
    @Transactional
    @RequiredArgsConstructor
    // init()에서 호출하는 식 말고 아래의 클래스를 바로 @PostConstruct 하기에는
    // 스프링 라이프 사이클이 있어서 트랜잭션 어노테이션 붙이는 것이 힘듦
    // ==> 따라서 별도의 빈으로 등록해야 함!
    static class InitService {

        private final EntityManager em;
        public void dbInit1() {
            Member member = createMember("userA","서울","1","1111");
            em.persist(member);

            Book book1 = createBook("JPA1 BOOK", 10000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA2 BOOK", 20000, 100);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = createDelivery(member);

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        public void dbInit2() {
            Member member = createMember("userB", "진주", "2", "2222");
            em.persist(member);

            Book book1 = createBook("SPRING1 BOOK", 20000, 200);
            em.persist(book1);

            Book book2 = createBook("SPRING2 BOOK", 40000, 300);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 20000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 40000, 4);

            Delivery delivery = createDelivery(member);

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);

        }

        private static Member createMember(String name, String city, String street, String zipcode) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(new Address(city, street, zipcode));
            return member;
        }

        private static Book createBook(String name, int price, int stockQuantity) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(stockQuantity);
            return book1;
        }

        private static Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }
    }
}


