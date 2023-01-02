package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class EnumPostgreSQLTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
        };
    }

    public void init() {
        executeStatement("DROP TYPE post_status_info CASCADE");
        executeStatement("CREATE TYPE post_status_info AS ENUM ('PENDING', 'APPROVED', 'SPAM')");
        super.init();
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setStatus(PostStatus.PENDING);
            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(PostStatus.PENDING, post.getStatus());
        });
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Enumerated(EnumType.STRING)
        @Column(columnDefinition = "post_status_info")
        @Type(type = "pgsql_enum")
        private PostStatus status;

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

        public PostStatus getStatus() {
            return status;
        }

        public void setStatus(PostStatus status) {
            this.status = status;
        }
    }
}
