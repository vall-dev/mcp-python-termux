package com.animekita.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.animekita.DetailActivity;
import com.animekita.R;
import com.animekita.model.Anime;

import java.util.List;

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.ViewHolder> {

    private Context context;
    private List<Anime> animeList;

    public AnimeAdapter(Context context, List<Anime> animeList) {
        this.context = context;
        this.animeList = animeList;
    }

    public void updateList(List<Anime> newList) {
        this.animeList = newList;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anime, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Anime anime = animeList.get(position);

        holder.titleText.setText(anime.getTitle());
        holder.genreText.setText(anime.getGenre());

        String rating = anime.getRating();
        if (rating != null && !rating.trim().isEmpty()) {
            holder.ratingText.setText(rating);
            holder.ratingBadge.setVisibility(View.VISIBLE);
        } else {
            holder.ratingBadge.setVisibility(View.GONE);
        }

        // Load thumbnail with Glide
        Glide.with(context)
                .load(anime.getThumb())
                .placeholder(R.drawable.placeholder_anime)
                .error(R.drawable.placeholder_anime)
                .into(holder.thumbImage);

        // Click to open detail
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(context, DetailActivity.class)
                        .putExtra("anime_id", anime.getId())
                        .putExtra("anime_title", anime.getTitle())
                        .putExtra("anime_thumb", anime.getThumb()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return animeList != null ? animeList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbImage;
        TextView titleText;
        TextView genreText;
        TextView ratingText;
        android.view.View ratingBadge;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbImage = itemView.findViewById(R.id.thumb_image);
            titleText = itemView.findViewById(R.id.title_text);
            genreText = itemView.findViewById(R.id.genre_text);
            ratingText = itemView.findViewById(R.id.rating_text);
            ratingBadge = itemView.findViewById(R.id.rating_badge);
        }
    }
}
