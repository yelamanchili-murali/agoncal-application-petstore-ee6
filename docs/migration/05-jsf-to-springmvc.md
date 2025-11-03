# JSF to Spring MVC + Thymeleaf Migration

## Overview
Migration from JSF 2.0 component-based web framework to Spring MVC's request/response model with Thymeleaf templating engine.

## Architecture Comparison

### JSF Architecture (Current)
- **Component-based**: Server-side component tree
- **Postback model**: Form submissions to same URL
- **Managed beans**: Session/request scoped backing beans
- **Facelets**: XHTML templating with JSF tags
- **Navigation**: faces-config.xml rules + implicit navigation
- **Validation**: JSF validators + Bean Validation
- **Ajax**: Built-in JSF Ajax support

### Spring MVC + Thymeleaf (Target)
- **MVC pattern**: Clear separation of concerns
- **RESTful URLs**: Semantic URLs with path variables
- **Controllers**: Stateless request handlers
- **Thymeleaf**: Natural HTML templates
- **Navigation**: Controller method returns + redirects
- **Validation**: Spring Validation + Bean Validation
- **Ajax**: REST endpoints + JavaScript

## Route Mapping Strategy

### Complete JSF → Spring MVC Route Map

| JSF Page | Original URL | Spring MVC Route | Controller | Template |
|----------|-------------|------------------|------------|----------|
| `index.html` | `/applicationPetstore/` | `GET /` | `HomeController.index()` | `index.html` |
| `main.xhtml` | `/main.faces` | `GET /home` | `HomeController.home()` | `home.html` |
| `signon.xhtml` | `/signon.faces` | `GET /auth/login` | `AuthController.loginForm()` | `auth/login.html` |
| `signon.xhtml` | `POST /signon.faces` | `POST /auth/login` | `AuthController.processLogin()` | redirect |
| `createaccount.xhtml` | `/createaccount.faces` | `GET /auth/register` | `AuthController.registerForm()` | `auth/register.html` |
| `createaccount.xhtml` | `POST /createaccount.faces` | `POST /auth/register` | `AuthController.processRegister()` | redirect |
| `showaccount.xhtml` | `/showaccount.faces` | `GET /account/profile` | `AccountController.showProfile()` | `account/profile.html` |
| `updateaccount.xhtml` | `/updateaccount.faces` | `GET /account/edit` | `AccountController.editProfile()` | `account/edit.html` |
| `updateaccount.xhtml` | `POST /updateaccount.faces` | `PUT /account` | `AccountController.updateProfile()` | redirect |
| `showproducts.xhtml` | `/showproducts.faces?categoryName=X` | `GET /catalog/{categoryName}` | `CatalogController.showProducts()` | `catalog/products.html` |
| `showitems.xhtml` | `/showitems.faces?productId=X` | `GET /catalog/products/{productId}` | `CatalogController.showItems()` | `catalog/items.html` |
| `showitem.xhtml` | `/showitem.faces?itemId=X` | `GET /catalog/items/{itemId}` | `CatalogController.showItem()` | `catalog/item-detail.html` |
| `searchresult.xhtml` | `/searchresult.faces?keyword=X` | `GET /search?q={keyword}` | `SearchController.search()` | `search/results.html` |
| `showcart.xhtml` | `/showcart.faces` | `GET /cart` | `CartController.showCart()` | `cart/show.html` |
| `showcart.xhtml` | `POST /showcart.faces` | `POST /cart/items` | `CartController.addToCart()` | redirect |
| `confirmorder.xhtml` | `/confirmorder.faces` | `GET /order/confirm` | `OrderController.confirmOrder()` | `order/confirm.html` |
| `orderconfirmed.xhtml` | `/orderconfirmed.faces` | `POST /orders` | `OrderController.placeOrder()` | `order/confirmed.html` |

## Controller Implementation Examples

### 1. Authentication Controller

#### AuthController.java
```java
package org.agoncal.application.petstore.controller;

import org.agoncal.application.petstore.dto.LoginRequest;
import org.agoncal.application.petstore.dto.RegisterRequest;
import org.agoncal.application.petstore.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final CustomerService customerService;

    public AuthController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute LoginRequest loginRequest,
                              BindingResult result,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/login";
        }

        try {
            Customer customer = customerService.authenticateCustomer(
                loginRequest.getLogin(), 
                loginRequest.getPassword()
            );
            
            session.setAttribute("loggedInCustomer", customer);
            redirectAttributes.addFlashAttribute("message", "Welcome back, " + customer.getFirstname());
            return "redirect:/home";
            
        } catch (AuthenticationException e) {
            result.rejectValue("login", "auth.failed", "Invalid credentials");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute RegisterRequest registerRequest,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/register";
        }

        if (customerService.doesLoginAlreadyExist(registerRequest.getLogin())) {
            result.rejectValue("login", "login.exists", "Login already exists");
            return "auth/register";
        }

        if (!registerRequest.getPassword().equals(registerRequest.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "password.mismatch", "Passwords do not match");
            return "auth/register";
        }

        Customer customer = customerService.createCustomer(registerRequest.toCustomer());
        redirectAttributes.addFlashAttribute("message", "Account created successfully");
        return "redirect:/auth/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("message", "You have been logged out");
        return "redirect:/";
    }
}
```

### 2. Catalog Controller

#### CatalogController.java
```java
package org.agoncal.application.petstore.controller;

import org.agoncal.application.petstore.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{categoryName}")
    public String showProducts(@PathVariable String categoryName, 
                              Pageable pageable, 
                              Model model) {
        
        Page<Product> products = catalogService.findProductsByCategory(categoryName, pageable);
        
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("products", products);
        model.addAttribute("page", products);
        
        return "catalog/products";
    }

    @GetMapping("/products/{productId}")
    public String showItems(@PathVariable Long productId, Model model) {
        
        Product product = catalogService.findProduct(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        
        List<Item> items = catalogService.findItemsByProduct(productId);
        
        model.addAttribute("product", product);
        model.addAttribute("items", items);
        
        return "catalog/items";
    }

    @GetMapping("/items/{itemId}")
    public String showItem(@PathVariable Long itemId, Model model) {
        
        Item item = catalogService.findItem(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));
        
        model.addAttribute("item", item);
        model.addAttribute("addToCartForm", new AddToCartForm(itemId, 1));
        
        return "catalog/item-detail";
    }
}
```

### 3. Shopping Cart Controller

#### CartController.java
```java
package org.agoncal.application.petstore.controller;

import org.agoncal.application.petstore.dto.AddToCartRequest;
import org.agoncal.application.petstore.service.ShoppingCartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final ShoppingCartService shoppingCartService;

    public CartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping
    public String showCart(HttpSession session, Model model) {
        ShoppingCart cart = shoppingCartService.getCart(session);
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getCartItems());
        model.addAttribute("total", cart.getTotal());
        return "cart/show";
    }

    @PostMapping("/items")
    public String addToCart(@ModelAttribute AddToCartRequest request,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        shoppingCartService.addItemToCart(session, request.getItemId(), request.getQuantity());
        redirectAttributes.addFlashAttribute("message", "Item added to cart");
        return "redirect:/cart";
    }

    @PutMapping("/items/{itemId}")
    public String updateQuantity(@PathVariable Long itemId,
                               @RequestParam Integer quantity,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        
        shoppingCartService.updateQuantity(session, itemId, quantity);
        return "redirect:/cart";
    }

    @DeleteMapping("/items/{itemId}")
    public String removeItem(@PathVariable Long itemId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        shoppingCartService.removeItemFromCart(session, itemId);
        redirectAttributes.addFlashAttribute("message", "Item removed from cart");
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session) {
        // Ensure user is logged in
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/auth/login";
        }
        
        return "redirect:/order/confirm";
    }
}
```

### 4. Order Controller

#### OrderController.java
```java
package org.agoncal.application.petstore.controller;

import org.agoncal.application.petstore.service.OrderService;
import org.agoncal.application.petstore.service.ShoppingCartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final ShoppingCartService shoppingCartService;

    public OrderController(OrderService orderService, ShoppingCartService shoppingCartService) {
        this.orderService = orderService;
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/confirm")
    public String confirmOrder(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/auth/login";
        }

        ShoppingCart cart = shoppingCartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        model.addAttribute("total", cart.getTotal());
        
        return "order/confirm";
    }

    @PostMapping
    public String placeOrder(HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        ShoppingCart cart = shoppingCartService.getCart(session);

        Order order = orderService.createOrder(cart, customer);
        shoppingCartService.clearCart(session);

        redirectAttributes.addFlashAttribute("order", order);
        redirectAttributes.addFlashAttribute("message", 
            "Order #" + order.getId() + " placed successfully");
        
        return "redirect:/order/" + order.getId() + "/confirmed";
    }

    @GetMapping("/{orderId}/confirmed")
    public String orderConfirmed(@PathVariable Long orderId, Model model) {
        Order order = orderService.findOrder(orderId);
        model.addAttribute("order", order);
        return "order/confirmed";
    }
}
```

## Data Transfer Objects (DTOs)

### Form Binding DTOs
```java
// LoginRequest.java
public class LoginRequest {
    @NotBlank(message = "Login is required")
    @Size(max = 10, message = "Login must not exceed 10 characters")
    private String login;

    @NotBlank(message = "Password is required")
    @Size(max = 10, message = "Password must not exceed 10 characters")
    private String password;

    // Getters and setters...
}

// RegisterRequest.java
public class RegisterRequest {
    @NotBlank(message = "Login is required")
    @Size(max = 10, message = "Login must not exceed 10 characters")
    private String login;

    @NotBlank(message = "Password is required")
    @Size(max = 10, message = "Password must not exceed 10 characters")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirm;

    public Customer toCustomer() {
        Customer customer = new Customer();
        customer.setLogin(login);
        customer.setPassword(password);
        return customer;
    }

    // Getters and setters...
}

// AddToCartRequest.java
public class AddToCartRequest {
    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    // Constructors, getters and setters...
}
```

## Thymeleaf Template Examples

### 1. Login Template (auth/login.html)
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - Petstore</title>
    <link href="/css/bootstrap.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
</head>
<body>
    <div th:replace="~{fragments/header :: header}"></div>
    
    <main class="container">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <h2 th:text="#{signon.signIn}">Sign In</h2>
                
                <div th:if="${param.error}" class="alert alert-danger">
                    <span th:text="#{auth.failed}">Invalid credentials</span>
                </div>
                
                <div th:if="${message}" class="alert alert-info">
                    <span th:text="${message}"></span>
                </div>

                <form th:action="@{/auth/login}" th:object="${loginRequest}" method="post">
                    <div class="mb-3">
                        <label for="login" class="form-label" th:text="#{customer.login}">Login</label>
                        <input type="text" class="form-control" id="login" th:field="*{login}" 
                               th:classappend="${#fields.hasErrors('login')} ? 'is-invalid' : ''"
                               maxlength="10" required>
                        <div th:if="${#fields.hasErrors('login')}" class="invalid-feedback">
                            <span th:errors="*{login}"></span>
                        </div>
                    </div>
                    
                    <div class="mb-3">
                        <label for="password" class="form-label" th:text="#{customer.password}">Password</label>
                        <input type="password" class="form-control" id="password" th:field="*{password}"
                               th:classappend="${#fields.hasErrors('password')} ? 'is-invalid' : ''"
                               maxlength="10" required>
                        <div th:if="${#fields.hasErrors('password')}" class="invalid-feedback">
                            <span th:errors="*{password}"></span>
                        </div>
                    </div>
                    
                    <button type="submit" class="btn btn-primary" th:text="#{signon.signIn}">Sign In</button>
                    <small class="form-text text-muted">Demo: user/user or admin/admin</small>
                </form>
                
                <hr>
                <p>Don't have an account? <a th:href="@{/auth/register}" th:text="#{signon.signup}">Sign up</a></p>
            </div>
        </div>
    </main>
    
    <div th:replace="~{fragments/footer :: footer}"></div>
    <script src="/js/bootstrap.bundle.js"></script>
</body>
</html>
```

### 2. Product Listing Template (catalog/products.html)
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="|Products for #{${categoryName}} - Petstore|">Products - Petstore</title>
    <link href="/css/bootstrap.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
</head>
<body>
    <div th:replace="~{fragments/header :: header}"></div>
    
    <main class="container">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a th:href="@{/}">Home</a></li>
                <li class="breadcrumb-item active" th:text="${categoryName}">Category</li>
            </ol>
        </nav>

        <h2 th:text="|Products for #{${categoryName}}|">Products for Category</h2>
        
        <div th:if="${products.isEmpty()}" class="alert alert-info">
            <span th:text="#{product.noProductFound}">No products found in this category</span>
        </div>
        
        <div th:if="${!products.isEmpty()}" class="row">
            <div th:each="product : ${products.content}" class="col-md-6 col-lg-4 mb-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">
                            <a th:href="@{/catalog/products/{id}(id=${product.id})}" 
                               th:text="${product.name}" class="text-decoration-none">Product Name</a>
                        </h5>
                        <p class="card-text" th:text="${product.description}">Product description</p>
                        <a th:href="@{/catalog/products/{id}(id=${product.id})}" class="btn btn-primary">
                            View Items
                        </a>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Pagination -->
        <nav th:if="${products.totalPages > 1}" aria-label="Product pagination">
            <ul class="pagination justify-content-center">
                <li class="page-item" th:classappend="${!products.hasPrevious()} ? 'disabled'">
                    <a class="page-link" th:href="@{/catalog/{category}(category=${categoryName}, page=${products.number - 1})}">Previous</a>
                </li>
                <li th:each="pageNum : ${#numbers.sequence(0, products.totalPages - 1)}" 
                    class="page-item" th:classappend="${pageNum == products.number} ? 'active'">
                    <a class="page-link" th:href="@{/catalog/{category}(category=${categoryName}, page=${pageNum})}" 
                       th:text="${pageNum + 1}">1</a>
                </li>
                <li class="page-item" th:classappend="${!products.hasNext()} ? 'disabled'">
                    <a class="page-link" th:href="@{/catalog/{category}(category=${categoryName}, page=${products.number + 1})}">Next</a>
                </li>
            </ul>
        </nav>
    </main>
    
    <div th:replace="~{fragments/footer :: footer}"></div>
    <script src="/js/bootstrap.bundle.js"></script>
</body>
</html>
```

### 3. Shopping Cart Template (cart/show.html)
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shopping Cart - Petstore</title>
    <link href="/css/bootstrap.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
</head>
<body>
    <div th:replace="~{fragments/header :: header}"></div>
    
    <main class="container">
        <h2 th:text="#{shoppingCart}">Shopping Cart</h2>
        
        <!-- Empty cart message -->
        <div th:if="${cart.empty}" class="alert alert-info">
            <span th:text="#{shoppingCart.empty}">Your shopping cart is empty</span>
        </div>
        
        <!-- Cart items -->
        <div th:if="${!cart.empty}">
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>Quantity</th>
                            <th>Unit Price</th>
                            <th>Subtotal</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="cartItem : ${cartItems}">
                            <td>
                                <div>
                                    <strong th:text="${cartItem.item.product.name}">Product Name</strong><br>
                                    <a th:href="@{/catalog/items/{id}(id=${cartItem.item.id})}" 
                                       th:text="${cartItem.item.name}" class="text-muted">Item Name</a>
                                </div>
                            </td>
                            <td>
                                <form th:action="@{/cart/items/{id}(id=${cartItem.item.id})}" method="post" class="d-inline">
                                    <input type="hidden" name="_method" value="put">
                                    <div class="input-group" style="width: 120px;">
                                        <input type="number" name="quantity" th:value="${cartItem.quantity}" 
                                               min="1" class="form-control form-control-sm">
                                        <button type="submit" class="btn btn-outline-secondary btn-sm">Update</button>
                                    </div>
                                </form>
                            </td>
                            <td>
                                $<span th:text="${#numbers.formatDecimal(cartItem.item.unitCost, 1, 2)}">0.00</span>
                            </td>
                            <td>
                                $<span th:text="${#numbers.formatDecimal(cartItem.subTotal, 1, 2)}">0.00</span>
                            </td>
                            <td>
                                <form th:action="@{/cart/items/{id}(id=${cartItem.item.id})}" method="post" class="d-inline">
                                    <input type="hidden" name="_method" value="delete">
                                    <button type="submit" class="btn btn-outline-danger btn-sm" 
                                            onclick="return confirm('Remove this item?')">Remove</button>
                                </form>
                            </td>
                        </tr>
                    </tbody>
                    <tfoot>
                        <tr>
                            <th colspan="3">Total:</th>
                            <th>$<span th:text="${#numbers.formatDecimal(total, 1, 2)}">0.00</span></th>
                            <th>
                                <form th:action="@{/cart/checkout}" method="post" class="d-inline">
                                    <button type="submit" class="btn btn-success">Checkout</button>
                                </form>
                            </th>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>
    </main>
    
    <div th:replace="~{fragments/footer :: footer}"></div>
    <script src="/js/bootstrap.bundle.js"></script>
</body>
</html>
```

## Navigation Flow Migration

### JSF faces-config.xml Rules → Spring MVC Redirects

#### Before (faces-config.xml)
```xml
<navigation-rule>
    <from-view-id>/signon.xhtml</from-view-id>
    <navigation-case>
        <from-outcome>main</from-outcome>
        <to-view-id>/main.xhtml</to-view-id>
        <redirect />
    </navigation-case>
</navigation-rule>
```

#### After (Spring MVC Controller)
```java
@PostMapping("/auth/login")
public String processLogin(...) {
    // Authentication logic
    return "redirect:/home";  // Equivalent to <redirect />
}
```

### Implicit Navigation Migration
- JSF: `return "showproducts";` → `showproducts.xhtml`
- Spring MVC: `return "catalog/products";` → `templates/catalog/products.html`

## Session Management Migration

### JSF Session Beans → Spring Session
```java
// Before: JSF @SessionScoped
@Named
@SessionScoped
public class AccountController {
    private Customer loggedinCustomer;
}

// After: Spring Session Attributes
@Controller
@SessionAttributes("loggedInCustomer")
public class AuthController {
    
    @ModelAttribute("loggedInCustomer")
    public Customer loggedInCustomer() {
        return new Customer();
    }
}
```

## AJAX Support Migration

### JSF Ajax → REST + JavaScript
```html
<!-- Before: JSF Ajax -->
<h:commandButton value="Add to Cart">
    <f:ajax execute="quantity" render="cartTotal" listener="#{cartController.addToCart}"/>
</h:commandButton>

<!-- After: REST + JavaScript -->
<button onclick="addToCart(${item.id})">Add to Cart</button>

<script>
async function addToCart(itemId) {
    const quantity = document.getElementById('quantity').value;
    const response = await fetch('/api/cart/items', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ itemId, quantity })
    });
    
    if (response.ok) {
        updateCartTotal();
        showNotification('Item added to cart');
    }
}
</script>
```

## Validation Migration

### JSF + Bean Validation → Spring MVC + Bean Validation
```java
// DTO with validation annotations (same as JSF)
public class LoginRequest {
    @NotBlank(message = "Login is required")
    @Size(max = 10)
    private String login;
    
    @NotBlank(message = "Password is required")
    @Size(max = 10)
    private String password;
}

// Controller method with validation
@PostMapping("/auth/login")
public String processLogin(@Valid @ModelAttribute LoginRequest loginRequest,
                          BindingResult result) {
    if (result.hasErrors()) {
        return "auth/login";  // Show form with errors
    }
    // Process login...
}
```

## Migration Phases

### Phase 1: Parallel Implementation
1. Keep JSF pages functional
2. Create Spring MVC controllers alongside
3. Create Thymeleaf templates
4. Test new routes

### Phase 2: Gradual Migration
1. Migrate authentication first
2. Migrate catalog browsing
3. Migrate shopping cart
4. Migrate order processing

### Phase 3: JSF Removal
1. Remove JSF dependencies
2. Remove .xhtml files
3. Clean up faces-config.xml
4. Remove JSF web.xml configuration

## Testing Strategy

### Controller Testing
```java
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CatalogService catalogService;
    
    @Test
    void shouldShowProducts() throws Exception {
        // Given
        List<Product> products = Arrays.asList(new Product("Test Product"));
        when(catalogService.findProductsByCategory("electronics"))
            .thenReturn(new PageImpl<>(products));
        
        // When & Then
        mockMvc.perform(get("/catalog/electronics"))
               .andExpect(status().isOk())
               .andExpect(view().name("catalog/products"))
               .andExpect(model().attribute("categoryName", "electronics"))
               .andExpect(model().attribute("products", hasSize(1)));
    }
}
```

## Migration Checklist

- [ ] Create controller classes for each JSF managed bean
- [ ] Implement route mapping according to the plan
- [ ] Create DTOs for form binding
- [ ] Create Thymeleaf templates for each JSF page
- [ ] Implement session management
- [ ] Add validation and error handling
- [ ] Create REST endpoints for AJAX functionality
- [ ] Test all navigation flows
- [ ] Performance test with realistic load
- [ ] Remove JSF dependencies

## Next Steps

1. Start with AuthController (login/register)
2. Implement CatalogController (browsing)
3. Create CartController (shopping cart)
4. Build OrderController (checkout)
5. Add search functionality
6. Proceed to observability setup