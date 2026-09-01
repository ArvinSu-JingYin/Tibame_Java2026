package com.tibame.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String root() {
        return "redirect:/ledger";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "登入與註冊 // SYS-AUTH");
        return "login";
    }

    @GetMapping("/ledger")
    public String ledgerPage(Model model) {
        model.addAttribute("pageTitle", "個人財務記帳工作台 // SYS-LEDGER");
        return "ledger";
    }
}
