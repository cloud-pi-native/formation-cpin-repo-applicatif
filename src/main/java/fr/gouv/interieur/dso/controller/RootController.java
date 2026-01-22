package fr.gouv.interieur.dso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String redirectToDemo() {
        return "redirect:/api/demo/demo";
    }
}
