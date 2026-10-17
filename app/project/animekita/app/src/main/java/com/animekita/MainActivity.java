package com.animekita;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.animekita.adapter.AnimeAdapter;
import com.animekita.model.Anime;
import com.animekita.model.HomeResponse;
import com.animekita.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_NOTIFICATION_PERMISSION = 1001;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorText;
    private Button btnOngoing, btnCompleted;
    private ImageView btnSearch;
    private AnimeAdapter adapter;
    private List<Anime> ongoingList = new ArrayList<>();
    private List<Anime> completedList = new ArrayList<>();
    private String currentTab = "ongoing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        btnOngoing = findViewById(R.id.btn_ongoing);
        btnCompleted = findViewById(R.id.btn_completed);
        btnSearch = findViewById(R.id.btn_search);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AnimeAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        btnOngoing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("ongoing");
            }
        });

        btnCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("completed");
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });

        loadHomeData();
        requestNotificationPermissionIfNeeded();
    }

    /** Minta izin notifikasi (Android 13+); di bawahnya otomatis diizinkan saat install. */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onNotificationPermissionGranted();
            } else {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION_PERMISSION);
            }
        } else {
            onNotificationPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onNotificationPermissionGranted();
        }
    }

    /** Izin diberikan: tampilkan notifikasi terima kasih + simpan subscriber ke Firebase. */
    private void onNotificationPermissionGranted() {
        if (!SubscriberManager.isSubscribed(this)) {
            NotificationHelper.showThanksNotification(this);
            SubscriberManager.subscribe(this);
        }
    }

    private void switchTab(String tab) {
        currentTab = tab;
        if ("ongoing".equals(tab)) {
            btnOngoing.setBackgroundResource(R.drawable.badge_accent);
            btnOngoing.setTextColor(getResources().getColor(R.color.white));
            btnCompleted.setBackgroundResource(R.drawable.tab_bg);
            btnCompleted.setTextColor(getResources().getColor(R.color.text_secondary));
            adapter.updateList(ongoingList);
        } else {
            btnCompleted.setBackgroundResource(R.drawable.badge_accent);
            btnCompleted.setTextColor(getResources().getColor(R.color.white));
            btnOngoing.setBackgroundResource(R.drawable.tab_bg);
            btnOngoing.setTextColor(getResources().getColor(R.color.text_secondary));
            adapter.updateList(completedList);
        }

        if (adapter.getItemCount() == 0) {
            errorText.setText("No anime found");
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
        }
    }

    private void loadHomeData() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        ApiClient.fetchHome(new ApiClient.Callback<HomeResponse>() {
            @Override
            public void onSuccess(HomeResponse result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        ongoingList = result.getOngoing();
                        completedList = result.getCompleted();

                        // If both are empty, try allAnime
                        if (ongoingList.isEmpty() && completedList.isEmpty()) {
                            ongoingList = result.getAllAnime();
                            completedList = new ArrayList<>();
                        }

                        switchTab(currentTab);
                        recyclerView.setVisibility(View.VISIBLE);
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
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
