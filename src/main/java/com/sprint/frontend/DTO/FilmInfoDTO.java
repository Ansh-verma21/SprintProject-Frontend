package com.sprint.frontend.DTO;

public class FilmInfoDTO {

    private String title;
    private String releaseYear;
    private String rating;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = safe(title);
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = safe(releaseYear);
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = safe(rating);
    }

    private String safe(String val) {
        return (val == null || val.isEmpty()) ? "N/A" : val;
    }
}