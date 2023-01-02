package com.vladmihalcea.book.hpjp.hibernate.logging;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.DataSourceProxyType;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
public class P6spyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put( "hibernate.jdbc.batch_size", "5" );
    }

    @Override
    protected DataSourceProxyType dataSourceProxyType() {
        return DataSourceProxyType.P6SPY;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "Post it!" );

            entityManager.persist(post);
        });
    }

    @Test
    public void testBatch() {
        doInJPA(entityManager -> {
            for ( long i = 0; i < 3; i++ ) {
                Post post = new Post();
                post.setId( i );
                post.setTitle(
                    String.format(
                        "Post no. %d",
                        i
                    )
                );
                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testOutageDetection() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "Post it!" );

            entityManager.persist(post);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

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
