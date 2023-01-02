package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import javax.persistence.*;

public class PooledLoGenericGeneratorIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 5; i++) {
                entityManager.persist(
                    new Post().setTitle(
                        String.format(
                            "High-Performance Java Persistence, Part %d",
                            i
                        )
                    )
                );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "seq_post"
        )
        @SequenceGenerator(
            name = "seq_post",
            allocationSize = 5
        )
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }

}
