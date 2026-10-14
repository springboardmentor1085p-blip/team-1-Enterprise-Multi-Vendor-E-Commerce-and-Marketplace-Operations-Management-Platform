package com.karthi.ecommerce.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.karthi.ecommerce.Model.User;

@Controller
public class UserController {


    @PostMapping
    public ResponseEntity<String> user(@RequestBody User user) {
        return ResponseEntity.ok("User registered successfully");
    }
}
