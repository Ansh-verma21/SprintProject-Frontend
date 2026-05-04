package com.sprint.frontend.Controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AddStaffController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";
    private static final int STORE_PAGE_SIZE = 50;

    @GetMapping("/member/4/add-staff")
    public String showAddForm(Model model) {
        resetForm(model);
        model.addAttribute("stores", fetchStores());
        return "Addstaff";
    }

    @PostMapping("/member/4/add-staff")
    public String createStaff(
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
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("email", email);
        model.addAttribute("username", username);
        model.addAttribute("active", active);
        model.addAttribute("addressText", addressText);
        model.addAttribute("selectedStoreId", storeId);

        String addressUrl = resolveAddressUrl(addressText.trim());
        if (addressUrl == null) {
            model.addAttribute("addressError", true);
            model.addAttribute("addressErrMsg", "No address found for \"" + addressText + "\".");
            return "Addstaff";
        }
        if (storeId <= 0) {
            model.addAttribute("storeError", true);
            model.addAttribute("storeErrMsg", "Please select a valid store.");
            return "Addstaff";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.createObjectNode()
                    .put("firstName", firstName.trim())
                    .put("lastName", lastName.trim())
                    .put("email", email.trim())
                    .put("username", username.trim())
                    .put("password", password.trim())
                    .put("active", active)
                    .put("address", addressUrl)
                    .put("store", BASE_URL + "/stores/" + storeId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/staff", request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                String location = response.getHeaders().getLocation() != null ? response.getHeaders().getLocation().toString() : "";
                model.addAttribute("success", true);
                model.addAttribute("staffId", extractIdFromLocation(location));
                model.addAttribute("fullName", toTitleCase(firstName) + " " + toTitleCase(lastName));
                return "Addstaff";
            }
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Unable to create staff: " + e.getMessage());
            return "Addstaff";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
            return "Addstaff";
        }

        model.addAttribute("error", true);
        model.addAttribute("errorMsg", "Unexpected server response.");
        return "Addstaff";
    }

    private String resolveAddressUrl(String addressText) {
        try {
            String url = BASE_URL + "/addresses/search/findByAddressIgnoreCase?address="
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
            String url = BASE_URL + "/stores?page=0&size=" + STORE_PAGE_SIZE;
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
        model.addAttribute("firstName", "");
        model.addAttribute("lastName", "");
        model.addAttribute("email", "");
        model.addAttribute("username", "");
        model.addAttribute("password", "");
        model.addAttribute("active", true);
        model.addAttribute("addressText", "");
        model.addAttribute("selectedStoreId", 1L);
    }

    private String extractIdFromLocation(String location) {
        if (location == null || location.isBlank()) return "";
        if (location.contains("{")) location = location.substring(0, location.indexOf('{'));
        if (location.endsWith("/")) location = location.substring(0, location.length() - 1);
        int pos = location.lastIndexOf('/');
        return pos >= 0 ? location.substring(pos + 1) : location;
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
