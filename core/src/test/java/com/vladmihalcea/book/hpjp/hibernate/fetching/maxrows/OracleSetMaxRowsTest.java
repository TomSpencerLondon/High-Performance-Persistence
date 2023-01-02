package com.vladmihalcea.book.hpjp.hibernate.fetching.maxrows;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class OracleSetMaxRowsTest extends AbstractOracleIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE INDEX idx_post_created_on ON post (created_on DESC)");
                }
            });
            LongStream.range(0, 50 * 100).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                if (i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Test
    public void testSetMaxSize() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(PreparedStatement statement = connection.prepareStatement("""
                    EXPLAIN PLAN FOR
                    SELECT p.title
                    FROM post p
                    ORDER BY p.created_on DESC
                    """
                )) {
                    statement.setMaxRows(50);
                    statement.executeQuery();
                }
            });

            List<String> planLines = entityManager.createNativeQuery("""
                SELECT *
                FROM TABLE(DBMS_XPLAN.DISPLAY (FORMAT=>'ALL +OUTLINE'))
                """)
                .getResultList();

            assertTrue(planLines.size() > 1);

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
        });
    }

    @Test
    public void testLimit() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(PreparedStatement statement = connection.prepareStatement("""
                    EXPLAIN PLAN FOR
                    SELECT *
                    FROM (
                        SELECT title
                       FROM post
                       ORDER BY id DESC
                    )
                    WHERE ROWNUM <= 50
                    """
                )) {
                    statement.executeQuery();
                }
            });

            List<String> planLines = entityManager.createNativeQuery("""
                SELECT *
                FROM TABLE(DBMS_XPLAN.DISPLAY (FORMAT=>'ALL +OUTLINE'))
                """)
                .getResultList();

            assertTrue(planLines.size() > 1);

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
