package jpabook2.jpashop2.repository.order.query;

import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(of = "orderId")  // v6의 groupingBy => orderId를 기준으로 묶어주게 설정
public class OrderQueryDto {

    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;
    private List<OrderItemQueryDto> orderItems;


    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
//        this.orderItems = orderItems;     // 생성자에서 컬렉션 제외 : (OrderQueryRepository -> findOrders 내부의 쿼리 작성 시)
                                            // new 해서 jpql 을 짜더라도 컬렉션을 바로 넣을 순 없음

    }


    // 컬렉션까지 모두 초기화하는 생성자 v6 (orders)
    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, List<OrderItemQueryDto> orderItems) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
        this.orderItems = orderItems;
    }
}
