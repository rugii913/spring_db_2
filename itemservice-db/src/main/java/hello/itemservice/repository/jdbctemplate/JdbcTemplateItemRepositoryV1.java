package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate
 */
@Slf4j
public class JdbcTemplateItemRepositoryV1 implements ItemRepository {

    private final JdbcTemplate template;

    public JdbcTemplateItemRepositoryV1(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) values (?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder(); // JdbcTemplate을 사용할 때 DB에서 생성한 id 값을 가져오기 위한 것
        template.update(connection -> {
            // 자동 증가 키
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, item.getItemName());
            ps.setInt(2, item.getPrice());
            ps.setInt(3, item.getQuantity());
            return ps;
        }, keyHolder); // keyHolder 때문에 로직이 조금 더 들어감

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=?, price=?, quantity=? where id=?";
        template.update(sql,
                updateParam.getItemName(), updateParam.getPrice(), updateParam.getQuantity(), itemId);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = ?";
        try {
            Item item = template.queryForObject(sql, itemRowMapper(), id);
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        String sql = "select id, item_name, price, quantity from item";
        // 동적 쿼리 - 조건에 따라 where, and 넣을 것인지 말지
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            // (cf.) StringUtils.hasText(~) -> not null & length != 0
            // -> & 모든 구성하는 char가 Character의 isWhiteSpace == false일 것(공백 문자거나 유사 공백 문자가 아니어야 함)
            sql += " where"; // where 절 추가
        }

        boolean andFlag = false; // 조건 두 개 함께 들어갈 때를 판별하는 flag
        List<Object> param = new ArrayList<>();
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',?,'%')"; // ~~~파라미터itemName~~~으로 찾기
            param.add(itemName);
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) { // andFlag가 true라면 itemName 조건이 있는 것이므로 and 붙이기
                sql += " and";
            }
            sql += " price <= ?";
            param.add(maxPrice);
        }

        log.info("sql={}", sql);
        return template.query(sql, itemRowMapper(), param.toArray());
        // param 개수가 0일 수도, 1일 수도, 2일 수도 있음
        // - 다행히 query(~) 메서드에 arg로 배열을 받더라도, 내부에서 알아서 PreparedStatement로 배치해주는 로직이 있음
        //  ==> 동적 쿼리 생성에 이용 가능
        // (cf.) ArgumentPreparedStatementSetter, PreparedStatementCreator
    }

    private RowMapper<Item> itemRowMapper() {
        return ((rs, rowNum) -> {
            Item item = new Item();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            item.setPrice(rs.getInt("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        });
    }
}
