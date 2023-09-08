package hello.itemservice.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
// @Table(name = "item") // 객체 이름과 같으면 @Table 생략 가능
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(name = "item_name", length = 10) // 카멜 케이스를 스네이크 케이스로 자동 변환, length는 DDL에 필요한 정보이므로 생략 가능
    private String itemName;
    // @Column // 필드로 선언되어 있고, 테이블의 컬럼명과 필드명이 같으면 @Column 생략 가능
    private Integer price;
    private Integer quantity;

    public Item() { // JPA는 public 또는 protected 기본 생성자 필수(JPA 명세에 명시된 내용, 프록시 기술 관련), 없으면 에러
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
