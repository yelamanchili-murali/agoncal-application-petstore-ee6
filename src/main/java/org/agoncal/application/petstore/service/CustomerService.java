package org.agoncal.application.petstore.service;

import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

/**
 * Service component responsible for customer account management including authentication,
 * registration, and profile updates. Handles customer lifecycle operations and login validation.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link EntityManager} - JPA persistence operations</li>
 *   <li>{@link AccountController} - Web layer account management</li>
 *   <li>{@link LoginModule} - Security integration</li>
 * </ul>
 * 
 * <p>Business invariants:</p>
 * <ul>
 *   <li>Customer login must be unique across the system</li>
 *   <li>Password validation occurs at service layer</li>
 *   <li>Customer profiles require valid email and address data</li>
 * </ul>
 * 
 * <p>Security considerations: Passwords stored in plaintext - production should use hashing</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class CustomerService implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private EntityManager em;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Validates whether a login name is already registered in the system.
     * Used during customer registration to enforce unique login constraint.
     * 
     * @param login the login name to check for uniqueness
     * @return true if login already exists, false otherwise
     * @throws ValidationException if login is null
     */
    public boolean doesLoginAlreadyExist(final String login) {

        if (login == null)
            throw new ValidationException("Login cannot be null");

        // Login has to be unique - use exception handling for existence check
        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
        typedQuery.setParameter("login", login);
        try {
            typedQuery.getSingleResult();
            return true; // Found existing customer with this login
        } catch (NoResultException e) {
            return false; // No customer found, login is available
        }
    }

    public Customer createCustomer(final Customer customer) {

        if (customer == null)
            throw new ValidationException("Customer object is null");

        em.persist(customer);

        return customer;
    }

    public Customer findCustomer(final String login) {

        if (login == null)
            throw new ValidationException("Invalid login");

        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
        typedQuery.setParameter("login", login);

        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Authenticates a customer using login and password credentials.
     * 
     * @param login the customer's login name
     * @param password the customer's password (plaintext)
     * @return the authenticated Customer entity
     * @throws ValidationException if login or password is null
     * @throws NoResultException if authentication fails (no matching customer)
     * TODO: Security risk - passwords stored/compared in plaintext
     * TODO: Consider implementing password hashing with salt
     */
    public Customer findCustomer(final String login, final String password) {

        if (login == null)
            throw new ValidationException("Invalid login");
        if (password == null)
            throw new ValidationException("Invalid password");

        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN_PASSWORD, Customer.class);
        typedQuery.setParameter("login", login);
        typedQuery.setParameter("password", password);

        return typedQuery.getSingleResult(); // TODO: Handle NoResultException for failed auth
    }

    public List<Customer> findAllCustomers() {
        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_ALL, Customer.class);
        return typedQuery.getResultList();
    }

    /**
     * Updates an existing customer's profile information.
     * 
     * @param customer the Customer entity with updated information
     * @return the updated Customer entity
     * @throws ValidationException if customer is null
     * TODO: Risk - no validation of login uniqueness during update
     * TODO: Consider optimistic locking to prevent concurrent modifications
     */
    public Customer updateCustomer(final Customer customer) {

        // Make sure the object is valid
        if (customer == null)
            throw new ValidationException("Customer object is null");

        // Update the object in the database - merge handles detached entities
        em.merge(customer);

        return customer;
    }

    public void removeCustomer(final Customer customer) {
        if (customer == null)
            throw new ValidationException("Customer object is null");

        em.remove(em.merge(customer));
    }
}
