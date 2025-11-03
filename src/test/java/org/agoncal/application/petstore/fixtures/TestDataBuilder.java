package org.agoncal.application.petstore.fixtures;

import org.agoncal.application.petstore.domain.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Fluent test data builder for creating consistent test entities.
 * 
 * This builder provides a clean, readable API for creating test data
 * with sensible defaults and the ability to override specific properties.
 * 
 * Usage Examples:
 * <pre>
 * Customer customer = TestDataBuilder.aCustomer()
 *     .withLogin("testuser")
 *     .withEmail("test@example.com")
 *     .build();
 *     
 * Product product = TestDataBuilder.aProduct()
 *     .withName("Test Product")
 *     .withCategory(category)
 *     .build();
 * </pre>
 */
public class TestDataBuilder {

    /**
     * Creates a customer builder with sensible defaults.
     * 
     * @return CustomerBuilder for fluent configuration
     */
    public static CustomerBuilder aCustomer() {
        return new CustomerBuilder();
    }
    
    /**
     * Creates a category builder with sensible defaults.
     * 
     * @return CategoryBuilder for fluent configuration  
     */
    public static CategoryBuilder aCategory() {
        return new CategoryBuilder();
    }
    
    /**
     * Creates a product builder with sensible defaults.
     * 
     * @return ProductBuilder for fluent configuration
     */
    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }
    
    /**
     * Creates an item builder with sensible defaults.
     * 
     * @return ItemBuilder for fluent configuration
     */
    public static ItemBuilder anItem() {
        return new ItemBuilder();
    }
    
    /**
     * Creates an order builder with sensible defaults.
     * 
     * @return OrderBuilder for fluent configuration
     */
    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public static class CustomerBuilder {
        private Customer customer;
        
        public CustomerBuilder() {
            customer = new Customer();
            customer.setLogin("testuser");
            customer.setPassword("testpass");
            customer.setFirstname("Test");
            customer.setLastname("User");
            customer.setEmail("test@example.com");
            customer.setTelephone("555-1234");
            
            Address address = new Address();
            address.setStreet1("123 Test Street");
            address.setCity("Test City");
            address.setZipcode("12345");
            address.setCountry("Test Country");
            customer.setHomeAddress(address);
        }
        
        public CustomerBuilder withLogin(String login) {
            customer.setLogin(login);
            return this;
        }
        
        public CustomerBuilder withPassword(String password) {
            customer.setPassword(password);
            return this;
        }
        
        public CustomerBuilder withEmail(String email) {
            customer.setEmail(email);
            return this;
        }
        
        public CustomerBuilder withName(String firstname, String lastname) {
            customer.setFirstname(firstname);
            customer.setLastname(lastname);
            return this;
        }
        
        public CustomerBuilder withAddress(String street, String city, String zipcode, String country) {
            Address address = new Address();
            address.setStreet1(street);
            address.setCity(city);
            address.setZipcode(zipcode);
            address.setCountry(country);
            customer.setHomeAddress(address);
            return this;
        }
        
        public Customer build() {
            return customer;
        }
    }

    public static class CategoryBuilder {
        private Category category;
        
        public CategoryBuilder() {
            category = new Category();
            category.setName("Test Category");
            category.setDescription("Test category description");
        }
        
        public CategoryBuilder withName(String name) {
            category.setName(name);
            return this;
        }
        
        public CategoryBuilder withDescription(String description) {
            category.setDescription(description);
            return this;
        }
        
        public Category build() {
            return category;
        }
    }

    public static class ProductBuilder {
        private Product product;
        
        public ProductBuilder() {
            product = new Product();
            product.setName("Test Product");
            product.setDescription("Test product description");
        }
        
        public ProductBuilder withName(String name) {
            product.setName(name);
            return this;
        }
        
        public ProductBuilder withDescription(String description) {
            product.setDescription(description);
            return this;
        }
        
        public ProductBuilder withCategory(Category category) {
            product.setCategory(category);
            return this;
        }
        
        public Product build() {
            return product;
        }
    }

    public static class ItemBuilder {
        private Item item;
        
        public ItemBuilder() {
            item = new Item();
            item.setName("Test Item");
            item.setDescription("Test item description");
            item.setUnitCost(new BigDecimal("19.99"));
            item.setImagePath("/images/test.jpg");
        }
        
        public ItemBuilder withName(String name) {
            item.setName(name);
            return this;
        }
        
        public ItemBuilder withDescription(String description) {
            item.setDescription(description);
            return this;
        }
        
        public ItemBuilder withUnitCost(BigDecimal unitCost) {
            item.setUnitCost(unitCost);
            return this;
        }
        
        public ItemBuilder withUnitCost(String unitCost) {
            item.setUnitCost(new BigDecimal(unitCost));
            return this;
        }
        
        public ItemBuilder withProduct(Product product) {
            item.setProduct(product);
            return this;
        }
        
        public ItemBuilder withImagePath(String imagePath) {
            item.setImagePath(imagePath);
            return this;
        }
        
        public Item build() {
            return item;
        }
    }

    public static class OrderBuilder {
        private Order order;
        
        public OrderBuilder() {
            order = new Order();
            order.setOrderDate(new Date());
            
            // Default credit card
            CreditCard creditCard = new CreditCard();
            creditCard.setCreditCardNumber("1234567890123456");
            creditCard.setCreditCardType(CreditCardType.VISA);
            creditCard.setCreditCardExpiryDate("12/25");
            
            // Default address  
            Address address = new Address();
            address.setStreet1("123 Test Street");
            address.setCity("Test City");
            address.setZipcode("12345");
            address.setCountry("Test Country");
            
            order.setCreditCard(creditCard);
            order.setDeliveryAddress(address);
        }
        
        public OrderBuilder withCustomer(Customer customer) {
            order.setCustomer(customer);
            return this;
        }
        
        public OrderBuilder withCreditCard(String number, CreditCardType type, String expiry) {
            CreditCard creditCard = new CreditCard();
            creditCard.setCreditCardNumber(number);
            creditCard.setCreditCardType(type);
            creditCard.setCreditCardExpiryDate(expiry);
            order.setCreditCard(creditCard);
            return this;
        }
        
        public OrderBuilder withDeliveryAddress(String street, String city, String zipcode, String country) {
            Address address = new Address();
            address.setStreet1(street);
            address.setCity(city);
            address.setZipcode(zipcode);
            address.setCountry(country);
            order.setDeliveryAddress(address);
            return this;
        }
        
        public OrderBuilder withOrderDate(Date orderDate) {
            order.setOrderDate(orderDate);
            return this;
        }
        
        public Order build() {
            return order;
        }
    }
}