package com.sprint.frontend.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;



import com.sprint.frontend.DTO.ActorDTO;
import com.sprint.frontend.DTO.ActorFilmDTO;
import com.sprint.frontend.DTO.FilmInfoDTO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Controller
public class ActorController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "http://localhost:8000";
    private static final int PAGE_SIZE = 5;

    // =========================
    // ✅ PAGE 2 → ACTOR LIST
    // =========================
    @GetMapping("/actors")
    public String getActors(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        List<ActorDTO> actors = new ArrayList<>();
        int currentPage = 0;
        int totalPages = 1;

        try {

            String url;

            if (firstName != null && !firstName.trim().isEmpty()) {

                // 🔍 SEARCH
                url = BASE_URL +
                        "/actors/search/findByFirstNameContainingIgnoreCase" +
                        "?firstName=" + firstName +
                        "&page=" + page +
                        "&size=" + PAGE_SIZE;

            } else {

                // 📋 DEFAULT LIST
                url = BASE_URL +
                        "/actors?page=" + page +
                        "&size=" + PAGE_SIZE;
            }

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode node : root.path("content")) {

                ActorDTO actor = new ActorDTO();

                actor.setFirstName(node.path("firstName").asText());
                actor.setLastName(node.path("lastName").asText());

                actors.add(actor);
            }

            JsonNode pageNode = root.path("page");

            if (!pageNode.isMissingNode()) {
                currentPage = pageNode.path("number").asInt(0);
                totalPages = pageNode.path("totalPages").asInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load actors");
        }

        model.addAttribute("actors", actors);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("firstName", firstName);
        model.addAttribute("noData", actors.isEmpty());

        return "actor"; // actor.html
    }

    // =========================
    // ✅ PAGE 3 → ACTOR + FILMS
    // =========================
    @GetMapping("/actors/films")
    public String getActorFilms(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        List<ActorFilmDTO> data = new ArrayList<>();
        int currentPage = 0;
        int totalPages = 1;

        try {

            // 🔥 Use filmActors API (CORRECT for actor + film mapping)
            String url = BASE_URL +
                    "/filmActors/search/findByActorName" +
                    "?firstName=" + firstName +
                    "&lastName=" + lastName +
                    "&page=" + page +
                    "&size=" + PAGE_SIZE;

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode node : root.path("content")) {

                ActorFilmDTO dto = new ActorFilmDTO();

                // 🔹 Actor
                ActorDTO actor = new ActorDTO();
                actor.setFirstName(node.path("actor").path("firstName").asText());
                actor.setLastName(node.path("actor").path("lastName").asText());

                // 🔹 Film
                FilmInfoDTO film = new FilmInfoDTO();
                film.setTitle(node.path("film").path("title").asText());
                film.setReleaseYear(node.path("film").path("releaseYear").asText());
                film.setRating(node.path("film").path("rating").asText());

                dto.setActor(actor);
                dto.setFilm(film);

                data.add(dto);
            }

            JsonNode pageNode = root.path("page");

            if (!pageNode.isMissingNode()) {
                currentPage = pageNode.path("number").asInt(0);
                totalPages = pageNode.path("totalPages").asInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load actor films");
        }

        model.addAttribute("data", data);
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("noData", data.isEmpty());

        return "actor-film"; // actor-film.html
    }
}