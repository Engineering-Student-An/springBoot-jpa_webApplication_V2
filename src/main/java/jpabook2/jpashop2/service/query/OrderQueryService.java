package jpabook2.jpashop2.service.query;

import jpabook2.jpashop2.domain.Order;
import jpabook2.jpashop2.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

// OSIV off 시 LazyInitializationException 해결 => 새로운 서비스 단 생성

// 트랜잭션을 컨트롤러에서 보통 안 쓰기 때문에 서비스 계층에서 쿼리 전용 서비스 만듦
// (api에 맞는 변환 해결하는 별도의 쿼리 서비스 생성)
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }
}
