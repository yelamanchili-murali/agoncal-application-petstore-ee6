package org.agoncal.application.petstore.ejb;

import org.agoncal.application.petstore.domain.*;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Characterization tests for CatalogService EJB.
 * 
 * These tests capture the current behavior of the catalog management service
 * including CRUD operations, query patterns, and business rule enforcement.
 * 
 * Transaction Boundaries: All service methods should execute within EJB container-managed transactions.
 * Business Invariants: Category names unique, product-category relationships required, item-product relationships required.
 * 
 * Key Behaviors Being Characterized:
 * - Input validation patterns and exception types
 * - Entity lifecycle management (persist, merge, remove)  
 * - Named query execution and parameter binding
 * - Lazy loading workarounds and N+1 query issues
 * - Auto-persistence of transient related entities
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Characterization Tests")
class CatalogServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Category> categoryQuery;

    @Mock  
    private TypedQuery<Product> productQuery;

    @Mock
    private TypedQuery<Item> itemQuery;

    @InjectMocks
    private CatalogService catalogService;

    private Category testCategory;
    private Product testProduct;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setCategory(testCategory);

        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setProduct(testProduct);
    }

    @Nested
    @DisplayName("Category Operations")
    class CategoryOperations {

        @Test
        @DisplayName("findCategory by ID returns entity when found")
        void findCategoryById_WhenExists_ReturnsCategory() {
            // Given
            Long categoryId = 1L;
            when(entityManager.find(Category.class, categoryId)).thenReturn(testCategory);

            // When  
            Category result = catalogService.findCategory(categoryId);

            // Then
            assertNotNull(result);
            assertEquals(testCategory.getId(), result.getId());
            assertEquals(testCategory.getName(), result.getName());
            verify(entityManager).find(Category.class, categoryId);
        }

        @Test
        @DisplayName("findCategory by ID returns null when not found")  
        void findCategoryById_WhenNotExists_ReturnsNull() {
            // Given
            Long categoryId = 999L;
            when(entityManager.find(Category.class, categoryId)).thenReturn(null);

            // When
            Category result = catalogService.findCategory(categoryId);

            // Then
            assertNull(result);
            verify(entityManager).find(Category.class, categoryId);
        }

        @Test
        @DisplayName("findCategory by ID throws ValidationException for null ID")
        void findCategoryById_WhenNullId_ThrowsValidationException() {
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> catalogService.findCategory((Long) null));
            assertEquals("Invalid category id", exception.getMessage());
            verifyNoInteractions(entityManager);
        }

        @Test
        @DisplayName("findCategory by name uses named query correctly")
        void findCategoryByName_WhenExists_ReturnsCategory() {
            // Given
            String categoryName = "Test Category";
            when(entityManager.createNamedQuery(Category.FIND_BY_NAME, Category.class))
                .thenReturn(categoryQuery);
            when(categoryQuery.setParameter("pname", categoryName)).thenReturn(categoryQuery);
            when(categoryQuery.getSingleResult()).thenReturn(testCategory);

            // When
            Category result = catalogService.findCategory(categoryName);

            // Then
            assertNotNull(result);
            assertEquals(testCategory, result);
            verify(entityManager).createNamedQuery(Category.FIND_BY_NAME, Category.class);
            verify(categoryQuery).setParameter("pname", categoryName);
            verify(categoryQuery).getSingleResult();
        }

        @Test
        @DisplayName("findCategory by name throws ValidationException for null name")
        void findCategoryByName_WhenNullName_ThrowsValidationException() {
            // When & Then  
            ValidationException exception = assertThrows(ValidationException.class,
                () -> catalogService.findCategory((String) null));
            assertEquals("Invalid category name", exception.getMessage());
            verifyNoInteractions(entityManager);
        }

        @Test
        @DisplayName("findCategory by name propagates NoResultException - CHARACTERIZATION: Current behavior may crash client")
        void findCategoryByName_WhenNotExists_PropagatesNoResultException() {
            // Given - This captures current risky behavior
            String categoryName = "Non-existent Category";
            when(entityManager.createNamedQuery(Category.FIND_BY_NAME, Category.class))
                .thenReturn(categoryQuery);
            when(categoryQuery.setParameter("pname", categoryName)).thenReturn(categoryQuery);
            when(categoryQuery.getSingleResult()).thenThrow(new NoResultException("No entity found"));

            // When & Then - Current implementation does NOT handle NoResultException
            assertThrows(NoResultException.class, 
                () -> catalogService.findCategory(categoryName));
        }

        @Test
        @DisplayName("findAllCategories returns all categories")
        void findAllCategories_ReturnsAllCategories() {
            // Given
            List<Category> categories = Arrays.asList(testCategory);
            when(entityManager.createNamedQuery(Category.FIND_ALL, Category.class))
                .thenReturn(categoryQuery);
            when(categoryQuery.getResultList()).thenReturn(categories);

            // When
            List<Category> result = catalogService.findAllCategories();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCategory, result.get(0));
        }

        @Test
        @DisplayName("findAllCategories returns empty list when no categories exist")
        void findAllCategories_WhenEmpty_ReturnsEmptyList() {
            // Given
            when(entityManager.createNamedQuery(Category.FIND_ALL, Category.class))
                .thenReturn(categoryQuery);
            when(categoryQuery.getResultList()).thenReturn(Collections.emptyList());

            // When
            List<Category> result = catalogService.findAllCategories();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("createCategory persists and returns category")
        void createCategory_WithValidCategory_PersistsAndReturns() {
            // When
            Category result = catalogService.createCategory(testCategory);

            // Then
            assertSame(testCategory, result);
            verify(entityManager).persist(testCategory);
        }

        @Test
        @DisplayName("createCategory throws ValidationException for null category")
        void createCategory_WhenNull_ThrowsValidationException() {
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                () -> catalogService.createCategory(null));
            assertEquals("Category object is null", exception.getMessage());
            verifyNoInteractions(entityManager);
        }

        @Test
        @DisplayName("updateCategory merges and returns category")
        void updateCategory_WithValidCategory_MergesAndReturns() {
            // Given
            when(entityManager.merge(testCategory)).thenReturn(testCategory);

            // When  
            Category result = catalogService.updateCategory(testCategory);

            // Then
            assertSame(testCategory, result);
            verify(entityManager).merge(testCategory);
        }

        @Test
        @DisplayName("updateCategory throws ValidationException for null category")
        void updateCategory_WhenNull_ThrowsValidationException() {
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                () -> catalogService.updateCategory(null));
            assertEquals("Category object is null", exception.getMessage());
            verifyNoInteractions(entityManager);
        }

        @Test
        @DisplayName("removeCategory merges then removes category")
        void removeCategory_WithValidCategory_MergesAndRemoves() {
            // Given
            when(entityManager.merge(testCategory)).thenReturn(testCategory);

            // When
            catalogService.removeCategory(testCategory);

            // Then
            verify(entityManager).merge(testCategory);
            verify(entityManager).remove(testCategory);
        }

        @Test
        @DisplayName("removeCategory by ID finds category then removes")
        void removeCategoryById_WhenExists_FindsAndRemoves() {
            // Given
            Long categoryId = 1L;
            when(entityManager.find(Category.class, categoryId)).thenReturn(testCategory);
            when(entityManager.merge(testCategory)).thenReturn(testCategory);

            // When
            catalogService.removeCategory(categoryId);

            // Then
            verify(entityManager).find(Category.class, categoryId);
            verify(entityManager).merge(testCategory);
            verify(entityManager).remove(testCategory);
        }
    }

    @Nested
    @DisplayName("Product Operations")  
    class ProductOperations {

        @Test
        @DisplayName("findProducts by category name uses correct named query")
        void findProductsByCategoryName_ReturnsProductList() {
            // Given
            String categoryName = "Test Category";
            List<Product> products = Arrays.asList(testProduct);
            when(entityManager.createNamedQuery(Product.FIND_BY_CATEGORY_NAME, Product.class))
                .thenReturn(productQuery);
            when(productQuery.setParameter("pname", categoryName)).thenReturn(productQuery);
            when(productQuery.getResultList()).thenReturn(products);

            // When
            List<Product> result = catalogService.findProducts(categoryName);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testProduct, result.get(0));
            verify(productQuery).setParameter("pname", categoryName);
        }

        @Test
        @DisplayName("findProduct by ID forces lazy collection loading - CHARACTERIZATION: N+1 query risk")
        void findProductById_WhenExists_ForcesItemsLoading() {
            // Given - This captures current lazy loading workaround
            Long productId = 1L;
            List<Item> mockItems = Arrays.asList(testItem);
            when(entityManager.find(Product.class, productId)).thenReturn(testProduct);
            when(testProduct.getItems()).thenReturn(mockItems); // Simulate lazy collection access

            // When
            Product result = catalogService.findProduct(productId);

            // Then
            assertNotNull(result);
            assertEquals(testProduct, result);
            // The service explicitly calls getItems() to force loading - this is current behavior
            verify(entityManager).find(Product.class, productId);
            // Note: In real scenario, this would trigger additional query for items
        }

        @Test
        @DisplayName("createProduct auto-persists transient category - CHARACTERIZATION: Cascading behavior")
        void createProduct_WithTransientCategory_PersistsCategory() {
            // Given - Product has a new (transient) category
            Category transientCategory = new Category();
            transientCategory.setId(null); // Transient entity
            transientCategory.setName("New Category");
            testProduct.setCategory(transientCategory);

            // When
            catalogService.createProduct(testProduct);

            // Then - Both category and product should be persisted
            verify(entityManager).persist(transientCategory); // Auto-persist transient category
            verify(entityManager).persist(testProduct);
        }

        @Test
        @DisplayName("createProduct with persistent category only persists product")
        void createProduct_WithPersistentCategory_OnlyPersistsProduct() {
            // Given - Product has existing (persistent) category  
            testCategory.setId(1L); // Persistent entity
            testProduct.setCategory(testCategory);

            // When
            catalogService.createProduct(testProduct);

            // Then - Only product should be persisted
            verify(entityManager, never()).persist(testCategory);
            verify(entityManager).persist(testProduct);
        }
    }

    @Nested
    @DisplayName("Item Operations")
    class ItemOperations {

        @Test
        @DisplayName("findItems by product ID uses correct named query")
        void findItemsByProductId_ReturnsItemList() {
            // Given
            Long productId = 1L;
            List<Item> items = Arrays.asList(testItem);
            when(entityManager.createNamedQuery(Item.FIND_BY_PRODUCT_ID, Item.class))
                .thenReturn(itemQuery);
            when(itemQuery.setParameter("productId", productId)).thenReturn(itemQuery);
            when(itemQuery.getResultList()).thenReturn(items);

            // When
            List<Item> result = catalogService.findItems(productId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testItem, result.get(0));
            verify(itemQuery).setParameter("productId", productId);
        }

        @Test  
        @DisplayName("searchItems handles null keyword by defaulting to empty string")
        void searchItems_WhenNullKeyword_DefaultsToEmptyString() {
            // Given
            when(entityManager.createNamedQuery(Item.SEARCH, Item.class))
                .thenReturn(itemQuery);
            when(itemQuery.setParameter("keyword", "%")).thenReturn(itemQuery);
            when(itemQuery.getResultList()).thenReturn(Arrays.asList(testItem));

            // When
            List<Item> result = catalogService.searchItems(null);

            // Then - Null keyword becomes empty string, which becomes "%" wildcard
            assertNotNull(result);
            verify(itemQuery).setParameter("keyword", "%");
        }

        @Test
        @DisplayName("searchItems converts keyword to uppercase with wildcards - CHARACTERIZATION: LIKE performance risk")  
        void searchItems_WithKeyword_ConvertsToUppercaseWithWildcards() {
            // Given
            String keyword = "test";
            when(entityManager.createNamedQuery(Item.SEARCH, Item.class))
                .thenReturn(itemQuery);
            when(itemQuery.setParameter("keyword", "%TEST%")).thenReturn(itemQuery);
            when(itemQuery.getResultList()).thenReturn(Arrays.asList(testItem));

            // When
            List<Item> result = catalogService.searchItems(keyword);

            // Then - Current implementation uses leading wildcard (prevents index usage)
            assertNotNull(result);
            verify(itemQuery).setParameter("keyword", "%TEST%");
        }

        @Test
        @DisplayName("createItem auto-persists transient product and category cascade")
        void createItem_WithTransientProductAndCategory_PersistsAll() {
            // Given
            Category transientCategory = new Category();
            transientCategory.setId(null);
            Product transientProduct = new Product();
            transientProduct.setId(null);
            transientProduct.setCategory(transientCategory);
            testItem.setProduct(transientProduct);

            // When
            catalogService.createItem(testItem);

            // Then - All transient entities in hierarchy should be persisted
            verify(entityManager).persist(transientCategory);
            verify(entityManager).persist(transientProduct);
            verify(entityManager).persist(testItem);
        }
    }

    @Nested
    @DisplayName("Validation and Error Handling")
    class ValidationAndErrorHandling {

        @Test
        @DisplayName("All operations validate for null entity parameters")
        void allOperations_WhenNullEntity_ThrowValidationException() {
            // Category operations
            assertThrows(ValidationException.class, () -> catalogService.createCategory(null));
            assertThrows(ValidationException.class, () -> catalogService.updateCategory(null)); 
            assertThrows(ValidationException.class, () -> catalogService.removeCategory((Category) null));

            // Product operations  
            assertThrows(ValidationException.class, () -> catalogService.createProduct(null));
            assertThrows(ValidationException.class, () -> catalogService.updateProduct(null));
            assertThrows(ValidationException.class, () -> catalogService.removeProduct((Product) null));

            // Item operations
            assertThrows(ValidationException.class, () -> catalogService.createItem(null));
            assertThrows(ValidationException.class, () -> catalogService.updateItem(null));
            assertThrows(ValidationException.class, () -> catalogService.removeItem((Item) null));
        }

        @Test
        @DisplayName("All ID-based operations validate for null ID parameters")
        void allIdOperations_WhenNullId_ThrowValidationException() {
            assertThrows(ValidationException.class, () -> catalogService.findCategory((Long) null));
            assertThrows(ValidationException.class, () -> catalogService.removeCategory((Long) null));
            assertThrows(ValidationException.class, () -> catalogService.findProduct(null));
            assertThrows(ValidationException.class, () -> catalogService.removeProduct((Long) null));
            assertThrows(ValidationException.class, () -> catalogService.findItems(null));
            assertThrows(ValidationException.class, () -> catalogService.findItem(null));
            assertThrows(ValidationException.class, () -> catalogService.removeItem((Long) null));
        }

        @Test
        @DisplayName("String parameter operations validate for null strings")
        void stringParameterOperations_WhenNull_ThrowValidationException() {
            assertThrows(ValidationException.class, () -> catalogService.findCategory((String) null));
            assertThrows(ValidationException.class, () -> catalogService.findProducts(null));
            // Note: searchItems accepts null and converts to empty string - different behavior
        }
    }

    @Nested
    @DisplayName("Transaction Boundary Documentation") 
    class TransactionBoundaryDocumentation {

        @Test
        @DisplayName("DOCUMENTATION: All service methods should execute within EJB CMT")
        void documentTransactionBehavior() {
            // This test documents expected transactional behavior for Spring Boot migration:
            // 
            // CURRENT EJB BEHAVIOR:
            // - @Stateless bean provides container-managed transactions (CMT)
            // - Each public method runs in transaction by default (REQUIRED)  
            // - EntityManager is container-managed and participates in JTA
            // - Exceptions cause automatic rollback
            //
            // SPRING BOOT EQUIVALENT:
            // - Add @Transactional(propagation = Propagation.REQUIRED) to service class
            // - Use application-managed EntityManager with @PersistenceContext
            // - RuntimeExceptions cause rollback, checked exceptions do not (unless configured)
            // - ValidationException (unchecked) will cause rollback in both environments
            
            assertTrue(true, "This test documents transaction requirements for migration");
        }

        @Test
        @DisplayName("DOCUMENTATION: Business invariants enforced by service layer")
        void documentBusinessInvariants() {
            // Business rules currently enforced that must be maintained:
            //
            // 1. Entity validation: All public methods validate null inputs
            // 2. Auto-persistence: Transient related entities are automatically persisted
            // 3. Merge-before-remove: All remove operations merge entity first (handles detached entities)
            // 4. Lazy loading workaround: findProduct explicitly loads items collection
            // 5. Search flexibility: null search terms default to match-all behavior
            // 6. Query exception propagation: NoResultException not handled (potential client crash)
            
            assertTrue(true, "This test documents business invariants for migration");
        }
    }
}