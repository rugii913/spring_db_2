package hello.springtx.propagation;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

    public Member() { // JPA 스펙 때문에 필수로 있어야 하는 기본 생성자
    }

    public Member(String username) {
        this.username = username;
    }
}
