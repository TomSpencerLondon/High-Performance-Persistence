package com.vladmihalcea.book.hpjp.hibernate.query.escape;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateGlobalEscapeKeywordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Table.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString());
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Table()
                .setCatalog("library")
                .setSchema("public")
                .setName("book")
                .setDescription("The book table stores book-related info")
            );
        });

        doInJPA(entityManager -> {
            List<Table> tables = entityManager.createQuery(
                "select t " +
                "from Table t " +
                "where t.description like '%book%'", Table.class)
            .getResultList();

            assertEquals(1, tables.size());
        });
    }

    @Entity(name = "Table")
    public static class Table {

        @Id
        @GeneratedValue
        private Long id;

        private String catalog;

        private String schema;

        private String name;

        private String description;

        public Long getId() {
            return id;
        }

        public Table setId(Long id) {
            this.id = id;
            return this;
        }

        public String getCatalog() {
            return catalog;
        }

        public Table setCatalog(String catalog) {
            this.catalog = catalog;
            return this;
        }

        public String getSchema() {
            return schema;
        }

        public Table setSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public String getName() {
            return name;
        }

        public Table setName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Table setDescription(String description) {
            this.description = description;
            return this;
        }
    }
}
