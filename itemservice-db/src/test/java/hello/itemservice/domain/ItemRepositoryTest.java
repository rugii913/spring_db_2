package hello.itemservice.domain;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import hello.itemservice.repository.memory.MemoryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Transactional // 원래는 트랜잭션 시작하고 문제 없으면 커밋하는 어노테이션 - 테스트에서는 기본이 항상 롤백
// @Commit // @Rollback(value = false)
@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;
    /*
    // 트랜잭션 관련 코드
    @Autowired
    PlatformTransactionManager transactionManager; // cf. 스프링 부트는 트랜잭션 매니저 자동 등록 대상
    // JdbcTransactionManager -> DataSourceTransactionManager -> PlatformTransactionManager
    TransactionStatus status;

    @BeforeEach
    void beforeEach() {
        // 트랜잭션 시작
        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    }
    */

    @AfterEach
    void afterEach() {
        //MemoryItemRepository 의 경우 제한적으로 사용
        if (itemRepository instanceof MemoryItemRepository) {
            ((MemoryItemRepository) itemRepository).clearStore();
        }
        /*
        // 트랜잭션 롤백
        transactionManager.rollback(status);
        */
    }

    @Test
    void save() {
        //given
        Item item = new Item("itemA", 10000, 10);

        //when
        Item savedItem = itemRepository.save(item);

        //then
        Item findItem = itemRepository.findById(item.getId()).get();
        assertThat(findItem).isEqualTo(savedItem);
    }

    @Test
    // @Commit
    // cf. update의 경우 테스트에서 SQL 나가는 것을 확인하고 싶으면 @Commit해야함
    // 캐시에 저장하고 있다가 commit하는 순간에 update 쿼리를 날리기 때문
    void updateItem() {
        //given
        Item item = new Item("item1", 10000, 10);
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getId();

        //when
        ItemUpdateDto updateParam = new ItemUpdateDto("item2", 20000, 30);
        itemRepository.update(itemId, updateParam);

        //then
        Item findItem = itemRepository.findById(itemId).get();
        assertThat(findItem.getItemName()).isEqualTo(updateParam.getItemName());
        assertThat(findItem.getPrice()).isEqualTo(updateParam.getPrice());
        assertThat(findItem.getQuantity()).isEqualTo(updateParam.getQuantity());
    }

    @Test
    void findItems() {
        //given
        Item item1 = new Item("itemA-1", 10000, 10);
        Item item2 = new Item("itemA-2", 20000, 20);
        Item item3 = new Item("itemB-1", 30000, 30);

        log.info("repository={}", itemRepository.getClass()); // 예외 변환용 SpringCGLIB 프록시 객체 확인 가능
        // @Transactional, @Repository 다 빼면, 프록시가 아니라 JpaItemRepository 객체 로그를 볼 수 있음
        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        // 여기서 3개 이상의 데이터가 조회되는 문제 발생 - 테스트에서는 격리된 환경이 중요함
        // 두 조건 모두 없는 경우 검증
        test(null, null, item1, item2, item3);
        test("", null, item1, item2, item3);

        //itemName 검증
        test("itemA", null, item1, item2);
        test("temA", null, item1, item2);
        test("itemB", null, item3);

        //maxPrice 검증
        test(null, 10000, item1);

        //둘 다 있음 검증
        test("itemA", 10000, item1);
    }

    void test(String itemName, Integer maxPrice, Item... items) {
        List<Item> result = itemRepository.findAll(new ItemSearchCond(itemName, maxPrice));
        assertThat(result).containsExactly(items);
    }
}
