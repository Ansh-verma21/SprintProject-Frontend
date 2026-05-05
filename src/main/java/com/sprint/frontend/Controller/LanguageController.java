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
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Controller
@RequestMapping("/languages")
public class LanguageController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.baseUrl}")
    private String baseUrl;

    // ── HOME: LIST ALL LANGUAGES ──────────────────────────────────
    @GetMapping("")
    public String languagesPage(Model model) {
        try {
            String url = baseUrl + "/languages";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode content = root.path("content");

            List<Map<String, Object>> languages = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode lang : content) {
                    Map<String, Object> langMap = new HashMap<>();
                    langMap.put("id", lang.path("languageId").asInt());
                    langMap.put("name", lang.path("name").asText(""));
                    langMap.put("lastUpdate", lang.path("lastUpdate").asText(""));
                    languages.add(langMap);
                }
            }

            model.addAttribute("languages", languages);
            model.addAttribute("error", null);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("languages", Collections.emptyList());
            model.addAttribute("error", "Failed to load languages.");
        }

        return "Languages";
    }

    // ── LANGUAGE DETAIL ────────────────────────────────────────────
    @GetMapping("/{id}")
    public String languageDetail(@PathVariable int id,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        try {
            String url = baseUrl + "/languages/" + id;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode lang = objectMapper.readTree(json);

            model.addAttribute("id", id);
            model.addAttribute("name", lang.path("name").asText(""));
            model.addAttribute("lastUpdate", lang.path("lastUpdate").asText(""));
            model.addAttribute("error", null);

            // pagination
            JsonNode pageData = fetchFilmsByLanguage(id, page);
            List<Map<String, Object>> films = new ArrayList<>();
            JsonNode content = pageData.path("content");
            if (content.isArray()) {
                for (JsonNode film : content) {
                    Map<String, Object> filmMap = new HashMap<>();
                    filmMap.put("title", film.path("title").asText(""));
                    String releaseYearStr = film.path("releaseYear").asText("");
                    String year = releaseYearStr.isEmpty() ? "" : releaseYearStr.substring(0, 4);
                    filmMap.put("releaseYear", year);
                    filmMap.put("rating", film.path("rating").asText("N/A"));
                    filmMap.put("rentalRate", film.path("rentalRate").asDouble(0.0));
                    filmMap.put("replacementCost", film.path("replacementCost").asDouble(0.0));
                    films.add(filmMap);
                }
            }

            int totalPages = pageData.path("page").path("totalPages").asInt(1);
            int totalElements = pageData.path("page").path("totalElements").asInt(0);

            model.addAttribute("films", films);
            model.addAttribute("filmCount", totalElements);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasPrev", page > 0);
            model.addAttribute("hasNext", page < totalPages - 1);

        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error", "Language not found.");
            model.addAttribute("films", Collections.emptyList());
            model.addAttribute("filmCount", 0);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load language details.");
            model.addAttribute("films", Collections.emptyList());
            model.addAttribute("filmCount", 0);
        }

        return "LanguageDetail";
    }

    // ── SHOW ADD FORM ──────────────────────────────────────────────
    @GetMapping("/add-language")
    public String showAddForm(Model model) {
        model.addAttribute("error", null);
        model.addAttribute("success", false);
        return "AddLanguage";
    }

    // ── SUBMIT ADD ─────────────────────────────────────────────────
    @PostMapping("/add-language")
    public String submitAdd(
            @RequestParam("name") String name,
            Model model) {

        model.addAttribute("v_name", name);
        model.addAttribute("error", false);
        model.addAttribute("success", false);

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Language name cannot be empty.");
            return "AddLanguage";
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", name.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/languages",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode createdLang = objectMapper.readTree(response.getBody());
                int languageId = createdLang.path("languageId").asInt();

                model.addAttribute("success", true);
                model.addAttribute("languageId", languageId);
                model.addAttribute("languageName", name.trim());
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Failed to create language: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "AddLanguage";
    }

    // ── SHOW EDIT FORM ─────────────────────────────────────────────
    @GetMapping("/edit-language/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
        resetFlags(model);

        try {
            String url = baseUrl + "/languages/" + id;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode lang = objectMapper.readTree(json);

            model.addAttribute("languageId", id);
            model.addAttribute("v_name", lang.path("name").asText(""));
            model.addAttribute("v_lastUpdate", lang.path("lastUpdate").asText(""));

        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Language not found.");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Could not load language details.");
        }

        return "EditLanguage";
    }

    // ── SUBMIT EDIT ────────────────────────────────────────────────
    @PostMapping("/edit-language/{id}")
    public String submitEdit(
            @PathVariable int id,
            @RequestParam("name") String name,
            @RequestParam("lastUpdate") String lastUpdate,
            Model model) {

        // keep form values for re-render on error
        model.addAttribute("languageId", id);
        model.addAttribute("v_name", name);
        resetFlags(model);

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("nameError", true);
            model.addAttribute("nameErrMsg", "Language name cannot be empty.");
            return "EditLanguage";
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", name.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/languages/" + id,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", true);
                model.addAttribute("languageName", name.trim());
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMsg", "Unexpected server response. Please try again.");
            }

        } catch (HttpClientErrorException e) {
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Failed to update language: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
            model.addAttribute("errorMsg", "Something went wrong. Please try again.");
        }

        return "EditLanguage";
    }

    // ── SEARCH LANGUAGES BY NAME (for film filtering) ──────────────
    @GetMapping("/search")
    public String searchLanguages(
            @RequestParam(value = "query", defaultValue = "") String query,
            Model model) {

        try {
            String url = baseUrl + "/films/search/byLanguage?language=" + query;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode content = root.path("content");

            List<Map<String, Object>> films = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode film : content) {
                    Map<String, Object> filmMap = new HashMap<>();
                    filmMap.put("id", film.path("filmId").asInt());
                    filmMap.put("title", film.path("title").asText(""));
                    filmMap.put("releaseYear", film.path("releaseYear").asInt());
                    filmMap.put("language", film.path("language").path("name").asText(query));
                    films.add(filmMap);
                }
            }

            model.addAttribute("query", query);
            model.addAttribute("films", films);
            model.addAttribute("hasResults", !films.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("query", query);
            model.addAttribute("films", Collections.emptyList());
            model.addAttribute("hasResults", false);
            model.addAttribute("error", "Failed to search films by language.");
        }

        return "LanguageSearch";
    }

    // ── HELPER: Fetch Films by Language ────────────────────────
    private JsonNode fetchFilmsByLanguage(int languageId, int page) throws Exception {
        String url = baseUrl + "/films/search/byLanguage?languageId=" + languageId + "&page=" + page + "&size=10";
        String json = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(json);
    }

    private void resetFlags(Model model) {
        model.addAttribute("error", false);
        model.addAttribute("success", false);
        model.addAttribute("nameError", false);
        model.addAttribute("nameErrMsg", "");
    }
}
