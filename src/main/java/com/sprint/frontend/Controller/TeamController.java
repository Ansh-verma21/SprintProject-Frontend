package com.sprint.frontend.Controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sprint.frontend.DTO.CustomerDTO;
import com.sprint.frontend.DTO.MemberDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Controller
public class TeamController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final tools.jackson.databind.ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.baseUrl}")
    private String baseUrl;
    private static final int CITY_PAGE_SIZE = 600;

    // ── HOME ──────────────────────────────────────────────────
    @GetMapping("/")
    public String teamPage(Model model) {
        model.addAttribute("members", buildMembers());
        return "index";
    }

    // ── MEMBER PAGE (GET — default city page 0) ───────────────
    @GetMapping("/member/{id}")
    public String memberDetail(@PathVariable int id, Model model) {

        if (id != 3) return "redirect:/";
        MemberDTO member = getMember(id);
        if (member == null) return "redirect:/";

        CityPage cp = fetchCities(0);
        String defaultCity = cp.cities.isEmpty() ? "" : cp.cities.get(0);

        model.addAttribute("member",       member);
        model.addAttribute("cities",       cp.cities);
        model.addAttribute("cityPage",     0);
        model.addAttribute("hasMoreCities", cp.hasMore);
        model.addAttribute("selectedCity", defaultCity);
        model.addAttribute("searchMode",   false);
        model.addAttribute("searchQuery",  "");
        model.addAttribute("customers",
                defaultCity.isEmpty() ? Collections.emptyList() : fetchCustomersByCity(defaultCity));

        return "Member3";
    }

    // ── CITY FILTER / PAGE NAVIGATION (POST) ─────────────────
    @PostMapping("/member/{id}")
    public String memberDetailFiltered(
            @PathVariable int id,
            @RequestParam(value = "city",     defaultValue = "") String city,
            @RequestParam(value = "cityPage", defaultValue = "0") int cityPage,
            @RequestParam(value = "pageNav",  defaultValue = "false") boolean pageNav,
            Model model) {

        if (id != 3) return "redirect:/";
        MemberDTO member = getMember(id);
        if (member == null) return "redirect:/";

        CityPage cp = fetchCities(cityPage);

        model.addAttribute("member",        member);
        model.addAttribute("cities",        cp.cities);
        model.addAttribute("cityPage",      cityPage);
        model.addAttribute("hasMoreCities", cp.hasMore);
        model.addAttribute("searchMode",    false);
        model.addAttribute("searchQuery",   "");

        if (pageNav) {
            // Auto-select first city of the newly loaded page
            String autoCity = cp.cities.isEmpty() ? "" : cp.cities.get(0);
            model.addAttribute("selectedCity", autoCity);
            model.addAttribute("customers",
                    autoCity.isEmpty() ? Collections.emptyList() : fetchCustomersByCity(autoCity));
        } else {
            // User picked a city from the dropdown
            model.addAttribute("selectedCity", city);
            model.addAttribute("customers",
                    city.isEmpty() ? Collections.emptyList() : fetchCustomersByCity(city));
        }

        return "Member3";
    }

    // ── SEARCH (independent of city) ─────────────────────────
    @GetMapping("/member/{id}/search")
    public String memberSearch(
            @PathVariable int id,
            @RequestParam(value = "query", defaultValue = "") String query,
            Model model) {

        if (id != 3) return "redirect:/";
        MemberDTO member = getMember(id);
        if (member == null) return "redirect:/";

        CityPage cp = fetchCities(0);

        model.addAttribute("member",        member);
        model.addAttribute("cities",        cp.cities);
        model.addAttribute("cityPage",      0);
        model.addAttribute("hasMoreCities", cp.hasMore);
        model.addAttribute("selectedCity",  "");
        model.addAttribute("searchMode",    true);
        model.addAttribute("searchQuery",   query);

        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("customers", Collections.emptyList());
            return "Member3";
        }

        String[] parts = query.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName  = parts.length > 1 ? parts[1] : "";

        model.addAttribute("customers", searchCustomers(firstName, lastName));
        return "Member3";
    }

    // ── HELPERS ───────────────────────────────────────────────

    private static class CityPage {
        List<String> cities;
        boolean hasMore;
        CityPage(List<String> cities, boolean hasMore) {
            this.cities  = cities;
            this.hasMore = hasMore;
        }
    }

    private CityPage fetchCities(int page) {
        List<String> cityNames = new ArrayList<>();
        boolean hasMore = false;
        try {
            String url = baseUrl + "/cities?page=" + page + "&size=" + CITY_PAGE_SIZE;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode node : root.path("content")) {
                cityNames.add(node.path("city").asText());
            }

            // Check if there's a "next" link — means more pages exist
            for (JsonNode link : root.path("links")) {
                if ("next".equals(link.path("rel").asText())) {
                    hasMore = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CityPage(cityNames, hasMore);
    }

    private List<CustomerDTO> fetchCustomersByCity(String city) {
        List<CustomerDTO> customers = new ArrayList<>();
        try {
            String url = baseUrl
                    + "/customers/search/findByAddress_City_CityIgnoreCase?city="
                    + city + "&page=0&size=20";

            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");

            for (JsonNode node : content) {
                customers.add(new CustomerDTO(
                        node.path("firstName").asText() + " " + node.path("lastName").asText(),
                        node.path("email").asText(),
                        node.path("active").asBoolean()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    private List<CustomerDTO> searchCustomers(String firstName, String lastName) {
        List<CustomerDTO> customers = new ArrayList<>();
        try {
            String url = baseUrl
                    + "/customers/search/findByFirstNameAndLastName?firstName="
                    + firstName.toUpperCase()
                    + (lastName.isEmpty() ? "" : "&lastName=" + lastName.toUpperCase())
                    + "&project=CustomerProjection";

            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");

            for (JsonNode c : content) {
                customers.add(new CustomerDTO(
                        c.path("firstName").asText() + " " + c.path("lastName").asText(),
                        c.path("email").asText(),
                        c.path("active").asBoolean()
                ));
            }
        } catch (HttpClientErrorException.NotFound e) {
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    private MemberDTO getMember(int id) {
        return buildMembers().stream()
                .filter(m -> m.getMemberNumber() == id)
                .findFirst().orElse(null);
    }

    private List<MemberDTO> buildMembers() {
        return Arrays.asList(
                new MemberDTO(1, "Ansh Verma", "Film",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/ansh.jpeg"),
                new MemberDTO(2, "Manan Kr. Agarwal", "Actor",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/manan.jpeg"),
                new MemberDTO(3, "Aayush Saxena", "Customer",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/aayush.jpeg"),
                new MemberDTO(4, "Aniket Rathore", "Staff",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/aniket.jpeg"),
                new MemberDTO(5, "Mohd Amaan", "Store & Inventory",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/WhatsApp%20Image%202026-05-05%20at%209.32.08%20AM.jpeg"),
                new MemberDTO(6, "Aurindum Bose", "Language",
                        "https://raw.githubusercontent.com/aurindumgit/SprintProject-Frontend/dev/images/auri.jpeg"));
    }
}