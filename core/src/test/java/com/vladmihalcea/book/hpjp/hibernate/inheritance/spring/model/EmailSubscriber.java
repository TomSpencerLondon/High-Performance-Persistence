package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "email_subscriber")
public class EmailSubscriber extends Subscriber {

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
