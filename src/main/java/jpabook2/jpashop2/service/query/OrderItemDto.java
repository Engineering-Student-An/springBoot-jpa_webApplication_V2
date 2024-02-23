package jpabook2.jpashop2.service.query;

import jpabook2.jpashop2.domain.OrderItem;
import lombok.Getter;

@Getter
public class OrderItemDto {

    private String itemName;    // 상품명
    private int orderPrice;     // 주문 가격
    private int count;          // 주문 수량

    public OrderItemDto(OrderItem orderItem) {
        itemName = orderItem.getItem().getName();
        orderPrice = orderItem.getOrderPrice();
        count = orderItem.getCount();
    }
}