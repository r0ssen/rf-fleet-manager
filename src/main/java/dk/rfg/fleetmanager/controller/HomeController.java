package dk.rfg.fleetmanager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth != null && isDriverOnly(auth)) {
            return "redirect:/tasks";
        }
        return "redirect:/fleet";
    }

    private boolean isDriverOnly(Authentication auth) {
        var authorities = auth.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList();
        return authorities.contains("ROLE_DRIVER") && !authorities.contains("ROLE_ADMIN");
    }

    @GetMapping("/login")
    public String login() { return "login"; }
}
