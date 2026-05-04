package com.sprint.frontend.Controller;

import com.sprint.frontend.DTO.MemberDTO;
import com.sprint.frontend.DTO.StoreDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class StoreController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";
    private static final int STORE_PAGE_SIZE = 2;
    private static final int CITY_PAGE_SIZE = 100;
    private static final String DEFAULT_CITY = "Lethbridge";

    @GetMapping("/member/5")
    public String storeTable(
            @RequestParam(value = "city", defaultValue = "") String city,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        MemberDTO member = getMember();
        model.addAttribute("member", member);
        if (city == null || city.trim().isEmpty()) {
            city = DEFAULT_CITY;
            page = 0;
        }

        model.addAttribute("cityQuery", city);
        model.addAttribute("page", page);
        model.addAttribute("cities", fetchCities(0));
        model.addAttribute("selectedCity", city);

        StorePage sp = fetchStoresByCity(city.trim(), page);
        model.addAttribute("stores", sp.stores);
        model.addAttribute("hasMore", sp.hasMore);
        model.addAttribute("hasPrev", page > 0);

        return "Member5";
    }

    @GetMapping("/member/5/store")
    public String storeDetail(
            @RequestParam("storeId") long storeId,
            Model model) {

        StoreDTO store = fetchStoreById(storeId);
        if (store == null) {
            model.addAttribute("error", "Store not found.");
            return "Storedetail";
        }

        model.addAttribute("store", store);
        model.addAttribute("movieCount", fetchStoreMovieCount(storeId));
        return "Storedetail";
    }

    private static class StorePage {
        List<StoreDTO> stores;
        boolean hasMore;

        StorePage(List<StoreDTO> stores, boolean hasMore) {
            this.stores = stores;
            this.hasMore = hasMore;
        }
    }

    private StorePage fetchStoresByCity(String city, int page) {
        List<StoreDTO> stores = new ArrayList<>();
        boolean hasMore = false;
        try {
            String url = BASE_URL
                    + "/stores/search/findByAddress_City_CityIgnoreCase?city="
                    + java.net.URLEncoder.encode(city, "UTF-8")
                    + "&page=" + page
                    + "&size=" + STORE_PAGE_SIZE
                    + "&projection=storeProjection";

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode node : root.path("content")) {
                StoreDTO dto = parseStore(node);
                if (dto != null) {
                    stores.add(dto);
                }
            }

            for (JsonNode link : root.path("links")) {
                if ("next".equals(link.path("rel").asText())) {
                    hasMore = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new StorePage(stores, hasMore);
    }

    private StoreDTO fetchStoreById(long storeId) {
        try {
            String url = BASE_URL + "/stores/" + storeId + "?projection=storeProjection";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            return parseStore(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> fetchCities(int page) {
        List<String> cityNames = new ArrayList<>();
        try {
            String url = BASE_URL + "/cities?page=" + page + "&size=" + CITY_PAGE_SIZE;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode node : root.path("content")) {
                cityNames.add(node.path("city").asText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cityNames;
    }

    private long fetchStoreMovieCount(long storeId) {
        try {
            String url = BASE_URL
                    + "/inventories/search/findByStore_StoreId?storeId=" + storeId
                    + "&page=0&size=1";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            return root.path("page").path("totalElements").asLong(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private StoreDTO parseStore(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;

        long storeId = node.path("storeId").asLong();
        String lastUpdate = node.path("lastUpdate").asText("");

        JsonNode addr = node.path("address");
        String address = addr.path("address").asText("");
        String district = addr.path("district").asText("");
        String city = addr.path("city").path("city").asText("");

        JsonNode manager = node.path("managerStaff");
        long managerStaffId = manager.path("staffId").asLong();
        String managerName = (manager.path("firstName").asText("") + " "
            + manager.path("lastName").asText("")).trim();

        return new StoreDTO(storeId, managerStaffId, managerName,
            address, district, city, lastUpdate);
    }

    private MemberDTO getMember() {
        return new MemberDTO(
                5,
                "Mohd Amaan",
                "Store & Inventory",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR4fGl36r2h3AqOEeZVvNE8rvAzn041njqYww&s"
        );
    }
}
