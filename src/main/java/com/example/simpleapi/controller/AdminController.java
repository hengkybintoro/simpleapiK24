package com.example.simpleapi.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.simpleapi.entity.Member;
import com.example.simpleapi.service.MemberService;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private MemberService memberService;

    // Admin home page
    @GetMapping
    public String viewAdmins(Model model) {
        List<Member> members = memberService.findAll();
        model.addAttribute("members", members);
        return "admin/view";
    }
}
