package com.animekita.model;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HomeResponse {
    private List<Anime> ongoing;
    private List<Anime> completed;
    private List<Anime> allAnime;

    public HomeResponse() {
        ongoing = new ArrayList<>();
        completed = new ArrayList<>();
        allAnime = new ArrayList<>();
    }

    public static HomeResponse fromJson(JSONObject json) throws JSONException {
        HomeResponse response = new HomeResponse();

        // Parse "data" object
        JSONObject data = json.optJSONObject("data");
        if (data != null) {
            // Parse ongoing list
            if (data.has("ongoing")) {
                response.ongoing = Anime.fromJsonArray(data.getJSONArray("ongoing"));
                for (Anime a : response.ongoing) a.setType("ongoing");
            }

            // Parse completed list
            if (data.has("completed")) {
                response.completed = Anime.fromJsonArray(data.getJSONArray("completed"));
                for (Anime a : response.completed) a.setType("completed");
            }

            // Parse anime list (all)
            if (data.has("anime")) {
                response.allAnime = Anime.fromJsonArray(data.getJSONArray("anime"));
            }

            // Parse list (alternative)
            if (data.has("list")) {
                response.allAnime = Anime.fromJsonArray(data.getJSONArray("list"));
            }
        }

        // Fallback: parse directly from root if no "data" wrapper
        if (response.ongoing.isEmpty() && response.completed.isEmpty() && response.allAnime.isEmpty()) {
            if (json.has("ongoing")) {
                response.ongoing = Anime.fromJsonArray(json.getJSONArray("ongoing"));
                for (Anime a : response.ongoing) a.setType("ongoing");
            }
            if (json.has("completed")) {
                response.completed = Anime.fromJsonArray(json.getJSONArray("completed"));
                for (Anime a : response.completed) a.setType("completed");
            }
            if (json.has("anime")) {
                response.allAnime = Anime.fromJsonArray(json.getJSONArray("anime"));
            }
            if (json.has("list")) {
                response.allAnime = Anime.fromJsonArray(json.getJSONArray("list"));
            }
        }

        return response;
    }

    public List<Anime> getOngoing() { return ongoing; }
    public List<Anime> getCompleted() { return completed; }
    public List<Anime> getAllAnime() { return allAnime; }
}
