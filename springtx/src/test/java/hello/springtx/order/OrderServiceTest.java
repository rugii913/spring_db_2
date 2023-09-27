package hello.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    void complete() throws NotEnoughMoneyException {
        // Given
        Order order = new Order();
        order.setUsername("정상");

        // When
        orderService.order(order);

        // Then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("완료");
    }

    @Test
    void runtimeException() {
        // Given
        Order order = new Order();
        order.setUsername("예외");

        // When
        assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        // Then
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }
    
    @Test
    void bizException() {
        // Given
        Order order = new Order();
        order.setUsername("잔고부족");

        // When
        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }

        // Then
        Order findOrder = orderRepository.findById(order.getId()).get(); // cf. 원래 Optional에서 get()으로 꺼내는 게 좋은 방식은 아니지만 테스트이므로 간단하게 작성
        assertThat(findOrder.getPayStatus()).isEqualTo("대기");
    }
}
