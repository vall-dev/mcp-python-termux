package com.animekita.network;

import com.animekita.model.Anime;
import com.animekita.model.Episode;
import com.animekita.model.HomeResponse;

import java.util.List;

/**
 * ApiClient — sekarang langsung scraping dari nontonanimeid.boats via Scraper.
 * (Dulunya pakai API sankavollerei.web.id)
 */
public class ApiClient {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // Home
    public static void fetchHome(final Callback<HomeResponse> callback) {
        Scraper.fetchHome(1, new Scraper.Callback<HomeResponse>() {
            @Override
            public void onSuccess(HomeResponse r) { callback.onSuccess(r); }
            @Override
            public void onError(String e) { callback.onError(e); }
        });
    }

    // Detail anime
    public static void fetchDetail(String animeId, final Callback<Anime> callback) {
        Scraper.fetchDetail(animeId, new Scraper.Callback<Anime>() {
            @Override
            public void onSuccess(Anime r) { callback.onSuccess(r); }
            @Override
            public void onError(String e) { callback.onError(e); }
        });
    }

    // Episode (stream URL)
    public static void fetchEpisode(String episodeId, final Callback<Episode> callback) {
        Scraper.fetchEpisode(episodeId, new Scraper.Callback<Episode>() {
            @Override
            public void onSuccess(Episode r) { callback.onSuccess(r); }
            @Override
            public void onError(String e) { callback.onError(e); }
        });
    }

    // Search
    public static void searchAnime(String query, final Callback<List<Anime>> callback) {
        Scraper.searchAnime(query, 1, new Scraper.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> r) { callback.onSuccess(r); }
            @Override
            public void onError(String e) { callback.onError(e); }
        });
    }
}
