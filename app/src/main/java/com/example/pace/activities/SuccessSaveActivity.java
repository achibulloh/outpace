package com.example.pace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pace.R;

public class SuccessSaveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_save);

        String firebaseId = getIntent().getStringExtra("FIREBASE_ID");
        
        FrameLayout iconContainer = findViewById(R.id.flIconContainer);
        TextView title = findViewById(R.id.tvSuccessTitle);
        TextView sub = findViewById(R.id.tvSuccessSub);
        LinearLayout loadingContainer = findViewById(R.id.llLoading);
        TextView loadingStatus = findViewById(R.id.tvLoadingStatus);
        
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // Animation: Icon Zoom and Fade In
        iconContainer.setScaleX(0f);
        iconContainer.setScaleY(0f);
        iconContainer.setAlpha(0f);
        iconContainer.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animation: Text Slide Up and Fade In
        title.setTranslationY(50f);
        title.setAlpha(0f);
        title.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(400)
                .start();

        sub.setTranslationY(50f);
        sub.setAlpha(0f);
        sub.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(600)
                .start();

        // Animation: Loading UI entry
        loadingContainer.setAlpha(0f);
        loadingContainer.animate().alpha(1f).setStartDelay(1000).setDuration(500).start();
        loadingStatus.setAlpha(0f);
        loadingStatus.animate().alpha(1f).setStartDelay(1200).setDuration(500).start();

        // Animated Dots logic
        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);

        // Delay then go to Detail
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, ActivityDetailActivity.class);
            intent.putExtra("FIREBASE_ID", firebaseId);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 3500); // Increased delay slightly for better UX
    }

    private void animateDot(View dot, int delay) {
        dot.animate()
                .translationY(-15f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .withEndAction(() -> {
                    dot.animate()
                            .translationY(0f)
                            .alpha(0.3f)
                            .setDuration(400)
                            .withEndAction(() -> animateDot(dot, 0)) // Repeat
                            .start();
                })
                .start();
    }
}
