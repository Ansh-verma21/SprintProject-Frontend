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
    private static final int PAGE_SIZE = 10;

    /**
     * GET /member/3/customer?firstName=SYLVIA&lastName=ORTIZ&page=0
     */
    @GetMapping("/member/3/customer")
    public String customerDetail(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName")  String lastName,
            @RequestParam(value = "page", defaultValue = "0") int page,
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

            String fullName = customer.path("firstName").asText()
                    + " " + customer.path("lastName").asText();

            String email = customer.path("email").asText("");

            JsonNode addrNode = customer.path("address");
            String phone   = addrNode.path("phone").asText("N/A");
            String address = addrNode.path("address").asText("");
            String city    = addrNode.path("city").path("city").asText("");
            String fullAddress = address.isEmpty() ? city
                    : (city.isEmpty() ? address : address + ", " + city);

            int customerId = extractCustomerIdFromLinks(customer.path("links"));

            if (customerId <= 0) {
                model.addAttribute("error", "Could not resolve customer ID.");
                return "Customerdetail";
            }

            // ── STEP 2: Get ALL rentals ───────────────────────────────────────
            List<Map<String, String>> allRentals = parseRentals(customerId);

            // Latest 3 sorted DESC
            List<Map<String, String>> latestRentals = allRentals.stream()
                    .sorted(Comparator.comparing(
                            (Map<String, String> r) -> r.getOrDefault("rentalDate", ""),
                            Comparator.reverseOrder()))
                    .limit(3)
                    .toList();

            // ── STEP 3: Paginate allRentals ───────────────────────────────────
            int totalRecords = allRentals.size();
            int totalPages   = (int) Math.ceil((double) totalRecords / PAGE_SIZE);

            // Clamp page to valid range
            if (page < 0) page = 0;
            if (page >= totalPages && totalPages > 0) page = totalPages - 1;

            int fromIndex = page * PAGE_SIZE;
            int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalRecords);

            List<Map<String, String>> pagedRentals = totalRecords > 0
                    ? allRentals.subList(fromIndex, toIndex)
                    : Collections.emptyList();

            // Page number list for Thymeleaf iteration (0-based internally, 1-based display)
            List<Integer> pageNumbers = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) pageNumbers.add(i);

            // ── Pass everything to the view ───────────────────────────────────
            model.addAttribute("fullName",      fullName);
            model.addAttribute("email",         email);
            model.addAttribute("phone",         phone);
            model.addAttribute("fullAddress",   fullAddress);
            model.addAttribute("firstName",     firstName);
            model.addAttribute("lastName",      lastName);
            model.addAttribute("latestRentals", latestRentals);
            model.addAttribute("allRentals",    allRentals);       // still needed if referenced
            model.addAttribute("pagedRentals",  pagedRentals);
            model.addAttribute("currentPage",   page);
            model.addAttribute("totalPages",    totalPages);
            model.addAttribute("totalRecords",  totalRecords);
            model.addAttribute("pageNumbers",   pageNumbers);
            model.addAttribute("fromRecord",    totalRecords > 0 ? fromIndex + 1 : 0);
            model.addAttribute("toRecord",      toIndex);
            model.addAttribute("hasPrev",       page > 0);
            model.addAttribute("hasNext",       page < totalPages - 1);
            model.addAttribute("prevPage",      page - 1);
            model.addAttribute("nextPage",      page + 1);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load customer details: " + e.getMessage());
        }

        return "Customerdetail";
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private int extractCustomerIdFromLinks(JsonNode linksNode) {
        for (JsonNode link : linksNode) {
            if ("self".equals(link.path("rel").asText())) {
                String href = link.path("href").asText("");
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

    private String safeDate(String dt) {
        if (dt == null || dt.isBlank()) return "";
        return dt.contains("T") ? dt.substring(0, dt.indexOf('T')) : dt;
    }
}