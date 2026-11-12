package org.example.movierecommendationsystem2.services;





import org.example.movierecommendationsystem2.model.MovieMatch;
import tools.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.example.movierecommendationsystem2.model.Movie;
import org.example.movierecommendationsystem2.model.MovieData;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class MovieService {
    private final EmbeddingModel embeddingModel;
    private final JsonMapper jsonMapper;


    List<Movie> moviesEmbedding = new ArrayList<>();

    public MovieService(EmbeddingModel embeddingModel,JsonMapper jsonMapper){
        this.embeddingModel = embeddingModel;
        this.jsonMapper = jsonMapper;
    }


    @PostConstruct
    public void initializeMovies() throws IOException {

        ClassPathResource resource = new ClassPathResource("movies.json");

        List<MovieData> movieDataList;
        try (InputStream inputStream = resource.getInputStream()) {
            movieDataList = jsonMapper.readValue(
                    inputStream, new TypeReference<List<MovieData>>() {});
        }

        int batchSize = 50;
        for (int i = 0; i < movieDataList.size(); i += batchSize) {
            List<MovieData> batch = movieDataList.subList(
                    i, Math.min(i + batchSize, movieDataList.size()));

            List<String> descriptions = batch.stream()
                    .map(MovieData::getDescription)
                    .toList();

            List<float[]> embeddings = embeddingModel.embed(descriptions);

            for (int j = 0; j < batch.size(); j++) {
                MovieData md = batch.get(j);
                moviesEmbedding.add(new Movie(
                        md.getTitle(), md.getDescription(), embeddings.get(j)));
            }
        }

        Movie first = moviesEmbedding.get(0);
        float[] emb = first.getEmbedding();
        System.out.println(first.getTitle() + " -> dimension: " + emb.length);
        System.out.println("First 5 values: " + Arrays.toString(Arrays.copyOf(emb, 5)));
    }

    public List<MovieMatch> search(String query) {
        float[] userQueryEmbedding = embeddingModel.embed(query);

        List<MovieMatch> matches = new ArrayList<>();

        for(Movie movie : moviesEmbedding) {
            double similarity = cosineSimilarity(userQueryEmbedding, movie.getEmbedding());

            MovieMatch match =
                    new MovieMatch(movie.getTitle(), movie.getDescription(), similarity);

            matches.add(match);
        }

        sortBySimilarity(matches);

        return topKMatches(matches, 3);
    }

    public List<MovieMatch> similarMovies(String title) {
        Movie selectedMovie = findMovie(title);
        List<MovieMatch> matches = new ArrayList<>();

        for (Movie movie : moviesEmbedding) {
            if (movie.getTitle().equalsIgnoreCase(title)) {
                continue;
            }

            double similarity = cosineSimilarity(
                    selectedMovie.getEmbedding(),
                    movie.getEmbedding());

            MovieMatch match = new MovieMatch(
                    movie.getTitle(),
                    movie.getDescription(),
                    similarity);

            matches.add(match);
        }
        sortBySimilarity(matches);
        return topKMatches(matches, 3);
    }

    private Movie findMovie(String title) {
        for (Movie movie : moviesEmbedding) {
            if (movie.getTitle().equalsIgnoreCase(title)) {
                return movie;
            }
        }

        throw new IllegalArgumentException(
                "Movie not found: " + title
        );
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += (a[i] * b[i]);
            normA += (a[i] * a[i]);
            normB += (b[i] * b[i]);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private void sortBySimilarity(List<MovieMatch> matches) {
        Collections.sort(matches,
                (first, second) -> Double.compare(
                        second.getMatch(),
                        first.getMatch())
        );
    }

    private List<MovieMatch> topKMatches(
            List<MovieMatch> matches, int limit) {

        List<MovieMatch> topMatches = new ArrayList<>();

        int numberOfMatches = Math.min(limit, matches.size());

        for (int i = 0; i < numberOfMatches; i++) {
            topMatches.add(matches.get(i));
        }

        return topMatches;
    }




    }


