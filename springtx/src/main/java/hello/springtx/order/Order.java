package hello.springtx.order;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders") // cf. order는 order by 문법 때문에 예약어이므로, 테이블 이름을 orders로 함
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private String username; // 정상, 예외, 잔고부족
    private String payStatus; // 대기, 완료
}
