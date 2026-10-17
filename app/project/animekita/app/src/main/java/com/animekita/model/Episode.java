package com.animekita.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Episode {
    private String id;
    private String title;
    private String videoUrl;
    private String thumb;
    private String episodeNumber;

    public Episode() {}

    public static Episode fromJson(JSONObject json) throws JSONException {
        Episode episode = new Episode();
        episode.id = json.optString("id", "");
        episode.title = json.optString("title", "");
        episode.videoUrl = json.optString("video_url", "");
        episode.thumb = json.optString("thumb", "");
        episode.episodeNumber = json.optString("episode", "");

        // Try alternative field names
        if (episode.videoUrl.isEmpty()) {
            episode.videoUrl = json.optString("stream_url", "");
        }
        if (episode.videoUrl.isEmpty()) {
            episode.videoUrl = json.optString("url", "");
        }
        if (episode.episodeNumber.isEmpty()) {
            episode.episodeNumber = json.optString("episode_number", "");
        }

        return episode;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getVideoUrl() { return videoUrl; }
    public String getThumb() { return thumb; }
    public String getEpisodeNumber() { return episodeNumber; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setThumb(String thumb) { this.thumb = thumb; }
    public void setEpisodeNumber(String episodeNumber) { this.episodeNumber = episodeNumber; }
}
