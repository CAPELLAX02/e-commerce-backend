package com.capellax.ecommerce.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.capellax.ecommerce.model.LocalUser;
import com.capellax.ecommerce.model.dao.LocalUserDAO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
public class JWTServiceTest {

    @Autowired
    private JWTService jwtService;

    @Autowired
    private LocalUserDAO localUserDAO;

    @Value("${jwt.algorithm.key}")
    private String algorithmKey;

    @Test
    public void testVerificationTokenNotUsableForLogin() {
        LocalUser user = localUserDAO.findByUsernameIgnoreCase("UserA").get();
        String token = jwtService.generateVerificationJWT(user);
        Assertions.assertNull(
                jwtService.getUsername(token),
                "Verification token should not contain username."
        );
    }

    @Test
    public void testAuthTokenReturnsUsername() {
        LocalUser user = localUserDAO.findByUsernameIgnoreCase("UserA").get();
        String token = jwtService.generateJWT(user);
        Assertions.assertEquals(
                user.getUsername(),
                jwtService.getUsername(token),
                "Token for authentication should contain users username."
        );
    }

    @Test
    public void testJWTNotGeneratedByUs() {
        String token = JWT.create()
                .withClaim("USERNAME", "UserA")
                .sign(Algorithm.HMAC256("NotTheRealSecret"));

        Assertions.assertThrows(
                SignatureVerificationException.class,
                () -> jwtService.getUsername(token)
        );
    }

    @Test
    public void testJWTCorrectlySignedNoIssuer() {
        String token = JWT.create()
                .withClaim("USERNAME", "UserA")
                .sign(Algorithm.HMAC256(algorithmKey));

        Assertions.assertThrows(
                MissingClaimException.class,
                () -> jwtService.getUsername(token)
        );
    }

}
