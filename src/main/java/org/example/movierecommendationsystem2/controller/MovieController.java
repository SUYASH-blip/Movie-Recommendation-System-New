package org.example.movierecommendationsystem2.controller;

import lombok.Getter;
import org.example.movierecommendationsystem2.model.MovieMatch;
import org.example.movierecommendationsystem2.services.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private MovieService movieService;

    public MovieController(MovieService movieService){
        this.movieService = movieService;
    }

    @GetMapping("/search")
    public List<MovieMatch> search(@RequestParam String query) {
        return movieService.search(query);
    }


    @GetMapping("/{title}/similar")
    public List<MovieMatch> similarMovies(@PathVariable String title) {
        return movieService.similarMovies(title);
    }


}
