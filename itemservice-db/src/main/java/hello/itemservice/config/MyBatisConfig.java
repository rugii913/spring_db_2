package hello.itemservice.config;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.mybatis.ItemMapper;
import hello.itemservice.repository.mybatis.MyBatisItemRepository;
import hello.itemservice.service.ItemService;
import hello.itemservice.service.ItemServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MyBatisConfig {

    private final ItemMapper itemMapper;
    // DataSource는?? - mybatis 모듈이 DataSource, PlatformTransactionManager 등 읽어들여서 Mapper와 연결시켜줌
    // 'ItemMapper' 타입의 bean을 찾을 수 없습니다. - 빨간줄 뜨는데 동작에 문제 없음, 컴파일 시 스캔 대상 아니어서 빨간줄
    // ItemServiceApplication에서 아래와 같이 바꿔주면 빨간줄 사라짐
    // scanBasePackages = {"hello.itemservice.web", "hello.itemservice.repository.mybatis"}

    @Bean
    public ItemService itemService() {
        return new ItemServiceV1(itemRepository());
    }

    @Bean
    public ItemRepository itemRepository() {
        return new MyBatisItemRepository(itemMapper);
    }

}
