package com.github.mtakaki.credentialstorage.database;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.google.common.base.Optional;

import io.dropwizard.hibernate.AbstractDAO;

import jodd.petite.meta.PetiteBean;

/**
 * Database Access Object that handles all credential operations.
 *
 * @author mitsuo
 *
 */
@PetiteBean
public class CredentialDAO extends AbstractDAO<Credential> {
    public CredentialDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    /**
     * Queries for a {@link Credential} stored under the given key.
     *
     * @param key
     *            Key used to store the credentials.
     * @return The credential stored under the given key or
     *         {@code Optional.absent()} if it's missing.
     */
    public Optional<Credential> getCredentialByKey(final String key) {
        final Criteria criteria = this.criteria().add(Restrictions.eq("key", key));
        return Optional.fromNullable(this.uniqueResult(criteria));
    }

    /**
     * Saves or updates the given credential.
     *
     * @param credential
     *            The credential that will be persisted to the database.
     */
    public void save(final Credential credential) {
        this.persist(credential);
    }

    /**
     * Deletes the credential stored under the given key.
     *
     * @param key
     *            The key that were used to store the credential.
     * @return {@code true} if the credential could be found and could be
     *         delete. {@code false} if otherwise.
     */
    public boolean deleteByKey(final String key) {
        final Query query = this.currentSession().createQuery("delete Credential where key = :key")
                .setString("key", key);
        return query.executeUpdate() != 0;
    }
}