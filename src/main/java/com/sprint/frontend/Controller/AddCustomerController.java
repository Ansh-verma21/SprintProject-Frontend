package com.sprint.frontend.Controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Controller
public class AddCustomerController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    // ── SHOW FORM ─────────────────────────────────────────────
    @GetMapping("/member/3/add-customer")
    public String showForm(Model model) {
        resetModel(model);
        return "addCustomer";
    }

    // ── SUBMIT ────────────────────────────────────────────────
    @PostMapping("/member/3/add-customer")
    public String submit(
            @RequestParam("firstName")   String firstName,
            @RequestParam("lastName")    String lastName,
            @RequestParam("email")       String email,
            @RequestParam("addressText") String addressText,
            @RequestParam("storeCity")   String storeCity,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            Model model) {

        // keep values for re-render
        model.addAttribute("v_firstName",   firstName);
        model.addAttribute("v_lastName",    lastName);
        model.addAttribute("v_email",       email);
        model.addAttribute("v_addressText", addressText);
        model.addAttribute("v_storeCity",   storeCity);
        model.addAttribute("success",       false);
        model.addAttribute("addressError",  false);
        model.addAttribute("storeError",    false);
        model.addAttribute("error",         false);

        // ── 1. Resolve address ────────────────────────────────
        String addressPath = resolveAddress(addressText.trim());
        if (addressPath == null) {
            model.addAttribute("addressError",  true);
            model.addAttribute("addressErrMsg", "No address found for \"" + addressText + "\". Please check and try again.");
            return "addCustomer";
        }

        // ── 2. Resolve store ──────────────────────────────────
        String storePath = resolveStore(storeCity.trim());
        if (storePath == null) {
            model.addAttribute("storeError",  true);
            model.addAttribute("storeErrMsg", "No store found in city \"" + storeCity + "\". Please check and try again.");
            return "addCustomer";
        }

        // ── 3. POST customer ──────────────────────────────────
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("firstName",  firstName.trim().toUpperCase());
            body.put("lastName",   lastName.trim().toUpperCase());
            body.put("email",      email.trim());
            body.put("active",     active);
            body.put("createDate", LocalDate.now().toString());
            body.put("store",      storePath);
            body.put("address",    addressPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/customers", HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                String customerId = resolveCustomerId(
                        firstName.trim().toUpperCase(),
                        lastName.trim().toUpperCase());

                model.addAttribute("success",    true);
                model.addAttribute("fullName",   toTitleCase(firstName) + " " + toTitleCase(lastName));
                model.addAttribute("customerId", customerId);
            } else {
                model.addAttribute("error",    true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Failed to create customer: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "addCustomer";
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

    // ── RESOLVE CUSTOMER ID AFTER CREATION ───────────────────
    private String resolveCustomerId(String firstName, String lastName) {
        try {
            String url = BASE_URL
                    + "/customers/search/findByFirstNameAndLastName?firstName="
                    + firstName + "&lastName=" + lastName;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");
            if (content.isArray() && content.size() > 0) {
                JsonNode customer = content.get(content.size() - 1);
                for (JsonNode link : customer.path("links")) {
                    if ("self".equals(link.path("rel").asText())) {
                        String href = link.path("href").asText();
                        if (href.contains("{")) href = href.substring(0, href.indexOf('{'));
                        return href.substring(href.lastIndexOf('/') + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    private void resetModel(Model model) {
        model.addAttribute("success",      false);
        model.addAttribute("error",        false);
        model.addAttribute("addressError", false);
        model.addAttribute("storeError",   false);
        model.addAttribute("v_firstName",  "");
        model.addAttribute("v_lastName",   "");
        model.addAttribute("v_email",      "");
        model.addAttribute("v_addressText","");
        model.addAttribute("v_storeCity",  "");
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String lower = input.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}