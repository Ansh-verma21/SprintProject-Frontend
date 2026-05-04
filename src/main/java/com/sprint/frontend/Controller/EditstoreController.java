package com.sprint.frontend.Controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Controller
public class EditstoreController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8000";

    @GetMapping("/member/5/edit-store")
    public String showForm(
            @RequestParam("storeId") String storeId,
            Model model) {

        resetModel(model);
        model.addAttribute("v_storeId", storeId);

        try {
            String url = BASE_URL + "/stores/" + storeId + "?projection=storeProjection";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            String addressText = root.path("address").path("address").asText("");
            String managerId = root.path("managerStaff").path("staffId").asText("");
            String managerName = (root.path("managerStaff").path("firstName").asText("") + " "
                    + root.path("managerStaff").path("lastName").asText("")).trim();

            model.addAttribute("v_addressText", addressText);
            model.addAttribute("v_managerStaffId", managerId);
            model.addAttribute("managerName", managerName);

        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Store not found.");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Could not load store details.");
        }

        return "Editstore";
    }

    @PostMapping("/member/5/edit-store")
    public String submit(
            @RequestParam("storeId") String storeId,
            @RequestParam("addressText") String addressText,
            @RequestParam("managerStaffId") String managerStaffId,
            Model model) {

        model.addAttribute("v_storeId", storeId);
        model.addAttribute("v_addressText", addressText);
        model.addAttribute("v_managerStaffId", managerStaffId);
        model.addAttribute("managerName", "");
        model.addAttribute("success", false);
        model.addAttribute("addressError", false);
        model.addAttribute("managerError", false);
        model.addAttribute("error", false);

        String addressPath = resolveAddress(addressText.trim());
        if (addressPath == null) {
            model.addAttribute("addressError", true);
            model.addAttribute("addressErrMsg",
                    "No address found for \"" + addressText + "\". Please check and try again.");
            return "Editstore";
        }

        String managerPath = resolveManagerStaff(managerStaffId.trim());
        if (managerPath == null) {
            model.addAttribute("managerError", true);
            model.addAttribute("managerErrMsg",
                    "No staff found for ID \"" + managerStaffId + "\". Please check and try again.");
            return "Editstore";
        }

        String managerName = fetchManagerName(managerStaffId.trim());
        model.addAttribute("managerName", managerName);

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("address", addressPath);
            body.put("managerStaff", managerPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/stores/" + storeId,
                    HttpMethod.PATCH,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", true);
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            String respBody = e.getResponseBodyAsString();
            if (respBody != null && respBody.contains("idx_unique_manager")) {
                model.addAttribute("managerError", true);
                model.addAttribute("managerErrMsg",
                        "Staff ID already assigned as manager for another store.");
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Failed to update store: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "Editstore";
    }

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

    private String fetchManagerName(String staffId) {
        try {
            String url = BASE_URL + "/staff/" + staffId;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            String first = root.path("firstName").asText("");
            String last = root.path("lastName").asText("");
            return (first + " " + last).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void resetModel(Model model) {
        model.addAttribute("success", false);
        model.addAttribute("error", false);
        model.addAttribute("addressError", false);
        model.addAttribute("managerError", false);
        model.addAttribute("v_storeId", "");
        model.addAttribute("v_addressText", "");
        model.addAttribute("v_managerStaffId", "");
        model.addAttribute("managerName", "");
    }
}