package com.sprint.frontend.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.sprint.frontend.DTO.MemberDTO;

import java.util.Arrays;
import java.util.List;

/**
 * Handles Page 1 (team grid) and Page 2 (member 3 detail only).
 * No service / repository layer — member data is hard-coded here for now.
 * Replace the buildMembers() list with an API call to your second PC later.
 */
@Controller
public class TeamController {

    // ── PAGE 1 : team grid ─────────────────────────────────────────────
    @GetMapping("/")
    public String teamPage(Model model) {
        model.addAttribute("members", buildMembers());
        return "index"; // → templates/index.html
    }

    // ── PAGE 2 : member detail (only member 3 allowed) ─────────────────
    @GetMapping("/member/{id}")
    public String memberDetail(@PathVariable int id, Model model) {

        // Guard: only member 3 has an active page
        if (id != 3) {
            // redirect all others back to the team page
            return "redirect:/";
        }

        MemberDTO member = buildMembers().stream()
                .filter(m -> m.getMemberNumber() == id)
                .findFirst()
                .orElse(null);

        if (member == null) {
            return "redirect:/";
        }

        model.addAttribute("member", member);
        return "member3"; // → templates/member3.html
    }

    // ── Helper: build the 6 member DTOs ───────────────────────────────
    // Replace imageUrl strings with real paths or remote URLs.
    // e.g. "/images/member1.jpg" or "https://server2/api/photo/1"
    private List<MemberDTO> buildMembers() {
        return Arrays.asList(
                new MemberDTO(1, "Ansh Verma", "Film", "/images/member1.jpg"),
                new MemberDTO(2, "Manan Kr. Agarwal", "Actor", "/images/member2.jpg"),
                new MemberDTO(3, "Aayush Saxena", "Customer", "/images/member3.jpg"),
                new MemberDTO(4, "Aniket Rathore", "Staff", "/images/member4.jpg"),
                new MemberDTO(5, "Mohd Amaan", "Store & Inventory", "/images/member5.jpg"),
                new MemberDTO(6, "Aurindum Bose", "Language", "/images/member6.jpg"));
    }
}
