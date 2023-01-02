package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyToManySetTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            Tag.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Tag().setName("JPA")
            );

            entityManager.persist(
                new Tag().setName("Hibernate")
            );
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("JPA with Hibernate")
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("JPA"))
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );
        });
    }

    @Test
    public void testRemoveTagReference() {
        doInJPA(entityManager -> {
            Post post1 = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.tags
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            Session session = entityManager.unwrap(Session.class);

            post1.getTags().remove(session.bySimpleNaturalId(Tag.class).getReference("JPA"));
        });
    }

    @Test
    public void testRemovePostEntity() {
        doInJPA(entityManager -> {
            LOGGER.info("Remove");
            Post post1 = entityManager.getReference(Post.class, 1L);

            entityManager.remove(post1);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

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

        public Set<Tag> getTags() {
            return tags;
        }

        public Post addTag(Tag tag) {
            tags.add(tag);
            tag.getPosts().add(this);
            return this;
        }

        public Post removeTag(Tag tag) {
            tags.remove(tag);
            tag.getPosts().remove(this);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        @ManyToMany(mappedBy = "tags")
        private Set<Post> posts = new HashSet<>();

        public Long getId() {
            return id;
        }

        public Tag setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }

        public Set<Post> getPosts() {
            return posts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
