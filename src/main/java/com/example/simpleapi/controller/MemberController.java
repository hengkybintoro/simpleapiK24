package com.example.simpleapi.controller;

import com.example.simpleapi.entity.Member;
import com.example.simpleapi.entity.MemberDto;
import com.example.simpleapi.service.MemberService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {
    @Autowired
    private MemberService service;

    // Member home page
    @GetMapping
    public String viewMembers(Model model, Authentication authentication) {
        Member member = service.findByEmail(authentication.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedBirthDate = member.getBirthDate().format(formatter);
        model.addAttribute("member", member);
        model.addAttribute("formattedBirthDate", formattedBirthDate);
        return "member/view";
    }

    @GetMapping("/edit")
    public String editMemberForm(@RequestParam("id") Long memberId, Authentication authentication, Model model) {
        Member member = service.findById(memberId).get();
        MemberDto parsed = service.parseMemberToDto(member);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedBirthDate = member.getBirthDate().format(formatter);

        model.addAttribute("member", parsed);
        model.addAttribute("formattedBirthDate", formattedBirthDate);

        if (isAdmin(authentication)) {
            model.addAttribute("isAdmin", true);
        }

        return "member/edit";
    }

    // Edit page
    @PostMapping("/edit")
    public String editMember(@RequestParam("id") Long memberId, MemberDto updatedMember, Authentication authentication) throws IOException {
        if (!updatedMember.getPhoto().isEmpty()) {
            if (updatedMember.getPhoto().getSize() > 1024 * 1024) {
                return "{\"success\": false, \"message\": \"Photo file size exceeds 1MB.\"}";
            }
        }

        if (updatedMember.getRole() == null) {
            updatedMember.setRole("MEMBER");
        }

        Member memberData = service.findById(memberId).get();
        
        updatedMember.setPassword(memberData.getPassword());

        if (updatedMember.getBirthDate() == null) {
            updatedMember.setBirthDate(memberData.getBirthDate());
        }

        if (updatedMember.getPhoto() == null) {
            updatedMember.setPhoto(service.parsePhoto(memberData.getName(), memberData.getPhoto()));
        }

        service.save(updatedMember, memberData.getId());
        
        if (isAdmin(authentication)) {
            return "redirect:/admin";
        } else {
            return "redirect:/member";
        }
    }

    // Delete member
    @GetMapping("/delete")
    public String deleteMember(@RequestParam("id") Long id) {
        service.deleteById(id);
        return "redirect:/admin";
    }

    @GetMapping("/register")
    public String showRegisterForm(@ModelAttribute("member") MemberDto member, Model model, Authentication authentication) {
        model.addAttribute("member", new Member());

        if (authentication != null && isAdmin(authentication)) {
            model.addAttribute("isAdmin", true);
        } else {
            model.addAttribute("isAdmin", false);
        }

        return "member/register";
    }

    // Register page
    @PostMapping("/register")
    public String registerMember(@ModelAttribute("member") MemberDto member, BindingResult result, @RequestParam("photo") MultipartFile photo, Model model, Authentication authentication) {
        if (!member.getPassword().equals(member.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match!");
        }

        if (photo.getSize() > 1048576) { // 1MB
            throw new IllegalArgumentException("Photo file size exceeds limit!");
        }

        if (result.hasErrors()) {
            return "member/register";
        }

        try {
            if (authentication == null) {
                member.setRole("MEMBER");
            }
            member.setPhoto(photo);
            service.save(member, 0);
            model.addAttribute("message", "Added Successfully");
            
            if (authentication != null && isAdmin(authentication)) {
                return "redirect:/admin";
            } else {
                return "redirect:/login";
            }
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "member/register";
        }
    }

    // JSON API to retrieve member that cointains "name"
    @GetMapping("/name")
    public ResponseEntity<List<Member>> viewMembersByName(@RequestParam String name) {
        List<Member> members = service.findUserContainsName(name);
        return ResponseEntity.ok(members);
    }

    // Export member data to excel
    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportToExcel(@RequestParam("id") Long id) throws IOException {
        Member member = service.findById(id).get();
        List<Member> listMember = new ArrayList<>();
        listMember.add(member);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Members");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Name");
        header.createCell(2).setCellValue("Email");
        header.createCell(3).setCellValue("Gender");
        header.createCell(4).setCellValue("KTP Number");
        header.createCell(5).setCellValue("Phone Number");
        header.createCell(6).setCellValue("Birth Date");

        int rowNum = 1;
        for (Member eachMember : listMember) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(eachMember.getId());
            row.createCell(1).setCellValue(eachMember.getName());
            row.createCell(2).setCellValue(eachMember.getEmail());
            row.createCell(3).setCellValue(eachMember.getGender());
            row.createCell(4).setCellValue(eachMember.getKtpNumber());
            row.createCell(5).setCellValue(eachMember.getPhoneNumber());
            row.createCell(6).setCellValue(eachMember.getBirthDate().toString());
        }

        workbook.write(out);
        workbook.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=members.xlsx");

        return new ResponseEntity<>(new InputStreamResource(in), headers, HttpStatus.OK);
    }

    // Export member data to pdf
    @GetMapping("/export/pdf")
    public void exportToPdf(@RequestParam("id") Long id, HttpServletResponse response) throws DocumentException, IOException {
        Member member = service.findById(id).get();
        List<Member> listMember = new ArrayList<>();
        listMember.add(member);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        PdfPTable table = new PdfPTable(7);

        table.addCell("ID");
        table.addCell("Name");
        table.addCell("Email");
        table.addCell("Gender");
        table.addCell("KTP Number");
        table.addCell("Phone Number");
        table.addCell("Birth Date");

        for (Member eachMember : listMember) {
            table.addCell(String.valueOf(eachMember.getId()));
            table.addCell(eachMember.getName());
            table.addCell(eachMember.getEmail());
            table.addCell(eachMember.getGender());
            table.addCell(eachMember.getKtpNumber());
            table.addCell(eachMember.getPhoneNumber());
            table.addCell(eachMember.getBirthDate().toString());
        }

        document.add(table);
        document.close();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=members.pdf");
        response.getOutputStream().write(out.toByteArray());
    }

    // Check role
    private Boolean isAdmin(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return true;
            }
        }

        return false;
    }
}
