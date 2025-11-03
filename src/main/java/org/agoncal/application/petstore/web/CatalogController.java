package org.agoncal.application.petstore.web;

import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.service.CatalogService;
import org.agoncal.application.petstore.util.Loggable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean controlling catalog browsing operations including product discovery,
 * item searches, and navigation between catalog pages. Serves as the web layer
 * coordinator between JSF views and the catalog service.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link CatalogService} - Business logic for catalog operations</li>
 *   <li>JSF Views - showproducts.xhtml, showitems.xhtml, showitem.xhtml, searchresult.xhtml</li>
 * </ul>
 * 
 * <p>State management:</p>
 * <ul>
 *   <li>Session-scoped for user navigation state retention</li>
 *   <li>Holds current category, product, and item selections</li>
 *   <li>Maintains search results and criteria</li>
 * </ul>
 * 
 * <p>Navigation flow: Categories → Products → Items → Item Details</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Named
//@RequestScoped TODO: Should be request scoped for better memory usage
@SessionScoped // TODO: Risk - memory accumulation in long sessions
@Loggable
@CatchException
public class CatalogController extends Controller implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private CatalogService catalogService;

    private String categoryName;
    private Long productId;
    private Long itemId;

    private String keyword;
    private Product product;
    private Item item;
    private List<Product> products;
    private List<Item> items;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Finds all products in a specific category and navigates to product listing page.
     * 
     * @return navigation outcome to showproducts.faces view
     */
    public String doFindProducts() {
        products = catalogService.findProducts(categoryName);
        return "showproducts.faces";
    }

    /**
     * Loads a specific product and its items, then navigates to item listing page.
     * 
     * @return navigation outcome to showitems.faces view
     */
    public String doFindItems() {
        product = catalogService.findProduct(productId);
        items = catalogService.findItems(productId);
        return "showitems.faces";
    }

    /**
     * Loads a specific item's details and navigates to item detail page.
     * 
     * @return navigation outcome to showitem.faces view
     */
    public String doFindItem() {
        item = catalogService.findItem(itemId);
        return "showitem.faces";
    }

    /**
     * Performs keyword search across items and navigates to search results with redirect.
     * 
     * @return navigation outcome to searchresult.faces with keyword parameter and redirect
     * TODO: Consider pagination for large search result sets
     */
    public String doSearch() {
        items = catalogService.searchItems(keyword);
        return "searchresult.faces?keyword=" + keyword + "&faces-redirect=true";
    }

    // ======================================
    // =         Getters & setters          =
    // ======================================

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}