package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SimpleJdbcInsert
 */
@Slf4j
public class  JdbcTemplateItemRepositoryV3 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;
    private final SimpleJdbcInsert jdbcInsert;


    public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("item")
                .usingGeneratedKeyColumns("id");
                // .usingColumns("item_name", "price", "quantity"); // 생략 가능 - DB에서 메타데이터를 읽어서 어떤 column이 있는지 인지함
    }

    @Override
    public Item save(Item item) { // insert SQL 대신 jdbcInsert 사용
        /*
        String sql = "insert into item(item_name, price, quantity) values (:itemName, :price, :quantity)";

        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        KeyHolder keyHolder = new GeneratedKeyHolder(); // JdbcTemplate을 사용할 때 DB에서 생성한 id 값을 가져오기 위한 것
        template.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
         */
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        Number key = jdbcInsert.executeAndReturnKey(param);
        item.setId(key.longValue());
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=:itemName, price=:price, quantity=:quantity where id=:id";

        // 여러 SqlParameterSource 구현 클래스를 사용하는 것을 보여주기 위해 MapSqlParameterSource 사용해봄
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId); // 이 부분이 별도로 필요 - BeanPropertySqlParameterSource을 사용할 수 없음
        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";
        try {
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";
        // 동적 쿼리 - 조건에 따라 where, and 넣을 것인지 말지
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            // (cf.) StringUtils.hasText(~) -> not null & length != 0
            // -> & 모든 구성하는 char가 Character의 isWhiteSpace == false일 것(공백 문자거나 유사 공백 문자가 아니어야 함)
            sql += " where"; // where 절 추가
        }

        boolean andFlag = false; // 조건 두 개 함께 들어갈 때를 판별하는 flag
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')"; // ~~~파라미터itemName~~~으로 찾기
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) { // andFlag가 true라면 itemName 조건이 있는 것이므로 and 붙이기
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }

        log.info("sql={}", sql);
        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class); // cameCase 자동 변환 지원
    }
}
/*
* (정리)
* - JdbcTemplate: 반복 문제 해결, 설정 간단 / 동적 쿼리 해결 어려움(가장 큰 단점)
*  - 순서 기반 파라미터 바인딩 문제
* - NamedParameterJdbcTemplate: 이름 기반 파라미터 바인딩 지원
*  - Map 혹은 SqlParameterSource 사용(이름이 다른 경우 DB에서 별칭으로 가져오기)
* - SimpleJdbcInsert: insert 쿼리 쉽게
* - SimpleJdbcCall: 스토어드 프로시저 편리하게 호출(강의에서 안 다룸, 매뉴얼 참고)
* *** JdbcTemplate 사용 공식 매뉴얼 docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#jdbc-JdbcTemplate
*
* - 조회
*  - 단건 조회
*   - 단순 데이터: queryForObject(~) 객체가 아니라 단순 데이터 하나라면 Integer.class, String.class 같은 클래스 정보 넘김
*   - 객체: RowMapper을 넘김, RowMapper를 변수로 분리하면 재사용할 수도 있음
* - 변경: update(~) 사용 - 반환값은 SQL 실행 결과 영향 받은 로우 수
* - 기타 기능
*  - DML 외의 SQL 실행시 execute(~) 사용
*  - update(스토어드 프로시저 SQL문, ~)으로 스토어드 프로시저도 호출 가능
*
* */
