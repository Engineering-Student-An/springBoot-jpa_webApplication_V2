package jpabook2.jpashop2.api;

import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.Order;
import jpabook2.jpashop2.domain.OrderStatus;
import jpabook2.jpashop2.repository.OrderRepository;
import jpabook2.jpashop2.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne 관계)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
//        return all;
        // => Order 에서 모든 양방향 연관관계에 @JsonIgnore ==> 무한루프에서 벗어남
        // => bytebuddy : 지연로딩 -> 실제 객체가 아닌 프록시 객체를 가져옴 (여기서 프록시 객체 : byteBuddyInterceptor
        //                                                          ex ) private Member member = new ByteBuddyInterceptor(); )
        // 이때 프록시 객체를 json으로 변환하는 과정에서 에러 발생
        // 잭슨 라이브러리에게 프록시 객체는 뿌리지 않도록 해야함 => Hibernate5 모듈 설치!
        // member, orderitems, delivery 모두 null 임 (이유 : 지연로딩 -> DB에서 조회한 것 아님 -> Hibernate5 : 지연 로딩 모두 무시!)

        // FORCE_LAZY_LOADING 옵션 끄고 원하는 지연로딩만 출력하는 방법 => Hibernate5 모듈은 초기화 된 프록시 객체만 노출!
        // 강제로 지연로딩 끌고옴
        // (order.getMember() 까지는 프록시 객체 / .getName() 하는 순간 강제 초기화됨!)
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getStatus();
        }
        return all;
    }
    // api 만들때 위와 같이 복잡하게 안 만듦 ( ex. member의 이름만 필요하지 member의 주소, id등은 필요 없다는 뜻) => 쓸데없는 데이터가 노출됨!
    // => 가급적이면 필요한 데이터만 api 스펙에 노출하는게 좋음!!!!

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    // v1, v2 모두 조회 쿼리가 많이 나가는 문제 발생 !
    // 쿼리로 조회 순 ( order -> sql 1번 -> 결과 주문수 2개
    // -> 루프 2바퀴 -> 첫번째 주문서의 member 조회 -> 첫번째 주문서의 delivery 조회
    //              -> 두번째 주문서의 " -> 두번째 주문서의 "
    // ==> 총 쿼리 5번 나감! => n+1 문제 발생! ( = 1 + member N + delivery N = 1+N+N)
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {    // DTO가 엔티티를 파라미터로 받는것은 크게 문제되지 않음!
            orderId = order.getId();
            name = order.getMember().getName();     // LAZY 초기화 (memberId를 가지고 영속성 컨텍스트 찾아봄 => 없으니깐 DB에서 끌고옴) => member 조회 쿼리
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            // 회원 주소가 아닌 배송지 주소
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }


    // v2 에서 성능 최적화 (페치 조인) 한 버전 => v2와 v3는 결과적으로 똑같지만 나가는 쿼리가 다름 => 쿼리가 단 1개만 나감!
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();   // 페치 조인
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }


}
