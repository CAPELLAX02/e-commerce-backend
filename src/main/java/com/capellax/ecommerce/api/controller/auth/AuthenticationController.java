package com.capellax.ecommerce.api.controller.auth;

import com.capellax.ecommerce.api.model.LoginBody;
import com.capellax.ecommerce.api.model.LoginResponse;
import com.capellax.ecommerce.api.model.RegistrationBody;
import com.capellax.ecommerce.exception.UserAlreadyExistsException;
import com.capellax.ecommerce.model.LocalUser;
import com.capellax.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(
            @Valid @RequestBody RegistrationBody registrationBody
    ) {
        try {
            userService.registerUser(registrationBody);
            return ResponseEntity.ok().build();
        } catch (UserAlreadyExistsException exp) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginBody loginBody
    ) {
        String jwt = userService.loginUser(loginBody);
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setJwt(jwt);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/me")
    public LocalUser getLoggedInUserProfile(
            @AuthenticationPrincipal LocalUser user
    ) {
        return user;
    }

}
