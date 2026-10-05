package com.cba.sdae.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomeController {

    @GetMapping("/welcome")
    public String welcome() {
        return "login/welcome"; // arquivo em templates/login/welcome.html
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/welcome";
    }
}