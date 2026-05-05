package com.sprint.frontend.Controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
public class EditStaffController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.baseUrl}")
    private String baseUrl;
    private static final int STORE_PAGE_SIZE = 50;

    @GetMapping("/member/4/edit-staff")
    public String showEditForm(@RequestParam("id") long id, Model model) {
        resetForm(model);
        model.addAttribute("stores", fetchStores());
        try {
            String json = restTemplate.getForObject(baseUrl + "/staff/" + id, String.class);
            JsonNode staff = objectMapper.readTree(json);
            model.addAttribute("staffId", id);
            model.addAttribute("firstName", staff.path("firstName").asText(""));
            model.addAttribute("lastName", staff.path("lastName").asText(""));
            model.addAttribute("email", staff.path("email").asText(""));
            model.addAttribute("username", staff.path("username").asText(""));
            model.addAttribute("password", "");
            model.addAttribute("active", staff.path("active").asBoolean(false));
            model.addAttribute("addressText", staff.path("address").path("address").asText(""));
            model.addAttribute("selectedStoreId", staff.path("store").path("storeId").asLong(1));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Could not load staff details.");
        }
        return "Editstaff";
    }

    @PostMapping("/member/4/edit-staff")
    public String submitEdit(
            @RequestParam("staffId") long staffId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            @RequestParam("addressText") String addressText,
            @RequestParam("storeId") long storeId,
            Model model) {

        resetForm(model);
        model.addAttribute("stores", fetchStores());
        model.addAttribute("staffId", staffId);
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("email", email);
        model.addAttribute("username", username);
        model.addAttribute("password", "");
        model.addAttribute("active", active);
        model.addAttribute("addressText", addressText);
        model.addAttribute("selectedStoreId", storeId);

        String addressUrl = resolveAddressUrl(addressText.trim());
        if (addressUrl == null) {
            model.addAttribute("addressError", true);
            model.addAttribute("addressErrMsg", "No address found for \"" + addressText + "\".");
            return "Editstaff";
        }
        if (storeId <= 0) {
            model.addAttribute("storeError", true);
            model.addAttribute("storeErrMsg", "Please select a valid store.");
            return "Editstaff";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode()
                    .put("firstName", firstName.trim())
                    .put("lastName", lastName.trim())
                    .put("email", email.trim())
                    .put("username", username.trim())
                    .put("active", active)
                    .put("address", addressUrl)
                    .put("store", baseUrl + "/stores/" + storeId);
            if (password != null && !password.trim().isEmpty()) {
                body.put("password", password.trim());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/staff/" + staffId,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", true);
                model.addAttribute("fullName", toTitleCase(firstName) + " " + toTitleCase(lastName));
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Unable to update staff.");
            }
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Failed to update staff: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }
        return "Editstaff";
    }

    private String resolveAddressUrl(String addressText) {
        try {
            String url = baseUrl + "/addresses/search/findByAddressIgnoreCase?address="
                    + URLEncoder.encode(addressText, StandardCharsets.UTF_8);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode link : root.path("links")) {
                if ("self".equals(link.path("rel").asText())) {
                    return link.path("href").asText();
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<StoreInfo> fetchStores() {
        List<StoreInfo> stores = new ArrayList<>();
        try {
            String url = baseUrl + "/stores?page=0&size=" + STORE_PAGE_SIZE;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");
            for (JsonNode node : content) {
                long id = node.path("storeId").asLong(0);
                String label = "Store " + id;
                JsonNode addressNode = node.path("address");
                if (addressNode.isObject()) {
                    String city = addressNode.path("city").path("city").asText("");
                    if (!city.isEmpty()) {
                        label = "Store " + id + " — " + city;
                    }
                }
                stores.add(new StoreInfo(id, label));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (stores.isEmpty()) {
            stores.add(new StoreInfo(1, "Store 1"));
            stores.add(new StoreInfo(2, "Store 2"));
        }
        return stores;
    }

    private void resetForm(Model model) {
        model.addAttribute("success", false);
        model.addAttribute("error", false);
        model.addAttribute("addressError", false);
        model.addAttribute("storeError", false);
        model.addAttribute("staffId", 0L);
        model.addAttribute("firstName", "");
        model.addAttribute("lastName", "");
        model.addAttribute("email", "");
        model.addAttribute("username", "");
        model.addAttribute("password", "");
        model.addAttribute("active", true);
        model.addAttribute("addressText", "");
        model.addAttribute("selectedStoreId", 1L);
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String lower = input.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static class StoreInfo {
        final long id;
        final String label;

        StoreInfo(long id, String label) {
            this.id = id;
            this.label = label;
        }
    }
}
