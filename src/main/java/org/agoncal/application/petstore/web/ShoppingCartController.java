package org.agoncal.application.petstore.web;

import org.agoncal.application.petstore.domain.*;
import org.agoncal.application.petstore.service.CatalogService;
import org.agoncal.application.petstore.service.OrderService;
import org.agoncal.application.petstore.util.Loggable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JSF managed bean controlling shopping cart operations and order checkout flow.
 * Uses CDI conversation scope to maintain cart state across multiple request cycles
 * during the shopping and checkout process.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link CatalogService} - Item lookups and validation</li>
 *   <li>{@link OrderService} - Order creation from cart contents</li>
 *   <li>{@link Customer} - Logged-in customer for order association</li>
 *   <li>{@link Conversation} - CDI conversation scope management</li>
 * </ul>
 * 
 * <p>State management:</p>
 * <ul>
 *   <li>Conversation-scoped for multi-step checkout process</li>
 *   <li>Cart items maintained across page navigations</li>
 *   <li>Conversation begins when first item added, ends after order confirmation</li>
 * </ul>
 * 
 * <p>Checkout flow: Add Items → Review Cart → Confirm Order → Order Confirmation</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Named
@ConversationScoped
@Loggable
@CatchException
public class ShoppingCartController extends Controller implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    @LoggedIn
    private Instance<Customer> loggedInCustomer;
    @Inject
    private CatalogService catalogBean;
    @Inject
    private OrderService orderBean;
    @Inject
    private Conversation conversation;

    private List<CartItem> cartItems;
    private CreditCard creditCard = new CreditCard();
    private Order order;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Adds an item to the shopping cart, starting a conversation if needed.
     * If item already exists in cart, increments quantity; otherwise adds new cart item.
     * 
     * @return navigation outcome to showcart.faces to display updated cart
     * TODO: Consider using Map<Long, CartItem> for O(1) item lookup instead of linear search
     */
    public String addItemToCart() {
        Item item = catalogBean.findItem(getParamId("itemId"));

        // Initialize conversation scope and cart on first item addition
        if (conversation.isTransient()) {
            cartItems = new ArrayList<CartItem>();
            conversation.begin();
        }

        boolean itemFound = false;
        // Linear search through cart items - potential performance bottleneck for large carts
        for (CartItem cartItem : cartItems) {
            // If item already exists in cart, increment its quantity
            if (cartItem.getItem().equals(item)) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                itemFound = true;
                break; // TODO: Add break to avoid unnecessary iterations
            }
        }
        if (!itemFound)
            // Add new item to cart with initial quantity of 1
            cartItems.add(new CartItem(item, 1));

        return "showcart.faces";
    }

    public String removeItemFromCart() {
        Item item = catalogBean.findItem(getParamId("itemId"));

        for (CartItem cartItem : cartItems) {
            if (cartItem.getItem().equals(item)) {
                cartItems.remove(cartItem);
                return null;
            }
        }

        return null;
    }

    public String updateQuantity() {
        return null;
    }

    public String checkout() {
        return "confirmorder.faces";
    }

    /**
     * Creates order from cart contents and completes the checkout process.
     * Clears cart and ends conversation after successful order creation.
     * 
     * @return navigation outcome to orderconfirmed.faces showing order confirmation
     * TODO: Risk - no rollback if order creation fails after cart is cleared
     * TODO: Consider inventory validation before order confirmation
     */
    public String confirmOrder() {
        // Create order from current cart state and customer/payment info
        order = orderBean.createOrder(getCustomer(), creditCard, getCartItems());
        cartItems.clear(); // Clear cart after successful order creation

        // End conversation scope - checkout process complete
        if (!conversation.isTransient()) {
            conversation.end();
        }

        return "orderconfirmed.faces";
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public boolean shoppingCartIsEmpty() {
        return getCartItems() == null || getCartItems().size() == 0;
    }


    /**
     * Calculates the total cost of all items in the shopping cart.
     * 
     * @return total cart value, or 0.0f if cart is empty
     * TODO: Performance - consider caching total and invalidating on cart changes
     * TODO: Currency and rounding considerations for monetary calculations
     */
    public Float getTotal() {

        if (cartItems == null || cartItems.isEmpty())
            return 0f;

        Float total = 0f;

        // Sum up all line item subtotals (quantity * unit price)
        for (CartItem cartItem : cartItems) {
            total += (cartItem.getSubTotal());
        }
        return total;
    }

    // ======================================
    // =         Getters & setters          =
    // ======================================

    public Customer getCustomer() {
        return loggedInCustomer.get();
    }


    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public CreditCardType[] getCreditCardTypes() {
        return CreditCardType.values();
    }
}