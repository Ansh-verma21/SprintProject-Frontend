package com.sprint.frontend.Controller;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Controller
public class StaffDetailController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    @GetMapping("/member/4/staff")
    public String staffDetail(@RequestParam("id") long id, Model model) {
        try {
            String staffUrl = BASE_URL + "/staff/" + id + "?projection=staffProjection";
            String staffJson = restTemplate.getForObject(staffUrl, String.class);
            JsonNode staff = objectMapper.readTree(staffJson);

            long storeId = staff.path("store").path("storeId").asLong(0);
            String address = staff.path("address").path("address").asText("");
            String city = staff.path("address").path("city").path("city").asText("");
            String storeLabel = "Store " + storeId;

            String addressUrl = BASE_URL + "/staff/" + id + "/address";
            String addressJson = restTemplate.getForObject(addressUrl, String.class);
            JsonNode addressDetails = objectMapper.readTree(addressJson);
            String phone = addressDetails.path("phone").asText("N/A");
            if (phone.isBlank()) {
                phone = "N/A";
            }

            model.addAttribute("staffId", id);
            model.addAttribute("fullName",
                    staff.path("firstName").asText("") + " " + staff.path("lastName").asText(""));
            model.addAttribute("email", staff.path("email").asText(""));
            model.addAttribute("username", staff.path("username").asText(""));
            model.addAttribute("active", staff.path("active").asBoolean(false));
            model.addAttribute("storeLabel", storeLabel);
            model.addAttribute("address", address);
            model.addAttribute("city", city);
            model.addAttribute("phone", phone);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load staff details.");
        }
        return "Staffdetail";
    }

    @PostMapping("/member/4/staff/delete")
    public String deleteStaff(@RequestParam("id") long id) {
        try {
            restTemplate.delete(BASE_URL + "/staff/" + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/member/4";
    }

    @PostMapping("/member/4/staff/toggle-active")
    public String toggleActive(@RequestParam("id") long id, @RequestParam("active") boolean active) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode bodyNode = mapper.createObjectNode().put("active", !active);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyNode.toString(), headers);
            restTemplate.exchange(BASE_URL + "/staff/" + id, HttpMethod.PATCH, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/member/4/staff?id=" + id;
    }
}
