package com.capellax.ecommerce.service;

import com.capellax.ecommerce.api.model.LoginBody;
import com.capellax.ecommerce.exception.EmailFailureException;
import com.capellax.ecommerce.exception.UserNotVerifiedException;
import com.capellax.ecommerce.model.VerificationToken;
import com.capellax.ecommerce.model.dao.VerificationTokenDAO;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.capellax.ecommerce.api.model.RegistrationBody;
import com.capellax.ecommerce.exception.UserAlreadyExistsException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.eclipse.angus.mail.imap.protocol.BODY;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class UserServiceTest {

    @RegisterExtension
    private static final GreenMailExtension greenMailExtension = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(
                    GreenMailConfiguration.
                            aConfig().
                            withUser("springboot", "secret")
            )
            .withPerMethodLifecycle(true);

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationTokenDAO verificationTokenDAO;

    @Test
    @Transactional
    public void testRegisterUser() throws MessagingException {
        RegistrationBody registrationBody = new RegistrationBody();

        registrationBody.setUsername("UserA");
        registrationBody.setEmail("UserServiceTest$testRegisterUser@junit.com");
        registrationBody.setFirstName("FirstName");
        registrationBody.setLastName("LastName");
        registrationBody.setPassword("MySecretPassword123");

        Assertions.assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(registrationBody), "Username should already be in use."
        );

        registrationBody.setUsername("UserServiceTest$testRegisterUser");
        registrationBody.setEmail("UserA@junit.com");

        Assertions.assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(registrationBody), "Email should already be in use.");

        registrationBody.setEmail("UserServiceTest$testRegisterUser@junit.com");

        Assertions.assertDoesNotThrow(
                () -> userService.registerUser(registrationBody), "User should register successfully."
        );

        Assertions.assertEquals(
                registrationBody.getEmail(),
                greenMailExtension
                        .getReceivedMessages()[0]
                        .getRecipients(Message.RecipientType.TO)[0]
                        .toString());

    }

    @Test
    @Transactional
    public void testLoginUser() throws UserNotVerifiedException, EmailFailureException {
        LoginBody loginBody = new LoginBody();

        loginBody.setUsername("UserA-NotExists");
        loginBody.setPassword("PasswordA123-BadPassword");

        Assertions.assertNull(
                userService.loginUser(loginBody),
                "The user should not exist."
        );

        loginBody.setUsername("UserA");

        Assertions.assertNull(
                userService.loginUser(loginBody),
                "The password should be incorrect."
        );

        loginBody.setPassword("PasswordA123");

        Assertions.assertNotNull(
                userService.loginUser(loginBody),
                "The user should login successfully."
        );

        loginBody.setUsername("UserB");
        loginBody.setPassword("PasswordB123");

        try {
            userService.loginUser(loginBody);
            Assertions.assertTrue(
                    false,
                    "User should not have email verified."
            );
        } catch (UserNotVerifiedException exception) {
            Assertions.assertTrue(
                    exception.isNewEmailSent(),
                    "Email verification should be sent."
            );
        }

        try {
            userService.loginUser(loginBody);
            Assertions.assertTrue(
                    false,
                    "User should not have email verified."
            );
        } catch (UserNotVerifiedException exception) {
            Assertions.assertFalse(
                    exception.isNewEmailSent(),
                    "Email verification should not be resent."
            );
            Assertions.assertEquals(
                    1,
                    greenMailExtension.getReceivedMessages().length
            );
        }
    }

    @Test
    @Transactional
    public void testVerifyUser() throws EmailFailureException {
        Assertions.assertFalse(
                userService.verifyUser("Bad Token"),
                "Token that is bad or does not exist should return false."
        );

        LoginBody loginBody = new LoginBody();

        loginBody.setUsername("UserB");
        loginBody.setPassword("PasswordB123");

        try {
            userService.loginUser(loginBody);
            Assertions.assertTrue(
                    false,
                    "User should not have email verified."
            );
        } catch (UserNotVerifiedException exception) {
            List<VerificationToken> tokens = verificationTokenDAO.findByUser_IdOrderByIdDesc(2L);
            String token = tokens.get(0).getToken();
            Assertions.assertTrue(
                    userService.verifyUser(token),
                    "Token should be valid."
            );
            Assertions.assertNotNull(
                    loginBody,
                    "The user should now be verified."
            );
        }
    }

}