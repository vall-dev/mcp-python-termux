package com.animekita;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.animekita.adapter.AnimeAdapter;
import com.animekita.model.Anime;
import com.animekita.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity {

    private EditText searchInput;
    private RecyclerView searchRecycler;
    private ProgressBar progressBar;
    private TextView errorText, emptyText;
    private AnimeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.search_input);
        searchRecycler = findViewById(R.id.search_recycler);
        progressBar = findViewById(R.id.search_progress);
        errorText = findViewById(R.id.search_error);
        emptyText = findViewById(R.id.search_empty);

        searchRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AnimeAdapter(this, new ArrayList<>());
        searchRecycler.setAdapter(adapter);

        // Search on Enter key
        searchInput.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(searchInput.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });

        // Or search button click
        findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch(searchInput.getText().toString().trim());
            }
        });

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        searchRecycler.setVisibility(View.GONE);

        ApiClient.searchAnime(query, new ApiClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(final List<Anime> results) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);

                        if (results.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            searchRecycler.setVisibility(View.GONE);
                        } else {
                            emptyText.setVisibility(View.GONE);
                            searchRecycler.setVisibility(View.VISIBLE);
                            adapter.updateList(results);
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
                        Toast.makeText(SearchActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
