package com.sprint.frontend.Controller;


import com.sprint.frontend.DTO.FilmDTO;
import com.sprint.frontend.DTO.FilmDetailDTO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Controller
public class FilmController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "http://localhost:8000";
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
                url = BASE_URL +
                        "/films/search/byTitle" +
                        "?title=" + title +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;

            } else if (title != null && !title.isEmpty()) {

                // 🔥 TITLE + YEAR
                url = BASE_URL +
                        "/films/search/byTitleAndYear" +
                        "?title=" + title +
                        "&releaseYear=" + year +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;

            } else {

                // 🔥 YEAR ONLY (YOUR CUSTOM API)
                url = BASE_URL +
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
   
}