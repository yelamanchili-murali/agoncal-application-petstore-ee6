# JSF Expression Language (EL) Reference Index

## Overview

This document catalogs all EL expressions found in the JSF views, documenting the coupling between the presentation layer and backing beans. This index will help identify dependencies that need to be maintained or refactored during the Spring Boot migration.

## Backing Bean References

### AccountController (Session Scoped)
**Bean Name**: `accountController`  
**Scope**: `@SessionScoped`  
**Class**: `org.agoncal.application.petstore.web.AccountController`

#### Methods Referenced
- `#{accountController.doLogin}` - User authentication
- `#{accountController.doCreateNewAccount}` - Navigate to registration  
- `#{accountController.doCreateCustomer}` - Register new customer
- `#{accountController.doUpdateAccount}` - Update user profile
- `#{accountController.doLogout}` - End user session

#### Properties Referenced
- `#{accountController.customer}` - Current logged-in customer (if any)
- `#{accountController.loggedIn}` - Authentication state boolean

### CatalogController (Conversation Scoped)
**Bean Name**: `catalogController`  
**Scope**: `@ConversationScoped` 
**Class**: `org.agoncal.application.petstore.web.CatalogController`

#### Methods Referenced
- `#{catalogController.doSearch}` - Product search functionality
- `#{catalogController.doFindItem}` - Item detail navigation

#### Properties Referenced  
- `#{catalogController.keyword}` - Search term input
- `#{catalogController.categories}` - Available categories list
- `#{catalogController.products}` - Current product list  
- `#{catalogController.items}` - Current item list
- `#{catalogController.item}` - Selected item details

### ShoppingCartController (Conversation Scoped)
**Bean Name**: `shoppingCartController`
**Scope**: `@ConversationScoped`
**Class**: `org.agoncal.application.petstore.web.ShoppingCartController`

#### Methods Referenced
- `#{shoppingCartController.addItemToCart}` - Add product to cart
- `#{shoppingCartController.updateQuantity}` - Modify item quantities  
- `#{shoppingCartController.removeItemFromCart}` - Remove cart items
- `#{shoppingCartController.checkout}` - Begin checkout process
- `#{shoppingCartController.confirmOrder}` - Complete order

#### Properties Referenced
- `#{shoppingCartController.cartItems}` - Shopping cart contents
- `#{shoppingCartController.total}` - Cart total amount
- `#{shoppingCartController.customer}` - Customer for order (merged from session)
- `#{shoppingCartController.customer.firstname}` - Customer first name
- `#{shoppingCartController.customer.lastname}` - Customer last name  
- `#{shoppingCartController.customer.email}` - Customer email
- `#{shoppingCartController.customer.homeAddress.street1}` - Address line 1
- `#{shoppingCartController.customer.homeAddress.street2}` - Address line 2
- `#{shoppingCartController.customer.homeAddress.city}` - City
- `#{shoppingCartController.customer.homeAddress.state}` - State  
- `#{shoppingCartController.customer.homeAddress.zipcode}` - ZIP code
- `#{shoppingCartController.customer.homeAddress.country}` - Country
- `#{shoppingCartController.creditCard.creditCardNumber}` - Card number
- `#{shoppingCartController.creditCard.creditCardType}` - Card type
- `#{shoppingCartController.creditCard.creditCardExpiryDate}` - Expiry date

### LocaleBean (Session Scoped)
**Bean Name**: `localeBean`
**Scope**: `@SessionScoped`
**Class**: `org.agoncal.application.petstore.web.LocaleBean`

#### Methods Referenced  
- `#{localeBean.setLanguage('fr')}` - Switch to French locale
- `#{localeBean.setLanguage('en')}` - Switch to English locale

### Credentials (Request/Conversation Scoped)
**Bean Name**: `credentials`  
**Scope**: `@Named` (likely Request scoped)
**Class**: `org.agoncal.application.petstore.web.Credentials`

#### Properties Referenced
- `#{credentials.login}` - User login input
- `#{credentials.password}` - User password input  
- `#{credentials.password2}` - Password confirmation input

## Internationalization (i18n) References

### Resource Bundle Configuration
**Bundle Name**: `i18n`  
**Base Name**: `messages`  
**Files**: `messages_en.properties`, `messages_fr.properties`

### i18n Keys Used in Views

#### Authentication & Account
- `#{i18n.signon_signIn}` - "Sign In" label
- `#{i18n.signon_returningCustomer}` - "Returning Customer" label  
- `#{i18n.signon_signup}` - "Sign Up" label
- `#{i18n.signon_new}` - "New Account" button
- `#{i18n.yes}` - "Yes" confirmation
- `#{i18n.again}` - "Again" (password confirmation)
- `#{i18n.submit}` - "Submit" button

#### Customer Fields
- `#{i18n.customer_login}` - "Login" field label
- `#{i18n.customer_password}` - "Password" field label
- `#{i18n.customer_firstname}` - "First Name" field label
- `#{i18n.customer_lastname}` - "Last Name" field label
- `#{i18n.customer_email}` - "Email" field label
- `#{i18n.customer_telephone}` - "Telephone" field label

#### Address Fields  
- `#{i18n.address_street1}` - "Street Address 1" field label
- `#{i18n.address_street2}` - "Street Address 2" field label  
- `#{i18n.address_city}` - "City" field label
- `#{i18n.address_state}` - "State" field label
- `#{i18n.address_zipcode}` - "ZIP Code" field label
- `#{i18n.address_country}` - "Country" field label

#### Order & Cart
- `#{i18n.confirmorder_personalInformation}` - "Personal Information" section
- `#{i18n.confirmorder_deliveryAddress}` - "Delivery Address" section
- `#{i18n.orderConfirmed_yourOrderIsComplete}` - "Order Complete" message
- `#{i18n.search}` - "Search" button label

## View-Specific EL Expression Analysis

### signon.xhtml
**Primary Dependencies**:
- `credentials.login`, `credentials.password`, `credentials.password2`
- `accountController.doLogin`, `accountController.doCreateNewAccount`  
- i18n messages for labels and buttons

**Navigation Contracts**:
- Login success → implicit navigation to "main"
- Create account → implicit navigation to "createaccount"

### createaccount.xhtml  
**Primary Dependencies**:
- `accountController.customer.*` for new customer data binding
- `accountController.doCreateCustomer` for form submission
- i18n messages for form labels

**Navigation Contracts**:
- Successful registration → implicit navigation to "main"
- Validation errors → stay on current page

### main.xhtml
**Primary Dependencies**:
- `catalogController.keyword` for search input
- `catalogController.doSearch` for search submission  
- `catalogController.categories` for category navigation

**Navigation Contracts**:
- Search → implicit navigation to "searchresult"
- Category selection → implicit navigation to "showproducts"

### showproducts.xhtml
**Primary Dependencies**:
- `catalogController.products` for product listing
- Product selection actions → implicit navigation to "showitems"

### showitems.xhtml  
**Primary Dependencies**:
- `catalogController.items` for item listing
- `shoppingCartController.addItemToCart` for cart functionality
- Item selection → implicit navigation to "showitem"

### showitem.xhtml
**Primary Dependencies**:
- `catalogController.item` for item details
- `shoppingCartController.addItemToCart` for cart functionality

### showcart.xhtml
**Primary Dependencies**:
- `shoppingCartController.cartItems` for cart contents
- `shoppingCartController.total` for total calculation
- `shoppingCartController.updateQuantity`, `removeItemFromCart` for cart management
- `shoppingCartController.checkout` → implicit navigation to "confirmorder"

### confirmorder.xhtml
**Primary Dependencies**:
- `shoppingCartController.customer.*` for customer data (read-only display)  
- `shoppingCartController.customer.homeAddress.*` for delivery address
- `shoppingCartController.creditCard.*` for payment information
- `shoppingCartController.confirmOrder` → implicit navigation to "orderconfirmed"

### showaccount.xhtml & updateaccount.xhtml
**Primary Dependencies**:
- `accountController.customer.*` for profile data
- `accountController.doUpdateAccount` for profile updates
- Static navigation link to "updateaccount"

## Critical EL Dependencies for Migration

### Session State Management
1. **Authentication State**: `accountController.loggedIn` and `accountController.customer`
2. **Shopping Cart State**: `shoppingCartController.cartItems` and related properties
3. **Locale State**: `localeBean` for internationalization

### Form Data Binding
1. **Login Forms**: `credentials.*` properties  
2. **Customer Forms**: `accountController.customer.*` properties
3. **Address Forms**: `customer.homeAddress.*` properties  
4. **Payment Forms**: `shoppingCartController.creditCard.*` properties

### Navigation Actions
1. **Authentication**: All `accountController.do*` methods
2. **Catalog**: All `catalogController.do*` methods  
3. **Shopping**: All `shoppingCartController.do*` methods

### Collection Iteration
1. **Categories**: `catalogController.categories`
2. **Products**: `catalogController.products`  
3. **Items**: `catalogController.items`
4. **Cart Items**: `shoppingCartController.cartItems`

These EL expressions represent the contract between JSF views and the backing beans that must be maintained or appropriately transformed during the Spring Boot migration.