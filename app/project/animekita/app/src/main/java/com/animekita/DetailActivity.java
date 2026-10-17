package com.animekita;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.animekita.adapter.EpisodeAdapter;
import com.animekita.model.Anime;
import com.animekita.network.ApiClient;

public class DetailActivity extends Activity {

    private ImageView thumbImage;
    private TextView titleText, genreText, statusText, ratingText, synopsisText;
    private RecyclerView episodeRecycler;
    private ProgressBar progressBar;
    private TextView errorText;
    private EpisodeAdapter episodeAdapter;

    private String animeId;
    private String animeTitle;
    private String animeThumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        animeId = getIntent().getStringExtra("anime_id");
        animeTitle = getIntent().getStringExtra("anime_title");
        animeThumb = getIntent().getStringExtra("anime_thumb");

        thumbImage = findViewById(R.id.detail_thumb);
        titleText = findViewById(R.id.detail_title);
        genreText = findViewById(R.id.detail_genre);
        statusText = findViewById(R.id.detail_status);
        ratingText = findViewById(R.id.detail_rating);
        synopsisText = findViewById(R.id.detail_synopsis);
        episodeRecycler = findViewById(R.id.episode_recycler);
        progressBar = findViewById(R.id.detail_progress);
        errorText = findViewById(R.id.detail_error);

        episodeRecycler.setLayoutManager(new LinearLayoutManager(this));
        episodeAdapter = new EpisodeAdapter(this, new java.util.ArrayList<>());
        episodeRecycler.setAdapter(episodeAdapter);

        // Show basic info from intent
        titleText.setText(animeTitle);
        if (animeThumb != null && !animeThumb.isEmpty()) {
            Glide.with(this)
                    .load(animeThumb)
                    .placeholder(R.drawable.placeholder_anime)
                    .error(R.drawable.placeholder_anime)
                    .into(thumbImage);
        }

        loadDetail();
    }

    public void onBackClicked(View view) {
        finish();
    }

    private void loadDetail() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);

        ApiClient.fetchDetail(animeId, new ApiClient.Callback<Anime>() {
            @Override
            public void onSuccess(final Anime anime) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);

                        titleText.setText(anime.getTitle());
                        genreText.setText("Genre: " + anime.getGenre());
                        statusText.setText("Status: " + anime.getStatus());
                        ratingText.setText("Rating: " + anime.getRating());
                        synopsisText.setText(anime.getSynopsis());

                        if (anime.getThumb() != null && !anime.getThumb().isEmpty()) {
                            Glide.with(DetailActivity.this)
                                    .load(anime.getThumb())
                                    .placeholder(R.drawable.placeholder_anime)
                                    .error(R.drawable.placeholder_anime)
                                    .into(thumbImage);
                        }

                        if (anime.getEpisodes() != null && !anime.getEpisodes().isEmpty()) {
                            episodeAdapter = new EpisodeAdapter(DetailActivity.this, anime.getEpisodes());
                            episodeRecycler.setAdapter(episodeAdapter);
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        errorText.setText("Error: " + error);
                        errorText.setVisibility(View.VISIBLE);
                        Toast.makeText(DetailActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
