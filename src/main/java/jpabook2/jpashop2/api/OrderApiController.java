package jpabook2.jpashop2.api;

import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.Order;
import jpabook2.jpashop2.domain.OrderItem;
import jpabook2.jpashop2.domain.OrderStatus;
import jpabook2.jpashop2.repository.OrderRepository;
import jpabook2.jpashop2.repository.OrderSearch;
import jpabook2.jpashop2.repository.order.query.OrderFlatDto;
import jpabook2.jpashop2.repository.order.query.OrderItemQueryDto;
import jpabook2.jpashop2.repository.order.query.OrderQueryDto;
import jpabook2.jpashop2.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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
                .collect(toList());
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
                    .collect(toList());
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
                .collect(toList());
        return result;
    }

    // v3에서 페이징 가능한 버전
    //  - ToOne 관계만 우선 모두 페치 조인으로 최적화
    //  - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
    // spring.jpa.properties.hibernate.default_batch_fetch_size => select 문의 where 절에 in 커멘드가 들어감!
    // => Order 조회 시 결과 2개 => 컬렉션 (orders) 과 관련된 OrderItems를 미리 in 쿼리로 한번에 다 가져옴!
    // => batchsize : in 쿼리로 미리 땡겨올 개수
    // item => userA 주문 2개 + userB 주문 2개 = 총 4개 (원래는 4번 쿼리)
    // 옵션 설정 이후 한방에 4개 모두 땡겨옴!!

    // 결론 : 1+N+M ==> 1+1+1
    // + 매 쿼리마다 DB에서 에플리케이션으로 전송되는 데이터에 중복이 존재하지 않음
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        // xToOne 관계는 모두 페치 조인 ( : 이 관계는 데이터 row 수가 증가하지 않고 옆에 컬럼만 추가되기 때문 )
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }



    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    // 코드 양이 늘어남 / 데이터 select 양은 줄어듦
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimiaztion();
    }

    // v5 최적화 => 쿼리 1방으로 해결 / Order기준으로 페이징 불가능
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        // OrderFlatDto 의 스펙으로 반환
//        return orderQueryRepository.findAllByDto_flat();
        
        // OrderFlatDto 스펙의 리스트에서 직접 중복을 걸러서 OrderQueryDto 스펙으로 변환함!
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }
}
