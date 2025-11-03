package org.agoncal.application.petstore.web;

import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.service.CustomerService;
import org.agoncal.application.petstore.util.Loggable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.Serializable;

/**
 * JSF managed bean controlling customer account operations including login, registration,
 * profile management, and logout. Integrates with JAAS security for authentication
 * and maintains session-scoped user state.
 * 
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link CustomerService} - Customer business operations</li>
 *   <li>{@link LoginContext} - JAAS authentication mechanism</li>
 *   <li>{@link Credentials} - User input for authentication</li>
 *   <li>{@link Conversation} - CDI conversation scope management</li>
 * </ul>
 * 
 * <p>State management:</p>
 * <ul>
 *   <li>Session-scoped for login state persistence</li>
 *   <li>Produces @LoggedIn Customer for injection throughout application</li>
 *   <li>Manages conversation scope for multi-step operations</li>
 * </ul>
 * 
 * <p>Security integration: Uses JAAS LoginModule for authentication with credential validation</p>
 * 
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Named
@SessionScoped
@Loggable
@CatchException
public class AccountController extends Controller implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private CustomerService customerService;

    @Inject
    private Credentials credentials;

    @Inject
    private Conversation conversation;

    @Produces
    @LoggedIn
    private Customer loggedinCustomer;

    @Inject
    @SessionScoped
    private transient LoginContext loginContext;

    // ======================================
    // =              Public Methods        =
    // ======================================

    /**
     * Authenticates user credentials through JAAS and establishes logged-in session.
     * 
     * @return navigation outcome to main.faces if successful, null to stay on current page
     * @throws LoginException if JAAS authentication fails
     * TODO: Risk - empty string check instead of null check for validation
     * TODO: Consider rate limiting for failed login attempts
     */
    public String doLogin() throws LoginException {
        if ("".equals(credentials.getLogin())) {
            addWarningMessage("id_filled");
            return null; // Stay on current page to show validation message
        }
        if ("".equals(credentials.getPassword())) {
            addWarningMessage("pwd_filled");
            return null; // Stay on current page to show validation message
        }

        // Delegate authentication to JAAS LoginModule
        loginContext.login();
        // Load customer profile after successful authentication
        loggedinCustomer = customerService.findCustomer(credentials.getLogin());
        return "main.faces";
    }

    /**
     * Validates new account credentials and prepares customer for registration.
     * Performs validation for login uniqueness and password confirmation.
     * 
     * @return navigation outcome to createaccount.faces for profile completion, null for validation errors
     * TODO: Consider extracting validation logic to separate validator class
     */
    public String doCreateNewAccount() {

        // Enforce unique login constraint
        if (customerService.doesLoginAlreadyExist(credentials.getLogin())) {
            addWarningMessage("login_exists");
            return null;
        }

        // Validate required credential fields
        if ("".equals(credentials.getLogin()) || "".equals(credentials.getPassword()) || "".equals(credentials.getPassword2())) {
            addWarningMessage("id_pwd_filled");
            return null;
        } else if (!credentials.getPassword().equals(credentials.getPassword2())) {
            // Ensure password confirmation matches
            addWarningMessage("both_pwd_same");
            return null;
        }

        // Initialize new customer with validated credentials
        loggedinCustomer = new Customer();
        loggedinCustomer.setLogin(credentials.getLogin());
        loggedinCustomer.setPassword(credentials.getPassword());

        return "createaccount.faces"; // Navigate to profile completion page
    }

    public String doCreateCustomer() {
        loggedinCustomer = customerService.createCustomer(loggedinCustomer);
        return "main.faces";
    }


    /**
     * Logs out the current user and cleans up session state.
     * Ends any active CDI conversation and clears the logged-in customer.
     * 
     * @return navigation outcome to main.faces (home page)
     * TODO: Consider invalidating entire HTTP session for complete cleanup
     */
    public String doLogout() {
        loggedinCustomer = null; // Clear logged-in user state
        // Clean up any active CDI conversation scope
        if (!conversation.isTransient()) {
            conversation.end();
        }
        addInformationMessage("been_loggedout");
        return "main.faces";
    }

    public String doUpdateAccount() {
        loggedinCustomer = customerService.updateCustomer(loggedinCustomer);
        addInformationMessage("account_updated");
        return "showaccount.faces";
    }

    public boolean isLoggedIn() {
        return loggedinCustomer != null;
    }

    public Customer getLoggedinCustomer() {
        return loggedinCustomer;
    }

    public void setLoggedinCustomer(Customer loggedinCustomer) {
        this.loggedinCustomer = loggedinCustomer;
    }
}
