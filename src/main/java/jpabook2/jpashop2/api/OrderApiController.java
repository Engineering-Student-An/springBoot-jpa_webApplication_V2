package jpabook2.jpashop2.api;

import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.Order;
import jpabook2.jpashop2.domain.OrderItem;
import jpabook2.jpashop2.domain.OrderStatus;
import jpabook2.jpashop2.repository.OrderRepository;
import jpabook2.jpashop2.repository.OrderSearch;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            // 프록시 객체 강제 초기화 (Hibernate5 모듈 : 레이지로딩 => 초기화 된 프록시 객체만 끌고옴)
            order.getMember().getName();
            order.getDelivery().getAddress();

            // OrderItem 프록시 초기화
            List<OrderItem> orderItems = order.getOrderItems();

            // OrderItem 내부의 Item 프록시 초기화
//            for (OrderItem orderItem : orderItems) {
//                orderItem.getItem().getName();
//            }
            orderItems.stream().forEach(o -> o.getItem().getName());    // 위 코드와 동일 (람다식)
        }

        return all;
    }


    // 쿼리 개수
    // 1. 오더 조회 => 결과 2개
    // 2. 첫번째 order => 멤버 조회, delivery 조회, orderItems 조회 (orderItems 결과 2개 => item 조회 2번)
    // 3. 두번째 order => "
    // 결과 : 쿼리 엄청 많이 나감!
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // orders를 DTO로 변환 과정
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
//        private List<OrderItem> orderItems;     // DTO 안에 엔티티가 존재하면 안됨! (매핑도 안됨)
                                                // 엔티티가 외부에 노출되기 때문에 (=> 엔티티에 대한 의존을 완전히 끊어야됨!)
                                                // ==> OrderItem 조차도 DTO로 모두 변환해야함!!!
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName());
//            orderItems = order.getOrderItems();

            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }


    @Getter
    static class OrderItemDto {     // OrderItem 을 DTO로 변환

        private String itemName;    // 상품명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }


    // orderRepository에서 페치 조인 해준것 제외하고는 v2와 코드 모두 동일
    // 그러나 쿼리는 엄청 줄어들었음!
    // 치명적 단점 : 페이징 불가능
    // cf) 컬렉션 페치 조인은 1개만 사용하자 (데이터가 부정합하게 조회될 수 있으므로)
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        // 조인 결과 => 데이터 수가 2배로 뻥튀기 됨!
        // ==> 스프링부트 3버전 부터는 Hibernate 6 버전 사용 => Hibernate 6 버전 : 자동 distinct 적용 => 페치 조인 사용 시 자동으로 중복제거됨!
        // (같은 주문서에 orderItem이 2개이기 때문에 orderId로 조인 결과 동일 orderId를 갖는 두개의 데이터로 조인됨)
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }
}
