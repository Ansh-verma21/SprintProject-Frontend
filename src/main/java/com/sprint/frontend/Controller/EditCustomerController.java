package com.sprint.frontend.Controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Controller
public class EditCustomerController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    // ── SHOW EDIT FORM ────────────────────────────────────────
    // Called with ?firstName=X&lastName=Y from the table Edit link
    @GetMapping("/member/3/edit-customer")
    public String showEditForm(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName")  String lastName,
            Model model) {

        model.addAttribute("success",      false);
        model.addAttribute("error",        false);
        model.addAttribute("addressError", false);
        model.addAttribute("storeError",   false);

        // Fetch current customer details to pre-fill the form
        try {
            String url = BASE_URL
                    + "/customers/search/findByFirstNameAndLastName?firstName="
                    + firstName.toUpperCase()
                    + "&lastName=" + lastName.toUpperCase();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");

            if (content.isArray() && content.size() > 0) {
                JsonNode c = content.get(content.size() - 1); // most recent

                // Extract customer ID from self link
                String customerId = extractIdFromLinks(c.path("links"), "self");

                // Current address text
                String currentAddress = c.path("address").path("address").asText("");
                // Current city (for store hint)
                String currentCity = c.path("address").path("city").path("city").asText("");

                model.addAttribute("customerId",      customerId);
                model.addAttribute("firstName",       c.path("firstName").asText());
                model.addAttribute("lastName",        c.path("lastName").asText());
                model.addAttribute("email",           c.path("email").asText());
                model.addAttribute("currentAddress",  currentAddress);
                model.addAttribute("currentCity",     currentCity);
                model.addAttribute("v_addressText",   currentAddress);
                model.addAttribute("v_storeCity",     currentCity);

            } else {
                model.addAttribute("error",    true);
                model.addAttribute("errorMsg", "Customer not found.");
            }

        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Customer not found.");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Could not load customer details.");
        }

        return "editCustomer";
    }

    // ── SUBMIT EDIT ───────────────────────────────────────────
    @PostMapping("/member/3/edit-customer")
    public String submitEdit(
            @RequestParam("customerId")  String customerId,
            @RequestParam("firstName")   String firstName,
            @RequestParam("lastName")    String lastName,
            @RequestParam("email")       String email,
            @RequestParam("addressText") String addressText,
            @RequestParam("storeCity")   String storeCity,
            Model model) {

        // Re-populate read-only fields for re-render
        model.addAttribute("customerId",     customerId);
        model.addAttribute("firstName",      firstName);
        model.addAttribute("lastName",       lastName);
        model.addAttribute("email",          email);
        model.addAttribute("v_addressText",  addressText);
        model.addAttribute("v_storeCity",    storeCity);
        model.addAttribute("success",        false);
        model.addAttribute("error",          false);
        model.addAttribute("addressError",   false);
        model.addAttribute("storeError",     false);

        // ── 1. Resolve address path ───────────────────────────
        String addressPath = resolveAddress(addressText.trim());
        if (addressPath == null) {
            model.addAttribute("addressError",  true);
            model.addAttribute("addressErrMsg", "No address found for \"" + addressText + "\". Please check and try again.");
            return "editCustomer";
        }

        // ── 2. Resolve store path ─────────────────────────────
        String storePath = resolveStore(storeCity.trim());
        if (storePath == null) {
            model.addAttribute("storeError",  true);
            model.addAttribute("storeErrMsg", "No store found in city \"" + storeCity + "\". Please check and try again.");
            return "editCustomer";
        }

        // ── 3. PUT address ────────────────────────────────────
        try {
            putUriList(
                BASE_URL + "/customers/" + customerId + "/address",
                BASE_URL + addressPath
            );
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Failed to update address: " + e.getMessage());
            return "editCustomer";
        }

        // ── 4. PUT store ──────────────────────────────────────
        try {
            putUriList(
                BASE_URL + "/customers/" + customerId + "/store",
                BASE_URL + storePath
            );
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Failed to update store: " + e.getMessage());
            return "editCustomer";
        }

        model.addAttribute("success",  true);
        model.addAttribute("fullName", toTitleCase(firstName) + " " + toTitleCase(lastName));
        return "editCustomer";
    }

    // ── PUT with text/uri-list ────────────────────────────────
    private void putUriList(String endpoint, String uriBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/uri-list"));
        HttpEntity<String> request = new HttpEntity<>(uriBody, headers);
        restTemplate.exchange(endpoint, HttpMethod.PUT, request, String.class);
    }

    // ── RESOLVE ADDRESS → "/addresses/{id}" ──────────────────
    private String resolveAddress(String addressText) {
        try {
            String url = BASE_URL + "/addresses/search/findByAddressIgnoreCase?address="
                    + java.net.URLEncoder.encode(addressText, "UTF-8");
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode link : root.path("links")) {
                if ("self".equals(link.path("rel").asText())) {
                    String href = link.path("href").asText();
                    String id = href.substring(href.lastIndexOf('/') + 1);
                    return "/addresses/" + id;
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── RESOLVE STORE → "/stores/{id}" ───────────────────────
    private String resolveStore(String city) {
        try {
            String url = BASE_URL
                    + "/stores/search/findByAddress_City_CityIgnoreCase?city="
                    + java.net.URLEncoder.encode(city, "UTF-8")
                    + "&page=0&size=1";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");
            if (content.isArray() && content.size() > 0) {
                for (JsonNode link : content.get(0).path("links")) {
                    if ("self".equals(link.path("rel").asText())) {
                        String href = link.path("href").asText();
                        String id = href.substring(href.lastIndexOf('/') + 1);
                        return "/stores/" + id;
                    }
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── EXTRACT ID FROM SELF LINK ─────────────────────────────
    private String extractIdFromLinks(JsonNode links, String rel) {
        for (JsonNode link : links) {
            if (rel.equals(link.path("rel").asText())) {
                String href = link.path("href").asText();
                if (href.contains("{")) href = href.substring(0, href.indexOf('{'));
                return href.substring(href.lastIndexOf('/') + 1);
            }
        }
        return null;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String lower = input.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}