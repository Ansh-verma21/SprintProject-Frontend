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

@Controller
public class EditCustomerController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    // ── SHOW EDIT FORM ────────────────────────────────────────
    @GetMapping("/member/3/edit-customer")
    public String showEditForm(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName")  String lastName,
            Model model) {

        resetFlags(model);

        try {
            String url = BASE_URL
                    + "/customers/search/findByFirstNameAndLastName?firstName="
                    + firstName.toUpperCase()
                    + "&lastName=" + lastName.toUpperCase();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");

            if (content.isArray() && content.size() > 0) {
                JsonNode c = content.get(content.size() - 1);

                String customerId    = extractIdFromLinks(c.path("links"), "self");
                String currentAddress = c.path("address").path("address").asText("");
                String currentCity   = c.path("address").path("city").path("city").asText("");

                model.addAttribute("customerId",    customerId);
                model.addAttribute("v_firstName",   c.path("firstName").asText());
                model.addAttribute("v_lastName",    c.path("lastName").asText());
                model.addAttribute("v_email",       c.path("email").asText());
                model.addAttribute("v_active",      c.path("active").asBoolean(true));
                model.addAttribute("v_addressText", currentAddress);
                model.addAttribute("v_storeCity",   currentCity);
                model.addAttribute("v_createDate",  c.path("createDate").asText(""));

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
            @RequestParam("createDate")  String createDate,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            Model model) {

        // keep form values for re-render on error
        model.addAttribute("customerId",    customerId);
        model.addAttribute("v_firstName",   firstName);
        model.addAttribute("v_lastName",    lastName);
        model.addAttribute("v_email",       email);
        model.addAttribute("v_addressText", addressText);
        model.addAttribute("v_storeCity",   storeCity);
        model.addAttribute("v_active",      active);
        model.addAttribute("v_createDate",  createDate);
        resetFlags(model);

        // ── 1. Resolve address ────────────────────────────────
        String addressUrl = resolveAddressUrl(addressText.trim());
        if (addressUrl == null) {
            model.addAttribute("addressError",  true);
            model.addAttribute("addressErrMsg",
                    "No address found for \"" + addressText + "\". Please check and try again.");
            return "Editcustomer";
        }

        // ── 2. Resolve store ──────────────────────────────────
        String storeUrl = resolveStoreUrl(storeCity.trim());
        if (storeUrl == null) {
            model.addAttribute("storeError",  true);
            model.addAttribute("storeErrMsg",
                    "No store found in city \"" + storeCity + "\". Please check and try again.");
            return "Editcustomer";
        }

        // ── 3. PUT /customers/{id} — single call updates everything ──
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("firstName",  firstName.trim().toUpperCase());
            body.put("lastName",   lastName.trim().toUpperCase());
            body.put("email",      email.trim());
            body.put("active",     active);
            body.put("createDate", createDate);
            body.put("address",    addressUrl);
            body.put("store",      storeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/customers/" + customerId,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success",  true);
                model.addAttribute("fullName",
                        toTitleCase(firstName) + " " + toTitleCase(lastName));
            } else {
                model.addAttribute("error",    true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Failed to update customer: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error",    true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "Editcustomer";
    }

    // ── RESOLVE ADDRESS → full URL e.g. http://localhost:8000/addresses/1 ──
    private String resolveAddressUrl(String addressText) {
        try {
            String url = BASE_URL + "/addresses/search/findByAddressIgnoreCase?address="
                    + java.net.URLEncoder.encode(addressText, "UTF-8");
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

    // ── RESOLVE STORE → full URL e.g. http://localhost:8000/stores/1 ──
    private String resolveStoreUrl(String city) {
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
                        if (href.contains("{")) href = href.substring(0, href.indexOf('{'));
                        return href;
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

    // ── HELPERS ───────────────────────────────────────────────
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

    private void resetFlags(Model model) {
        model.addAttribute("success",      false);
        model.addAttribute("error",        false);
        model.addAttribute("addressError", false);
        model.addAttribute("storeError",   false);
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String lower = input.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}