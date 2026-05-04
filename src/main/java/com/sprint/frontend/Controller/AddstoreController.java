package com.sprint.frontend.Controller;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Controller
public class AddstoreController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    @GetMapping("/member/5/add-store")
    public String showForm(Model model) {
        resetModel(model);
        return "Addstore";
    }

    @PostMapping("/member/5/add-store")
    public String submit(
            @RequestParam("addressText") String addressText,
            @RequestParam("managerStaffId") String managerStaffId,
            Model model) {

        model.addAttribute("v_addressText", addressText);
        model.addAttribute("v_managerStaffId", managerStaffId);
        model.addAttribute("success", false);
        model.addAttribute("addressError", false);
        model.addAttribute("managerError", false);
        model.addAttribute("error", false);

        String addressPath = resolveAddress(addressText.trim());
        if (addressPath == null) {
            model.addAttribute("addressError", true);
            model.addAttribute("addressErrMsg",
                    "No address found for \"" + addressText + "\". Please check and try again.");
            return "Addstore";
        }

        String managerPath = resolveManagerStaff(managerStaffId.trim());
        if (managerPath == null) {
            model.addAttribute("managerError", true);
            model.addAttribute("managerErrMsg",
                    "No staff found for ID \"" + managerStaffId + "\". Please check and try again.");
            return "Addstore";
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("address", addressPath);
            body.put("managerStaff", managerPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/stores", HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                String storeId = resolveStoreIdFromLocation(response.getHeaders().getLocation());
                model.addAttribute("success", true);
                model.addAttribute("storeId", storeId);
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Failed to create store: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "Addstore";
    }

    private String resolveAddress(String addressText) {
        try {
            String url = BASE_URL + "/addresses/search/findByAddressIgnoreCase?address="
                    + java.net.URLEncoder.encode(addressText, "UTF-8");
            String json = restTemplate.getForObject(url, String.class);
            tools.jackson.databind.JsonNode root = objectMapper.readTree(json);
            for (tools.jackson.databind.JsonNode link : root.path("links")) {
                if ("self".equals(link.path("rel").asText())) {
                    String href = link.path("href").asText();
                    String id = href.substring(href.lastIndexOf('/') + 1);
                    return BASE_URL + "/addresses/" + id;
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String resolveManagerStaff(String staffId) {
        try {
            String url = BASE_URL + "/staff/" + staffId;
            restTemplate.getForObject(url, String.class);
            return BASE_URL + "/staff/" + staffId;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String resolveStoreIdFromLocation(URI location) {
        if (location == null) return "N/A";
        String path = location.getPath();
        if (path == null || path.isEmpty()) return "N/A";
        String[] parts = path.split("/");
        return parts.length == 0 ? "N/A" : parts[parts.length - 1];
    }

    private void resetModel(Model model) {
        model.addAttribute("success", false);
        model.addAttribute("error", false);
        model.addAttribute("addressError", false);
        model.addAttribute("managerError", false);
        model.addAttribute("v_addressText", "");
        model.addAttribute("v_managerStaffId", "");
    }
}
