package com.sprint.frontend.Controller;


import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import com.sprint.frontend.DTO.FilmDTO;
import com.sprint.frontend.DTO.FilmDetailDTO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Controller
public class FilmController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.baseUrl}")
    private String baseUrl;
    private static final int PAGE_SIZE = 5;

    @GetMapping("/member/1")
    public String getFilms(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "releaseYear", required = false) String year,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int currentPage = 0;
        int totalPages = 1;
        List<FilmDTO> films = new ArrayList<>();

        try {

            // 🔥 Convert date → year (important)
            if (year != null && year.contains("-")) {
                year = year.substring(0, 4);
            }

            String url;

            if (title != null && !title.isEmpty() && (year == null || year.isEmpty())) {

                // 🔥 TITLE ONLY
                url = baseUrl +
                        "/films/search/byTitle" +
                        "?title=" + title +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;

            } else if (title != null && !title.isEmpty()) {

                // 🔥 TITLE + YEAR
                url = baseUrl +
                        "/films/search/byTitleAndYear" +
                        "?title=" + title +
                        "&releaseYear=" + year +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;

            } else {

                // 🔥 YEAR ONLY (YOUR CUSTOM API)
                url = baseUrl +
                        "/films/search/byReleaseYear" +
                        "?releaseYear=" + (year != null ? year : "2006") +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;
            }

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode f : root.path("content")) {

                FilmDTO dto = new FilmDTO();

                dto.setFilmId(extractId(f.path("links")));
                dto.setTitle(f.path("title").asText());
                dto.setDescription(f.path("description").asText("N/A"));
                dto.setReleaseYear(f.path("releaseYear").asText());
                dto.setRating(f.path("rating").asText("N/A"));

                films.add(dto);
            }

            JsonNode pageNode = root.path("page");

            if (!pageNode.isMissingNode()) {
                currentPage = pageNode.path("number").asInt(0);
                totalPages = pageNode.path("totalPages").asInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading films");
        }

        // 🔥 IMPORTANT
        model.addAttribute("films", films);
        model.addAttribute("noData", films.isEmpty());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("title", title);
        model.addAttribute("releaseYear", year);

        return "film";
    }
    @GetMapping("/member/1/film")
    public String getFilmDetails(@RequestParam Long id, Model model) {

        try {

            FilmDetailDTO dto = new FilmDetailDTO();

            // 🔹 1. Film basic data
            String filmJson = restTemplate.getForObject(baseUrl + "/films/" + id, String.class);
            JsonNode film = objectMapper.readTree(filmJson);

            dto.setTitle(film.path("title").asText());
            dto.setDescription(film.path("description").asText());
            dto.setReleaseYear(film.path("releaseYear").asText());
            dto.setRating(film.path("rating").asText());
            dto.setLength(film.path("length").asInt());

            // 🔹 2. Language
            String langJson = restTemplate.getForObject(baseUrl + "/films/" + id + "/language", String.class);
            JsonNode lang = objectMapper.readTree(langJson);
            dto.setLanguage(lang.path("name").asText());

            // 🔹 3. Actors
            String actorJson = restTemplate.getForObject(baseUrl + "/films/" + id + "/actors", String.class);
            JsonNode actorRoot = objectMapper.readTree(actorJson);

            List<String> actors = new ArrayList<>();
            for (JsonNode a : actorRoot.path("content")) {
                actors.add(a.path("firstName").asText() + " " + a.path("lastName").asText());
            }
            dto.setActors(actors);

            // 🔹 4. Categories
            String catJson = restTemplate.getForObject(baseUrl + "/films/" + id + "/categories", String.class);
            JsonNode catRoot = objectMapper.readTree(catJson);

            List<String> categories = new ArrayList<>();
            for (JsonNode c : catRoot.path("content")) {
                categories.add(c.path("name").asText());
            }
            dto.setCategories(categories);

            model.addAttribute("film", dto);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load film details");
        }

        return "film-detail";
    }

    private Long extractId(JsonNode linksNode) {
        for (JsonNode link : linksNode) {
            if ("self".equals(link.path("rel").asText())) {

                String href = link.path("href").asText();

                if (href.contains("{")) {
                    href = href.substring(0, href.indexOf("{"));
                }

                return Long.parseLong(
                        href.substring(href.lastIndexOf("/") + 1)
                );
            }
        }
        return null;
    }
}