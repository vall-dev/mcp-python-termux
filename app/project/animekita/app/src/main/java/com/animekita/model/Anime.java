package com.animekita.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Anime {
    private String id;
    private String title;
    private String thumb;
    private String genre;
    private String rating;
    private String status;
    private String synopsis;
    private String type; // "ongoing" or "completed"
    private List<Episode> episodes;

    public Anime() {
        episodes = new ArrayList<>();
    }

    public static Anime fromJson(JSONObject json) throws JSONException {
        Anime anime = new Anime();
        anime.id = json.optString("id", "");
        anime.title = json.optString("title", "");
        anime.thumb = json.optString("thumb", "");
        anime.genre = json.optString("genre", "");
        anime.rating = json.optString("rating", "");
        anime.status = json.optString("status", "");
        anime.synopsis = json.optString("synopsis", "");
        anime.type = json.optString("type", "");

        // Parse episodes if present
        if (json.has("episodes")) {
            JSONArray epsArray = json.getJSONArray("episodes");
            for (int i = 0; i < epsArray.length(); i++) {
                anime.episodes.add(Episode.fromJson(epsArray.getJSONObject(i)));
            }
        }

        // Parse episode_list if present (alternative field name)
        if (json.has("episode_list")) {
            JSONArray epsArray = json.getJSONArray("episode_list");
            for (int i = 0; i < epsArray.length(); i++) {
                anime.episodes.add(Episode.fromJson(epsArray.getJSONObject(i)));
            }
        }

        return anime;
    }

    public static List<Anime> fromJsonArray(JSONArray array) throws JSONException {
        List<Anime> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(fromJson(array.getJSONObject(i)));
        }
        return list;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getThumb() { return thumb; }
    public String getGenre() { return genre; }
    public String getRating() { return rating; }
    public String getStatus() { return status; }
    public String getSynopsis() { return synopsis; }
    public String getType() { return type; }
    public List<Episode> getEpisodes() { return episodes; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setThumb(String thumb) { this.thumb = thumb; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setRating(String rating) { this.rating = rating; }
    public void setStatus(String status) { this.status = status; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public void setType(String type) { this.type = type; }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }
}
