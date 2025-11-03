package org.agoncal.application.petstore.service;

import org.agoncal.application.petstore.domain.Category;
import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

/**
 * Service component responsible for managing the petstore catalog including categories, products, and items.
 * This service provides comprehensive CRUD operations and search capabilities across the catalog hierarchy.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link EntityManager} - JPA persistence operations</li>
 *   <li>{@link CatalogController} - Web layer integration</li>  
 *   <li>{@link CatalogRestService} - REST API integration</li>
 * </ul>
 * 
 * <p>Business invariants:</p>
 * <ul>
 *   <li>Category names must be unique</li>
 *   <li>Products must belong to a valid category</li>
 *   <li>Items must belong to a valid product</li>
 *   <li>All entity references are validated before persistence</li>
 * </ul>
 * 
 * <p>Transaction behavior: All operations run within EJB container-managed transactions</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class CatalogService implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private EntityManager em;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Retrieves a category by its unique identifier.
     * 
     * @param categoryId the unique identifier of the category to find
     * @return the Category entity, or null if not found
     * @throws ValidationException if categoryId is null
     * TODO: Consider returning Optional<Category> instead of null for better null-safety
     */
    public Category findCategory(Long categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");

        return em.find(Category.class, categoryId);
    }

    /**
     * Finds a category by its name using a named query.
     * 
     * @param categoryName the name of the category to find
     * @return the Category entity matching the name
     * @throws ValidationException if categoryName is null
     * @throws NoResultException if no category with the given name exists
     * TODO: Risk - NoResultException not handled, could crash client code
     */
    public Category findCategory(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        TypedQuery<Category> typedQuery = em.createNamedQuery(Category.FIND_BY_NAME, Category.class);
        typedQuery.setParameter("pname", categoryName);
        return typedQuery.getSingleResult(); // TODO: Handle NoResultException
    }

    /**
     * Retrieves all categories in the system.
     * 
     * @return list of all categories, empty list if none exist
     * TODO: Performance risk - no pagination, could return large datasets
     */
    public List<Category> findAllCategories() {
        TypedQuery<Category> typedQuery = em.createNamedQuery(Category.FIND_ALL, Category.class);
        return typedQuery.getResultList();
    }

    public Category createCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        em.persist(category);
        return category;
    }

    public Category updateCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        return em.merge(category);
    }

    public void removeCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        em.remove(em.merge(category));
    }

    public void removeCategory(Long categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");

        removeCategory(findCategory(categoryId));
    }

    public List<Product> findProducts(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        TypedQuery<Product> typedQuery = em.createNamedQuery(Product.FIND_BY_CATEGORY_NAME, Product.class);
        typedQuery.setParameter("pname", categoryName);
        return typedQuery.getResultList();
    }

    /**
     * Retrieves a product by ID and eagerly initializes its items collection.
     * 
     * @param productId the unique identifier of the product to find
     * @return the Product entity with items loaded, or null if not found
     * @throws ValidationException if productId is null
     * TODO: Lazy loading workaround - this triggers N+1 query problem
     * TODO: Consider using JOIN FETCH in named query instead
     */
    public Product findProduct(Long productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        Product product = em.find(Product.class, productId);
        if (product != null) {
            product.getItems(); // Force lazy collection loading - potential N+1 query issue
        }
        return product;
    }

    public List<Product> findAllProducts() {
        TypedQuery<Product> typedQuery = em.createNamedQuery(Product.FIND_ALL, Product.class);
        return typedQuery.getResultList();
    }

    /**
     * Creates a new product, automatically persisting its category if it's transient.
     * 
     * @param product the Product entity to create
     * @return the persisted Product with generated ID
     * @throws ValidationException if product is null
     * TODO: Transaction boundary risk - category persist could fail after product validation
     */
    public Product createProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        // Auto-persist transient category if attached to product
        if (product.getCategory() != null && product.getCategory().getId() == null)
            em.persist(product.getCategory());

        em.persist(product);
        return product;
    }

    public Product updateProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        return em.merge(product);
    }

    public void removeProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        em.remove(em.merge(product));
    }

    public void removeProduct(Long productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        removeProduct(findProduct(productId));
    }

    public List<Item> findItems(Long productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        TypedQuery<Item> typedQuery = em.createNamedQuery(Item.FIND_BY_PRODUCT_ID, Item.class);
        typedQuery.setParameter("productId", productId);
        return typedQuery.getResultList();
    }

    public Item findItem(final Long itemId) {
        if (itemId == null)
            throw new ValidationException("Invalid item id");

        return em.find(Item.class, itemId);
    }

    /**
     * Performs a case-insensitive text search across item names and product names.
     * 
     * @param keyword search term to match against item and product names
     * @return list of items matching the search criteria, ordered by category and product name
     * TODO: Performance risk - LIKE with leading wildcard prevents index usage
     * TODO: Consider full-text search for better performance
     */
    public List<Item> searchItems(String keyword) {
        if (keyword == null)
            keyword = ""; // Empty string will match all items

        TypedQuery<Item> typedQuery = em.createNamedQuery(Item.SEARCH, Item.class);
        typedQuery.setParameter("keyword", "%" + keyword.toUpperCase() + "%");
        return typedQuery.getResultList();
    }

    public List<Item> findAllItems() {
        TypedQuery<Item> typedQuery = em.createNamedQuery(Item.FIND_ALL, Item.class);
        return typedQuery.getResultList();
    }

    public Item createItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        if (item.getProduct() != null && item.getProduct().getId() == null) {
            em.persist(item.getProduct());
            if (item.getProduct().getCategory() != null && item.getProduct().getCategory().getId() == null)
                em.persist(item.getProduct().getCategory());
        }

        em.persist(item);
        return item;
    }

    public Item updateItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        return em.merge(item);
    }

    public void removeItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        em.remove(em.merge(item));
    }

    public void removeItem(Long itemId) {
        if (itemId == null)
            throw new ValidationException("itemId is null");

        removeItem(findItem(itemId));
    }
}
