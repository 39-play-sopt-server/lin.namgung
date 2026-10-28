package org.sopt.post.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PostIndexTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("제목 인덱스가 생성된다")
    void titleIndexExists() {
        assertEquals(1, countIndex("IDX_POST_TITLE"));
    }

    @Test
    @DisplayName("생성일 인덱스가 생성된다")
    void createdAtIndexExists() {
        assertEquals(1, countIndex("IDX_POST_CREATED_AT"));
    }

    private int countIndex(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(distinct index_name) from information_schema.indexes "
                        + "where upper(table_name) = 'POST' and upper(index_name) = ?",
                Integer.class,
                indexName);
        return count == null ? 0 : count;
    }
}
