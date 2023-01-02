package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CollectionLoadedStateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.region.factory_class", "ehcache");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });
        doInJPA(entityManager -> {
            Post post = entityManager
                .find(Post.class, 1L)
                .addComment(
                    new PostComment()
                        .setId(1L)
                        .setReview("JDBC part review")
                )
                .addComment(
                    new PostComment()
                        .setId(2L)
                        .setReview("Hibernate part review")
                );
        });
    }

    @Test
    public void testEntityLoad() {

        printCollectionCacheRegionStatistics(Post.class, "comments");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printCollectionCacheRegionStatistics(Post.class, "comments");

        doInJPA(entityManager -> {
            LOGGER.info("Load from cache");
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printCollectionCacheRegionStatistics(Post.class, "comments");
    }

    @Entity(name = "Post")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
