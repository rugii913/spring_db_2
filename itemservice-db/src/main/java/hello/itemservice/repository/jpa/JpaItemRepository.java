package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@Transactional // JPA의 모든 데이터 변경은 트랜잭션 안에서 이뤄짐, select가 아닌 update 코드들은 반드시 @Transactional이 필요함
public class JpaItemRepository implements ItemRepository {

    private final EntityManager em; // JPA를 사용하는 repository에서는 반드시 EntityManager를 주입받아야 함
    // 이 EntityManager가 JPA의 핵심 부분 - 이것을 통해서 저장, 조회 등 이뤄짐
    // 원래는 DataSource 넣어주고, EntityManagerFactory 세팅 등 복잡한데, 스프링 부트가 알아서 해결해준다.
    // 스프링 부트 자동 설정은 JpaBaseConfiguration 참고

    public JpaItemRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Item save(Item item) {
        em.persist(item); // Java 컬렉션에 넣는 것과 크게 다를 게 없다.
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = em.find(Item.class, itemId);
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
        // em.persist(findItem); // 이게 아님
        // Java 컬렉션에 들어가 있는 객체의 필드 바꿀 때처럼, 필드만 바꾸면 됨
        // 스냅샷 등으로 객체의 변경을 감지하고 Transaction이 커밋되는 시점에 update 쿼리를 만들어서 DB에 날림
    }

    @Override
    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) { // JPA의 단점 - 동적 쿼리에 약함
        String jpql = "select i from Item i"; // from item이 아니라 from Item임
        // JPQL 문법은 SQL과 거의 비슷한데, 테이블을 대상으로 하는 것이 아닌 엔티티를 대상으로 한다고 생각하면 됨

        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if (StringUtils.hasText(itemName) || maxPrice != null) {
            jpql += " where"; // 공백 유의
        }

        boolean andFlag = false;
        if (StringUtils.hasText(itemName)) {
            jpql += " i.itemName like concat('%', :itemName, '%')"; // 이름 기반 파라미터 사용 가능
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) {
                jpql += " and";
            }
            jpql += " i.price <= :maxPrice";
        }

        log.info("jpql={}", jpql);

        TypedQuery<Item> query = em.createQuery(jpql, Item.class);
        if (StringUtils.hasText(itemName)) {
            query.setParameter("itemName", itemName);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }
}
