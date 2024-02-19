package jpabook2.jpashop2.repository.order.simplequery;

import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.Order;
import jpabook2.jpashop2.domain.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;


// 원래는 SimpleOrderDto가 컨트롤러에 있었는데 리파지토리에서 클래스가 필요하기 때문에
// (리파지토리 -> 컨트롤러의 의존관계가 생기므로) 별도의 클래스 생성
@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;


    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }
}