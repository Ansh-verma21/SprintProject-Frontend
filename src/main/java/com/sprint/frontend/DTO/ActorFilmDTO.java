package com.sprint.frontend.DTO;

public class ActorFilmDTO {

    private ActorDTO actor;
    private FilmInfoDTO film;

    public ActorDTO getActor() {
        return actor;
    }

    public void setActor(ActorDTO actor) {
        this.actor = actor;
    }

    public FilmInfoDTO getFilm() {
        return film;
    }

    public void setFilm(FilmInfoDTO film) {
        this.film = film;
    }
}