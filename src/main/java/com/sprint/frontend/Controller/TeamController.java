package com.sprint.frontend.Controller;

import com.sprint.frontend.DTO.MemberDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class TeamController {

    // ── HOME PAGE ─────────────────────────────────────────────
    @GetMapping("/")
    public String teamPage(Model model) {
        model.addAttribute("members", buildMembers());

        // ❌ was: return "team";
        // ✅ correct file is: index.html
        return "index";
    }

    // ── MEMBER DETAIL PAGE ────────────────────────────────────
    @GetMapping("/member/{id}")
    public String memberDetail(@PathVariable int id, Model model) {

        List<MemberDTO> members = buildMembers();

        MemberDTO member = members.stream()
                .filter(m -> m.getMemberNumber() == id)
                .findFirst()
                .orElse(null);

        // only allow member 3 (as per your logic)
        if (member == null || id != 3) {
            return "redirect:/";
        }

        model.addAttribute("member", member);

        // dummy table data (you are not using it in UI yet, so keep it)
        List<Map<String, String>> data = new ArrayList<>();

        data.add(Map.of(
                "id", "1",
                "name", "Sample A",
                "field1", "Value 1",
                "field2", "Value 2"));

        data.add(Map.of(
                "id", "2",
                "name", "Sample B",
                "field1", "Value 3",
                "field2", "Value 4"));

        model.addAttribute("data", data);

        // ❌ was: "member-detail"
        // ✅ correct file is: member3.html
        return "member3";
    }

    // ── MEMBER DATA ───────────────────────────────────────────
    private List<MemberDTO> buildMembers() {
        return Arrays.asList(
                new MemberDTO(1, "Ansh Verma", "Film",
                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRMkg4yY-1vpEMgzIV1GyLIHgGfzPafZ7c4lA&s"),

                new MemberDTO(2, "Manan Kr. Agarwal", "Actor",
                        "https://pbs.twimg.com/profile_images/1168413160019091456/HmkFFlqY_400x400.jpg"),

                new MemberDTO(3, "Aayush Saxena", "Customer",
                        "https://i.pinimg.com/originals/b5/1b/c7/b51bc7c7f77ef1d955e9a2e1b4caa64e.jpg"),

                new MemberDTO(4, "Aniket Rathore", "Staff",
                        "https://i.quotev.com/wgx7jg4haaaa.jpg"),

                new MemberDTO(5, "Mohd Amaan", "Store & Inventory",
                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR4fGl36r2h3AqOEeZVvNE8rvAzn041njqYww&s"),

                new MemberDTO(6, "Aurindum Bose", "Language",
                        "https://media.tenor.com/ba4PQ9G4sksAAAAe/itachi-itachi-spanch-bob.png"));
    }
}