package com.sprint.frontend.Controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Controller
public class CustomerDetailController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    /**
     * GET /member/3/customer?firstName=SYLVIA&lastName=ORTIZ
     *
     * Uses ONLY two endpoints (no multi-step traversal needed):
     *   1. /customers/search/findByFirstNameAndLastName  →  name, email, phone, address, city
     *   2. /customers/{id}/rentals                       →  rentalDate, returnDate, film title
     */
    @GetMapping("/member/3/customer")
    public String customerDetail(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName")  String lastName,
            Model model) {

        try {
            // ── STEP 1: Search customer by name ──────────────────────────────
            String searchUrl = BASE_URL
                    + "/customers/search/findByFirstNameAndLastName"
                    + "?firstName=" + firstName.toUpperCase()
                    + "&lastName="  + lastName.toUpperCase();

            String searchJson = restTemplate.getForObject(searchUrl, String.class);
            JsonNode root    = objectMapper.readTree(searchJson);
            JsonNode content = root.path("content");

            if (content.isEmpty()) {
                model.addAttribute("error", "Customer not found.");
                return "Customerdetail";
            }

            JsonNode customer = content.get(0);

            // Full name
            String fullName = customer.path("firstName").asText()
                    + " " + customer.path("lastName").asText();

            // Email
            String email = customer.path("email").asText("");

            // Phone — lives inside address object
            JsonNode addrNode = customer.path("address");
            String phone   = addrNode.path("phone").asText("N/A");
            String address = addrNode.path("address").asText("");
            String city    = addrNode.path("city").path("city").asText("");
            // address + city (no country, as requested)
            String fullAddress = address.isEmpty() ? city
                    : (city.isEmpty() ? address : address + ", " + city);

            // Extract customerId from self link: "http://localhost:8000/customers/120"
            int customerId = extractCustomerIdFromLinks(customer.path("links"));

            if (customerId <= 0) {
                model.addAttribute("error", "Could not resolve customer ID.");
                return "Customerdetail";
            }

            // ── STEP 2: Get rentals ───────────────────────────────────────────
            // Response: content[].rentalDate, returnDate
            //           content[].content[].value.film.title  (rel = "inventory")
            List<Map<String, String>> allRentals = parseRentals(customerId);

            // Latest 3 sorted DESC by rentalDate
            List<Map<String, String>> latestRentals = allRentals.stream()
                    .sorted(Comparator.comparing(
                            (Map<String, String> r) -> r.getOrDefault("rentalDate", ""),
                            Comparator.reverseOrder()))
                    .limit(3)
                    .toList();

            model.addAttribute("fullName",      fullName);
            model.addAttribute("email",         email);
            model.addAttribute("phone",         phone);
            model.addAttribute("fullAddress",   fullAddress);
            model.addAttribute("latestRentals", latestRentals);
            model.addAttribute("allRentals",    allRentals);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load customer details: " + e.getMessage());
        }

        return "Customerdetail";
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────

    /**
     * Extract the numeric ID from the "self" link inside a "links" array.
     * e.g. href = "http://localhost:8000/customers/120" → 120
     */
    private int extractCustomerIdFromLinks(JsonNode linksNode) {
        for (JsonNode link : linksNode) {
            if ("self".equals(link.path("rel").asText())) {
                String href = link.path("href").asText("");
                // Remove projection template like {?projection}
                href = href.replaceAll("\\{.*?}", "");
                String[] parts = href.split("/");
                if (parts.length > 0) {
                    try {
                        return Integer.parseInt(parts[parts.length - 1]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    /**
     * Parse GET /customers/{id}/rentals
     *
     * The response "content" array has items like:
     * {
     *   "rentalDate": "2005-05-25T09:47:31",
     *   "returnDate": "2005-05-31T10:20:31",
     *   "content": [
     *     { "rel": "inventory", "value": { "film": { "title": "TEEN APOLLO" } } },
     *     { "rel": "staff", ... },
     *     { "rel": "customer", ... }
     *   ]
     * }
     */
    private List<Map<String, String>> parseRentals(int customerId) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            String url      = BASE_URL + "/customers/" + customerId + "/rentals";
            String json     = restTemplate.getForObject(url, String.class);
            JsonNode root   = objectMapper.readTree(json);
            JsonNode rentals = root.path("content");

            for (JsonNode rental : rentals) {
                String rentalDate = safeDate(rental.path("rentalDate").asText(""));
                String returnDate = safeDate(rental.path("returnDate").asText(""));

                // Film title lives in the embedded "content" array under rel="inventory"
                String filmTitle = "N/A";
                for (JsonNode embedded : rental.path("content")) {
                    if ("inventory".equals(embedded.path("rel").asText())) {
                        filmTitle = embedded.path("value")
                                .path("film").path("title").asText("N/A");
                        break;
                    }
                }

                Map<String, String> row = new LinkedHashMap<>();
                row.put("filmName",   filmTitle);
                row.put("rentalDate", rentalDate);
                row.put("returnDate", returnDate.isEmpty() ? "Not Returned" : returnDate);
                result.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Trim datetime to date only: "2005-05-25T09:47:31" → "2005-05-25" */
    private String safeDate(String dt) {
        if (dt == null || dt.isBlank()) return "";
        return dt.contains("T") ? dt.substring(0, dt.indexOf('T')) : dt;
    }
}