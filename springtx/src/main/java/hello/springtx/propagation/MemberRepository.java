package hello.springtx.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    // @PersistenceContext - 예전에는 이 어노테이션 필수였는데, 지금은 스프링 버전 업 되면서 알아서 주입
    private final EntityManager em;

    @Transactional // JPA의 모든 데이터 변경은 트랜잭션 안에서 이뤄진다.(없으면 에러)
    public void save(Member member) {
        log.info("member 저장");
        em.persist(member);
    }

    public Optional<Member> find(String username) { // id 같은 PK로 찾는다면 em.find()를 사용할 수 있지만, username은 PK가 아니므로 JPQL 사용해야함
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList().stream().findAny(); // 만약 값이 없으면 Optional.empty, 값이 여러 개라면 여러 개 중 하나만
    }
}
