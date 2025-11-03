# JSF Navigation Contract Analysis

## Overview

This document captures the navigation patterns and EL (Expression Language) dependencies in the Java EE 6 Petstore JSF application. Since `faces-config.xml` contains no explicit navigation rules, all navigation is implicit based on action method return values and view IDs.

## Navigation Rule Analysis

### Implicit Navigation Rules

The application uses JSF 2.0 implicit navigation where:
- Action methods return outcome strings that map directly to view IDs
- Null returns stay on the same page  
- View transitions follow the pattern: `outcome` → `/outcome.xhtml`

### Core Navigation Flows

#### 1. Authentication Flow
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| signon.xhtml | `#{accountController.doLogin}` | "main" / null | main.xhtml / signon.xhtml | Successful login → main, failed → stay |
| signon.xhtml | `#{accountController.doCreateNewAccount}` | "createaccount" | createaccount.xhtml | Navigate to registration form |
| createaccount.xhtml | `#{accountController.doCreateCustomer}` | "main" / "createaccount" | main.xhtml / createaccount.xhtml | Success → main, validation error → stay |
| * | `#{accountController.doLogout}` | "main" | main.xhtml | Logout from any page → main |

#### 2. Catalog Browsing Flow  
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| main.xhtml | `#{catalogController.doSearch}` | "searchresult" | searchresult.xhtml | Product search results |
| main.xhtml | Category selection | "showproducts" | showproducts.xhtml | Browse products by category |
| showproducts.xhtml | Product selection | "showitems" | showitems.xhtml | Show items for selected product |
| showitems.xhtml | `#{catalogController.doFindItem}` | "showitem" | showitem.xhtml | Item detail view |

#### 3. Shopping Cart Flow
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| showitem.xhtml | `#{shoppingCartController.addItemToCart}` | null / "showcart" | stay / showcart.xhtml | Add item, optionally go to cart |
| showitems.xhtml | `#{shoppingCartController.addItemToCart}` | null | stay | Add item, stay on product list |
| showcart.xhtml | `#{shoppingCartController.updateQuantity}` | null | stay | Update item quantity |
| showcart.xhtml | `#{shoppingCartController.removeItemFromCart}` | null | stay | Remove item from cart |
| showcart.xhtml | `#{shoppingCartController.checkout}` | "confirmorder" | confirmorder.xhtml | Proceed to checkout |

#### 4. Order Processing Flow
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| confirmorder.xhtml | `#{shoppingCartController.confirmOrder}` | "orderconfirmed" | orderconfirmed.xhtml | Complete order |

#### 5. Account Management Flow  
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| showaccount.xhtml | Static link | "updateaccount" | updateaccount.xhtml | Edit account form |
| updateaccount.xhtml | `#{accountController.doUpdateAccount}` | "showaccount" / null | showaccount.xhtml / stay | Update success / validation error |

#### 6. Locale/Language Flow
| From View | Action Method | Expected Outcome | Target View | Description |
|-----------|---------------|------------------|-------------|-------------|
| * | `#{localeBean.setLanguage('fr')}` | null | stay | Switch to French, stay on page |
| * | `#{localeBean.setLanguage('en')}` | null | stay | Switch to English, stay on page |

## Navigation Contract Assumptions

### Security-Based Navigation
- **Login Required Pages**: `showcart.xhtml`, `confirmorder.xhtml`, `showaccount.xhtml`, `updateaccount.xhtml`
- **Public Pages**: `main.xhtml`, `signon.xhtml`, `createaccount.xhtml`, `showproducts.xhtml`, `showitems.xhtml`, `showitem.xhtml`
- **Post-Auth Redirects**: Failed access to secured pages should redirect to `signon.xhtml`

### Error Handling Navigation  
- **Validation Errors**: Stay on current page, display validation messages
- **Service Exceptions**: Default error handling (likely container error pages)
- **Not Found**: Missing entities should handle gracefully (implementation dependent)

### Session State Dependencies
- **Login State**: Managed by `@SessionScoped AccountController` 
- **Shopping Cart**: Managed by `@ConversationScoped ShoppingCartController`
- **Current Selection**: Category/Product/Item context maintained in controllers

## Business Rules Affecting Navigation

### Authentication Rules
1. **Login Success**: Valid credentials → `main.xhtml`  
2. **Login Failure**: Invalid credentials → stay on `signon.xhtml` with error message
3. **Registration Success**: New account created → automatic login → `main.xhtml`
4. **Registration Failure**: Validation errors → stay on `createaccount.xhtml`

### Shopping Cart Rules  
1. **Empty Cart Checkout**: Should prevent navigation to `confirmorder.xhtml`
2. **Guest Checkout**: Non-authenticated users must login before checkout
3. **Order Completion**: Successful order → clear cart → `orderconfirmed.xhtml`
4. **Order Failure**: Stay on `confirmorder.xhtml` with error message

### Catalog Browsing Rules
1. **Invalid Category**: Non-existent category → empty product list or error
2. **Invalid Product**: Non-existent product → empty item list or error  
3. **Invalid Item**: Non-existent item → error or redirect to parent product

## Expected Navigation Contracts for Testing

### Contract Assertions Needed
1. **Login Flow**: `signon.xhtml` → successful auth → `main.xhtml`
2. **Registration Flow**: `signon.xhtml` → `createaccount.xhtml` → success → `main.xhtml`
3. **Catalog Flow**: `main.xhtml` → category → `showproducts.xhtml` → product → `showitems.xhtml` → item → `showitem.xhtml`
4. **Purchase Flow**: `showitem.xhtml` → add to cart → `showcart.xhtml` → checkout → `confirmorder.xhtml` → confirm → `orderconfirmed.xhtml`
5. **Account Flow**: authenticated → `showaccount.xhtml` → edit → `updateaccount.xhtml` → save → `showaccount.xhtml`

### Negative Flow Contracts
1. **Unauthorized Access**: Direct URL to secured page → redirect to `signon.xhtml`  
2. **Invalid Credentials**: Login attempt → stay on `signon.xhtml` with error
3. **Empty Cart Checkout**: Checkout with no items → prevent navigation or show error
4. **Validation Failures**: Form submission with invalid data → stay on current page

These contracts will be implemented as JUnit tests that parse the XHTML files and validate the expected navigation behavior without requiring a running JSF container.