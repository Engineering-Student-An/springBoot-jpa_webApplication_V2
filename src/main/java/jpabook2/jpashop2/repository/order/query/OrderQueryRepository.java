package jpabook2.jpashop2.repository.order.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    // OrderApiController 내부의 OrderDto가 아닌 OrderQueryDto 를 새로 생성한 이유
    // : OrderDto를 사용하면 repository -> controller 방향으로 의존관계가 생기므로 + 같은 패키지
    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();

        // 컬렉션에 해당하는 일대다 의 데이터 채워넣기
        // 결과적으로 N+1 문제 발생 (findOrders() 실행 쿼리 1번 (=> 결과 2개) + 루프 돌면서 2번(N번)쿼리 발생)
        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });

        return result;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                        "select new jpabook2.jpashop2.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                        "select new jpabook2.jpashop2.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    // v4는 루프를 돌려서 orderItem을 가져왔지만 v5는 한번에 가져옴!
    // 쿼리 총 2번 (findOrders 1번 + findOrderItemMap -> orderItems 조회 쿼리 1번)
    public List<OrderQueryDto> findAllByDto_optimiaztion() {
        // 루트 조회
        List<OrderQueryDto> result = findOrders();

        // result의 orderId를 리스트로 다 뽑음 = orderIds
        List<Long> orderIds = toOrderIds(result);

        // orderItem (컬렉션) 조회
        // 루트 (order) 데이터 만큼 한방에 맵으로 메모리에 올림
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        // 키(orderId)로 찾아서 setOrderItems 해줌
        // result에서 모자랐던 컬렉션 데이터를 채워주는 역할
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new jpabook2.jpashop2.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                " from OrderItem oi" +
                                " join oi.item i" +
                                // orderId 한 개씩 가져오는 것이 아닌 in 절로 한번에 가져옴
                                " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        // orderItems를 그냥 써도 되지만 코드작성 쉽게 + 최적화 위해서 맵으로 바꿈
        // orderId를 기준으로 map 으로 변환
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId()));
        return orderItemMap;
    }

    private static List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());
    }




    // 다 join 함
    // 중복 제거 하지 못하고 ( = 데이터 뻥튀기) 모두 반환됨
    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                "select new jpabook2.jpashop2.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d" +
                        " join o.orderItems oi" +
                        " join oi.item i", OrderFlatDto.class)
                .getResultList();
    }
}
