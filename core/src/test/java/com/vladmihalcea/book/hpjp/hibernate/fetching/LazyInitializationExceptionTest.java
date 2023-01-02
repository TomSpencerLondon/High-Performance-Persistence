package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.LazyInitializationException;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class LazyInitializationExceptionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    private String review = "Excellent!";

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i < 4; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);

                PostComment comment = new PostComment();
                comment.setId(i);
                comment.setPost(post);
                comment.setReview(review);
                entityManager.persist(comment);
            }
        });
    }

    @Test
    public void testNPlusOne() {

        List<PostComment> comments = null;

        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            transaction = entityManager.getTransaction();
            transaction.begin();

            comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "where pc.review = :review", PostComment.class)
            .setParameter("review", review)
            .getResultList();

            transaction.commit();
        } catch (Throwable e) {
            if ( transaction != null && transaction.isActive())
                transaction.rollback();
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        try {
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            }
        } catch (LazyInitializationException expected) {
            assertTrue(expected.getMessage().contains("could not initialize proxy"));
        }
    }

    @Test
    public void testNPlusOneSimplified() {

        List<PostComment> comments = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select pc
                from PostComment pc
                where pc.review = :review""", PostComment.class)
            .setParameter("review", review)
            .getResultList();
        });

        try {
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            }
        } catch (LazyInitializationException expected) {
            assertTrue(expected.getMessage().contains("could not initialize proxy"));
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
