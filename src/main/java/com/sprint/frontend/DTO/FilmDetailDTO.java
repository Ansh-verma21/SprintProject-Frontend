package com.sprint.frontend.DTO;

import java.util.ArrayList;
import java.util.List;

public class FilmDetailDTO {

    private String title;
    private String description;
    private String releaseYear;
    private String rating;
    private Integer length;

    private String language;
    private List<String> actors = new ArrayList<>();
    private List<String> categories = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = safe(title); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = safe(description); }

    public String getReleaseYear() { return releaseYear; }
    public void setReleaseYear(String releaseYear) { this.releaseYear = safe(releaseYear); }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = safe(rating); }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length != null ? length : 0; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = safe(language); }

    public List<String> getActors() { return actors; }
    public void setActors(List<String> actors) { this.actors = actors; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    private String safe(String val) {
        return (val == null || val.isEmpty()) ? "N/A" : val;
    }
}