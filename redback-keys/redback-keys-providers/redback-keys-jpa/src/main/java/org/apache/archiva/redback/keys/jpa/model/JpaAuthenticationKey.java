package org.apache.archiva.redback.keys.jpa.model;

import org.apache.archiva.redback.keys.AuthenticationKey;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by martin on 27.10.16.
 */
@javax.persistence.Entity
@Table(name="JDOAUTHENTICATIONKEY")
public class JpaAuthenticationKey implements AuthenticationKey {

    @Column(name="AUTHKEY")
    @Id
    private String key;

    @Column(name="FOR_PRINCIPAL")
    private String forPrincipal;

    @Column(name="PURPOSE")
    private String purpose;

    @Column(name="DATE_CREATED")
    private Date dateCreated;

    @Column(name="DATE_EXPIRES")
    private Date dateExpires;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getForPrincipal() {
        return forPrincipal;
    }

    public void setForPrincipal(String forPrincipal) {
        this.forPrincipal = forPrincipal;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateExpires() {
        return dateExpires;
    }

    public void setDateExpires(Date dateExpires) {
        this.dateExpires = dateExpires;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JpaAuthenticationKey that = (JpaAuthenticationKey) o;

        return key.equals(that.key);

    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
