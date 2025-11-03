package org.agoncal.application.petstore.service;

import org.agoncal.application.petstore.domain.*;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Service component responsible for order processing and lifecycle management.
 * Handles order creation from shopping cart items and order retrieval operations.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link EntityManager} - JPA persistence operations</li>
 *   <li>{@link ShoppingCartController} - Order creation initiation</li>
 *   <li>{@link Customer} - Order ownership</li>
 *   <li>{@link CartItem} - Order line generation</li>
 * </ul>
 * 
 * <p>Business invariants:</p>
 * <ul>
 *   <li>Orders must have at least one order line</li>
 *   <li>All order items must reference valid persisted entities</li>
 *   <li>Customer and credit card information required for order creation</li>
 * </ul>
 * 
 * <p>Transaction behavior: Order creation is atomic - all entities persisted in single transaction</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class OrderService implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private EntityManager em;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Creates a new order from shopping cart contents, customer, and payment information.
     * Transforms cart items into order lines and persists the complete order structure.
     * 
     * @param customer the customer placing the order (will be merged if detached)
     * @param creditCard payment information for the order
     * @param cartItems list of items in the shopping cart to convert to order lines
     * @return the persisted Order entity with generated ID and order date
     * @throws ValidationException if cartItems is null or empty
     * TODO: Bean validation should replace manual cart validation
     * TODO: Consider payment processing integration
     * TODO: Inventory checking before order confirmation
     */
    public Order createOrder(final Customer customer, final CreditCard creditCard, final List<CartItem> cartItems) {

        // Validate that cart contains items before proceeding
        if (cartItems == null || cartItems.size() == 0)
            throw new ValidationException("Shopping cart is empty"); // TODO: Use bean validation

        // Create order with merged customer entity (handles detached state)
        Order order = new Order(em.merge(customer), creditCard, customer.getHomeAddress());

        // Transform shopping cart items into persistent order lines
        List<OrderLine> orderLines = new ArrayList<OrderLine>();

        for (CartItem cartItem : cartItems) {
            // Each cart item becomes an order line with merged item reference
            orderLines.add(new OrderLine(cartItem.getQuantity(), em.merge(cartItem.getItem())));
        }
        order.setOrderLines(orderLines);

        // Persist complete order structure atomically
        em.persist(order);

        return order;
    }

    /**
     * Retrieves an order by its unique identifier.
     * 
     * @param orderId the unique identifier of the order to find
     * @return the Order entity with all associated data, or null if not found
     * @throws ValidationException if orderId is null
     * TODO: Consider eager/lazy loading strategy for order lines and customer
     */
    public Order findOrder(Long orderId) {
        if (orderId == null)
            throw new ValidationException("Invalid order id");

        return em.find(Order.class, orderId);
    }

    public List<Order> findAllOrders() {
        TypedQuery<Order> typedQuery = em.createNamedQuery(Order.FIND_ALL, Order.class);
        return typedQuery.getResultList();
    }

    public void removeOrder(Order order) {
        if (order == null)
            throw new ValidationException("Order object is null");

        em.remove(em.merge(order));
    }
}
