# Application Specification (Reverse-Engineered)

## 1. Domain Model

### 1.1 Core Entities

| Entity | Key Fields | Validation Rules | Relationships |
|--------|-----------|------------------|---------------|
| **Customer** | login (unique), password, firstname, lastname, email, homeAddress, dateOfBirth | login: 10 chars max, password: 10 chars max, email: @Email format | 1 → * Order (customer_fk) |
| **Category** | name (unique), description | name: 1-30 chars, description: @NotEmpty | 1 → * Product (category_fk) |
| **Product** | name, description, category | name: 1-30 chars, description: required | * → 1 Category, 1 → * Item |
| **Item** | name, description, unitCost, imagePath, product | name: 1-30 chars, unitCost: @Price, imagePath: @NotEmpty | * → 1 Product |
| **Order** | orderDate, customer, orderLines, deliveryAddress, creditCard | orderDate: auto-generated on persist | * → 1 Customer, 1 → * OrderLine |
| **OrderLine** | quantity, item | quantity: required | * → 1 Item |
| **CartItem** | item, quantity | quantity: min 1 | * → 1 Item (transient) |

### 1.2 Embedded Value Objects

| Value Object | Fields | Usage |
|-------------|--------|-------|
| **Address** | street1, street2, city, state, zipcode, country | Customer.homeAddress, Order.deliveryAddress |
| **CreditCard** | creditCardNumber, creditCardType, creditCardExpDate | Order.creditCard |

### 1.3 Entity Relationships

```
Category (1) ←→ (*) Product ←→ (*) Item
Customer (1) ←→ (*) Order (1) ←→ (*) OrderLine (*) ←→ (1) Item
CartItem (*) ←→ (1) Item [transient relationship]
```

## 2. Use-Case Catalogue

### 2.1 Customer Management

| Use Case | Preconditions | Postconditions | Invariants |
|----------|---------------|----------------|------------|
| **Register Customer** | - Login unique<br>- Valid email format | - Customer persisted<br>- Login session established | - Login uniqueness maintained |
| **Customer Login** | - Valid credentials<br>- Customer exists | - Session established<br>- Customer profile loaded | - Password validation via JAAS |
| **Update Profile** | - Customer logged in | - Profile changes persisted | - Login uniqueness preserved |
| **Customer Logout** | - Customer logged in | - Session cleared<br>- Conversation ended | - Clean session state |

### 2.2 Catalog Management

| Use Case | Preconditions | Postconditions | Invariants |
|----------|---------------|----------------|------------|
| **Browse Categories** | - None | - Categories displayed | - Ordered by name ASC |
| **View Products** | - Valid category selected | - Products in category displayed | - Category-product relationship maintained |
| **View Items** | - Valid product selected | - Items for product displayed | - Product-item relationship maintained |
| **Search Items** | - Keyword provided | - Matching items displayed | - Case-insensitive search |

### 2.3 Shopping Cart Operations

| Use Case | Preconditions | Postconditions | Invariants |
|----------|---------------|----------------|------------|
| **Add Item to Cart** | - Valid item selected | - Item added/quantity updated<br>- Conversation started | - Positive quantities only |
| **Remove from Cart** | - Item exists in cart | - Item removed from cart | - Cart consistency maintained |
| **View Cart** | - Cart exists | - Cart contents displayed with totals | - Accurate total calculation |
| **Checkout** | - Non-empty cart<br>- Customer logged in | - Order created<br>- Cart cleared<br>- Conversation ended | - Order-customer relationship |

## 3. Controller/API Inventory

### 3.1 JSF Web Controllers

| Controller | Method | Navigation Outcome | Parameters | Model Keys |
|------------|--------|-------------------|------------|------------|
| **CatalogController** | doFindProducts() | showproducts.faces | categoryName | products |
|  | doFindItems() | showitems.faces | productId | product, items |
|  | doFindItem() | showitem.faces | itemId | item |
|  | doSearch() | searchresult.faces?keyword={keyword}&faces-redirect=true | keyword | items |
| **AccountController** | doLogin() | main.faces | credentials.login, credentials.password | loggedinCustomer |
|  | doCreateNewAccount() | createaccount.faces | credentials.* | loggedinCustomer |
|  | doCreateCustomer() | main.faces | loggedinCustomer.* | loggedinCustomer |
|  | doLogout() | main.faces | - | - |
|  | doUpdateAccount() | showaccount.faces | loggedinCustomer.* | loggedinCustomer |
| **ShoppingCartController** | addItemToCart() | showcart.faces | itemId (param) | cartItems |
|  | removeItemFromCart() | null | itemId (param) | cartItems |
|  | checkout() | confirmorder.faces | - | - |
|  | confirmOrder() | orderconfirmed.faces | creditCard.* | order |

### 3.2 REST API Endpoints

| Resource | HTTP Method | Path | Content-Type | Response |
|----------|-------------|------|-------------|----------|
| **Categories** | GET | /catalog/categories | application/xml, application/json | List<Category> |
|  | GET | /catalog/category/{id} | application/xml, application/json | Category |
|  | POST | /catalog/category | application/xml, application/json | 201 Created |
|  | PUT | /catalog/category | application/xml, application/json | 200 OK |
|  | DELETE | /catalog/category/{id} | - | 204 No Content |
| **Products** | GET | /catalog/products | application/xml, application/json | List<Product> |
|  | GET | /catalog/product/{id} | application/xml, application/json | Product |
|  | POST | /catalog/product | application/xml, application/json | 201 Created |
|  | PUT | /catalog/product | application/xml, application/json | 200 OK |
|  | DELETE | /catalog/product/{id} | - | 204 No Content |
| **Items** | GET | /catalog/items | application/xml, application/json | List<Item> |
|  | GET | /catalog/item/{id} | application/xml, application/json | Item |
|  | POST | /catalog/item | application/xml, application/json | 201 Created |
|  | PUT | /catalog/item | application/xml, application/json | 200 OK |
|  | DELETE | /catalog/item/{id} | - | 204 No Content |

## 4. Service Contracts

### 4.1 CatalogService

| Method | Parameters | Return Type | Side Effects | Transaction |
|--------|------------|-------------|--------------|-------------|
| findCategory(Long) | categoryId: Long | Category | - Read-only lookup | Container-managed |
| findCategory(String) | categoryName: String | Category | - Named query execution | Container-managed |
| findAllCategories() | - | List<Category> | - | Container-managed |
| createCategory(Category) | category: Category | Category | - Entity persistence | Container-managed |
| updateCategory(Category) | category: Category | Category | - Entity merge | Container-managed |
| removeCategory(Long/Category) | categoryId or category | void | - Entity removal | Container-managed |
| findProduct(Long) | productId: Long | Product | - Lazy collection loading | Container-managed |
| searchItems(String) | keyword: String | List<Item> | - Case-insensitive LIKE query | Container-managed |

### 4.2 CustomerService

| Method | Parameters | Return Type | Side Effects | Transaction |
|--------|------------|-------------|--------------|-------------|
| doesLoginAlreadyExist(String) | login: String | boolean | - Uniqueness validation | Container-managed |
| createCustomer(Customer) | customer: Customer | Customer | - Entity persistence | Container-managed |
| findCustomer(String) | login: String | Customer | - | Container-managed |
| findCustomer(String, String) | login, password: String | Customer | - Authentication query | Container-managed |
| updateCustomer(Customer) | customer: Customer | Customer | - Entity merge | Container-managed |

### 4.3 OrderService

| Method | Parameters | Return Type | Side Effects | Transaction |
|--------|------------|-------------|--------------|-------------|
| createOrder(Customer, CreditCard, List<CartItem>) | customer, creditCard, cartItems | Order | - Order + OrderLines persistence<br>- Entity merging | Container-managed |
| findOrder(Long) | orderId: Long | Order | - | Container-managed |
| findAllOrders() | - | List<Order> | - | Container-managed |

## 5. Non-Functional Requirements

### 5.1 Security

| Aspect | Implementation | Configuration |
|--------|----------------|---------------|
| **Authentication** | JAAS LoginModule | LoginContextProducer, SimpleLoginModule |
| **Authorization** | Session-based (@LoggedIn) | AccountController.loggedinCustomer |
| **Password Storage** | Plaintext (SECURITY RISK) | No encryption implemented |
| **Session Management** | HTTP Session + CDI | @SessionScoped controllers |

### 5.2 Persistence

| Aspect | Configuration | Behavior |
|--------|---------------|----------|
| **JPA Provider** | EclipseLink/Hibernate/OpenJPA | persistence.xml properties |
| **Database** | Derby (embedded) | java:global/jdbc/applicationPetstoreDS |
| **Schema Generation** | DDL auto-generation | drop-and-create-tables |
| **Connection Pooling** | Application server managed | JTA datasource |

### 5.3 Validation

| Framework | Usage | Annotations |
|-----------|-------|-------------|
| **Bean Validation** | Entity-level | @NotNull, @Size, @Email, @Price |
| **Custom Constraints** | Domain-specific | @Login, @NotEmpty, @Email |
| **Manual Validation** | Service-layer | ValidationException throwing |

### 5.4 Caching and Performance

| Aspect | Implementation | Risks |
|--------|----------------|-------|
| **Entity Caching** | JPA L1 cache (default) | No L2 cache configured |
| **Query Performance** | Named queries | N+1 issues with lazy loading |
| **Session Scope** | Long-lived controllers | Memory accumulation risk |

### 5.5 Error Handling

| Pattern | Implementation | Coverage |
|---------|----------------|----------|
| **Exception Handling** | @CatchException interceptor | Controller-level |
| **Validation Errors** | FacesMessage integration | UI feedback |
| **System Errors** | ValidationException | Service-layer |

## 6. Traceability Map

### 6.1 Domain Model → Code

| Specification Item | Code Location | Type |
|-------------------|---------------|------|
| Customer Entity | `org.agoncal.application.petstore.domain.Customer` | JPA Entity |
| Category Entity | `org.agoncal.application.petstore.domain.Category` | JPA Entity |
| Product Entity | `org.agoncal.application.petstore.domain.Product` | JPA Entity |
| Item Entity | `org.agoncal.application.petstore.domain.Item` | JPA Entity |
| Order Entity | `org.agoncal.application.petstore.domain.Order` | JPA Entity |
| OrderLine Entity | `org.agoncal.application.petstore.domain.OrderLine` | JPA Entity |
| CartItem Value Object | `org.agoncal.application.petstore.domain.CartItem` | POJO |
| Address Value Object | `org.agoncal.application.petstore.domain.Address` | @Embeddable |
| CreditCard Value Object | `org.agoncal.application.petstore.domain.CreditCard` | @Embeddable |

### 6.2 Use Cases → Code

| Use Case | Controller Method | Service Method | View |
|----------|-------------------|----------------|------|
| Register Customer | `AccountController.doCreateNewAccount()` | `CustomerService.createCustomer()` | createaccount.xhtml |
| Customer Login | `AccountController.doLogin()` | `CustomerService.findCustomer()` | signon.xhtml → main.xhtml |
| Browse Categories | Direct navigation | `CatalogService.findAllCategories()` | main.xhtml |
| View Products | `CatalogController.doFindProducts()` | `CatalogService.findProducts()` | showproducts.xhtml |
| Search Items | `CatalogController.doSearch()` | `CatalogService.searchItems()` | searchresult.xhtml |
| Add to Cart | `ShoppingCartController.addItemToCart()` | `CatalogService.findItem()` | showcart.xhtml |
| Checkout | `ShoppingCartController.confirmOrder()` | `OrderService.createOrder()` | orderconfirmed.xhtml |

### 6.3 API Contracts → Code

| API Endpoint | Controller Class | Service Method | HTTP Mapping |
|--------------|------------------|----------------|--------------|
| GET /catalog/categories | `CatalogRestService` | `CatalogService.findAllCategories()` | @GET @Path("/categories") |
| POST /catalog/category | `CatalogRestService` | `CatalogService.createCategory()` | @POST @Path("/category") |
| GET /catalog/items | `CatalogRestService` | `CatalogService.findAllItems()` | @GET @Path("/items") |

### 6.4 Configuration → Code

| Configuration Aspect | File Location | Purpose |
|---------------------|---------------|---------|
| JPA Configuration | `src/main/resources/META-INF/persistence.xml` | Database connection, ORM settings |
| JSF Configuration | `src/main/webapp/WEB-INF/faces-config.xml` | JSF navigation, managed beans |
| Web Configuration | `src/main/webapp/WEB-INF/web.xml` | Servlet mapping, welcome files |
| CDI Configuration | `src/main/resources/META-INF/beans.xml` | CDI container activation |
| Security Configuration | `src/main/resources/petstore-test.login` | JAAS login module configuration |

## Conclusion

This specification represents the complete behavioral and structural documentation of the petstore application as derived from the existing codebase. The application follows a traditional Java EE 6 architecture with JSF for presentation, CDI for dependency injection, JPA for persistence, and EJB for transaction management. The specification preserves all current functionality while identifying areas for modernization and improvement.