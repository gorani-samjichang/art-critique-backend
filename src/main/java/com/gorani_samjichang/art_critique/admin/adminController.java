package com.gorani_samjichang.art_critique.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class adminController {
    @GetMapping("/isAdmin")
    ResponseEntity<Void> isAmdin() {
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }
}
