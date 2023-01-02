package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jta.dao;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class PostDAOImpl extends GenericDAOImpl<Post, Long> implements PostDAO {

    protected PostDAOImpl() {
        super(Post.class);
    }

    @Override
    public List<Post> findByTitle(String title) {
        return getEntityManager()
            .createQuery(
                "select p " +
                    "from Post p " +
                    "where p.title = :title", Post.class)
            .setParameter("title", title)
            .getResultList();
    }

    @Override
    public PostDTO getPostDTOById(Long id) {
        return getEntityManager()
            .createQuery(
                "select new PostDTO(p.id, p.title) " +
                    "from Post p " +
                    "where p.id = :id", PostDTO.class)
            .setParameter("id", id)
            .getSingleResult();
    }
}
