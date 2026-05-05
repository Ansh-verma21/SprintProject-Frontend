package com.sprint.frontend.Controller;

import com.sprint.frontend.DTO.MemberDTO;
import com.sprint.frontend.DTO.StaffDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class StaffController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.baseUrl}")
    private String baseUrl;
    private static final int STORE_PAGE_SIZE = 50;

    @GetMapping("/member/4")
    public String staffPage(Model model) {
        MemberDTO member = getMember(4);
        if (member == null) return "redirect:/";

        List<StoreInfo> stores = fetchStores();
        long selectedStoreId = stores.isEmpty() ? 0 : stores.get(0).id;
        model.addAttribute("member", member);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStoreId", selectedStoreId);
        model.addAttribute("searchMode", false);
        model.addAttribute("searchQuery", "");
        model.addAttribute("staff", selectedStoreId == 0 ? Collections.emptyList() : fetchStaffByStore(selectedStoreId));
        return "Member4";
    }

    @PostMapping("/member/4")
    public String staffPageFiltered(
            @RequestParam(value = "storeId", defaultValue = "0") long storeId,
            Model model) {
        MemberDTO member = getMember(4);
        if (member == null) return "redirect:/";

        List<StoreInfo> stores = fetchStores();
        if (storeId <= 0 && !stores.isEmpty()) {
            storeId = stores.get(0).id;
        }

        model.addAttribute("member", member);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStoreId", storeId);
        model.addAttribute("searchMode", false);
        model.addAttribute("searchQuery", "");
        model.addAttribute("staff", storeId == 0 ? Collections.emptyList() : fetchStaffByStore(storeId));
        return "Member4";
    }

    @GetMapping("/member/{id}/search/staff")
    public String staffSearch(
            @PathVariable int id,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "storeId", defaultValue = "0") long storeId,
            Model model) {
        if (id != 4) return "redirect:/";
        MemberDTO member = getMember(4);
        if (member == null) return "redirect:/";

        List<StoreInfo> stores = fetchStores();
        if (storeId <= 0 && !stores.isEmpty()) {
            storeId = stores.get(0).id;
        }

        model.addAttribute("member", member);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStoreId", storeId);
        model.addAttribute("searchMode", true);
        model.addAttribute("searchQuery", query);

        if (query == null || query.trim().isEmpty() || storeId == 0) {
            model.addAttribute("staff", Collections.emptyList());
            return "Member4";
        }

        model.addAttribute("staff", searchStaff(query.trim(), storeId));
        return "Member4";
    }

    private List<StaffDTO> fetchStaffByStore(long storeId) {
        List<StaffDTO> staff = new ArrayList<>();
        try {
            String url = baseUrl + "/staff/search/findByStore_StoreId?storeId=" + storeId + "&page=0&size=60";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");
            for (JsonNode node : content) {
                staff.add(new StaffDTO(
                        node.path("staffId").asLong(0),
                        node.path("firstName").asText("") + " " + node.path("lastName").asText(""),
                        node.path("email").asText(""),
                        node.path("username").asText(""),
                        node.path("active").asBoolean(false),
                        node.path("store").path("storeId").asLong(0)
                ));
            }
        } catch (HttpClientErrorException.NotFound e) {
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return staff;
    }

    private List<StaffDTO> searchStaff(String query, long storeId) {
        List<StaffDTO> staff = new ArrayList<>();
        try {
            String url = baseUrl + "/staff/search/findByFirstNameContainingIgnoreCaseAndStore_StoreId?name="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&storeId=" + storeId;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(json).path("content");
            for (JsonNode node : content) {
                staff.add(new StaffDTO(
                        node.path("staffId").asLong(0),
                        node.path("firstName").asText("") + " " + node.path("lastName").asText(""),
                        node.path("email").asText(""),
                        node.path("username").asText(""),
                        node.path("active").asBoolean(false),
                        node.path("store").path("storeId").asLong(0)
                ));
            }
        } catch (HttpClientErrorException.NotFound e) {
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return staff;
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

    private MemberDTO getMember(int id) {
        if (id != 4) return null;
        return new MemberDTO(4, "Aniket Rathore", "Staff",
                "https://i.quotev.com/wgx7jg4haaaa.jpg");
    }

    private static class StoreInfo {
        final long id;
        final String label;

        StoreInfo(long id, String label) {
            this.id = id;
            this.label = label;
        }

        public long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }
}
