package com.animekita;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.animekita.model.Episode;
import com.animekita.network.ApiClient;

public class PlayerActivity extends Activity {

    private VideoView videoView;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText, titleText;
    private LinearLayout topOverlay, errorContainer;
    private FrameLayout fullscreenContainer;
    private View tapCatcher;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private String episodeId;
    private String episodeTitle;

    private boolean controlsVisible = true;
    private final Handler hideHandler = new Handler();
    private static final int AUTO_HIDE_DELAY = 3000;

    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            if (controlsVisible) {
                hideControls();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        episodeId = getIntent().getStringExtra("episode_id");
        episodeTitle = getIntent().getStringExtra("episode_title");

        videoView = findViewById(R.id.video_view);
        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.player_progress);
        errorText = findViewById(R.id.player_error);
        titleText = findViewById(R.id.player_title);
        topOverlay = findViewById(R.id.top_overlay);
        errorContainer = findViewById(R.id.error_container);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        tapCatcher = findViewById(R.id.tap_catcher);

        setupWebView();

        titleText.setText(episodeTitle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Tap to toggle controls
        tapCatcher.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    toggleControls();
                }
                return true;
            }
        });

        loadVideo();
    }

    // ==================== CONTROLS AUTO-HIDE ====================

    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
            resetAutoHide();
        }
    }

    private void showControls() {
        controlsVisible = true;
        topOverlay.setVisibility(View.VISIBLE);
        topOverlay.animate().alpha(1f).setDuration(200).start();
    }

    private void hideControls() {
        controlsVisible = false;
        topOverlay.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                topOverlay.setVisibility(View.GONE);
            }
        }).start();
    }

    private void resetAutoHide() {
        hideHandler.removeCallbacks(hideControlsRunnable);
        hideHandler.postDelayed(hideControlsRunnable, AUTO_HIDE_DELAY);
    }

    // ==================== VIDEO ====================

    private void loadVideo() {
        progressBar.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);

        ApiClient.fetchEpisode(episodeId, new ApiClient.Callback<Episode>() {
            @Override
            public void onSuccess(final Episode episode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playVideo(episode.getVideoUrl());
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
                        errorContainer.setVisibility(View.VISIBLE);
                        Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Honor the embed's own fullscreen request.
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                topOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                errorText.setText("Gagal memuat video");
                errorContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        fullscreenContainer.setVisibility(View.GONE);
        fullscreenContainer.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    private boolean isVideoUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".m3u8")
            || lower.endsWith(".mkv") || lower.endsWith(".webm")
            || lower.endsWith(".avi") || lower.endsWith(".ts");
    }

    private void loadWebView(String url) {
        videoView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        // Let the embed handle its own taps (play/pause/fullscreen buttons).
        tapCatcher.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        webView.loadUrl(url);
    }

    private void playVideo(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            errorText.setText("No video URL found");
            errorContainer.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Video URL not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isVideoUrl(videoUrl)) {
            loadWebView(videoUrl);
            return;
        }

        try {
            progressBar.setVisibility(View.GONE);
            tapCatcher.setVisibility(View.VISIBLE);
            Uri uri = Uri.parse(videoUrl);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(android.media.MediaPlayer mp) {
                    videoView.start();
                    resetAutoHide();
                }
            });

            videoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                    loadWebView(videoUrl);
                    return true;
                }
            });

            videoView.start();
        } catch (Exception e) {
            loadWebView(videoUrl);
        }
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onBackPressed() {
        // Exit the embed's fullscreen first, then the activity.
        if (customView != null) {
            hideCustomView();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
        if (webView != null) {
            webView.onPause();
        }
        hideHandler.removeCallbacks(hideControlsRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        if (controlsVisible) {
            resetAutoHide();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacks(hideControlsRunnable);
        if (videoView != null) {
            videoView.stopPlayback();
        }
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
    }
}
