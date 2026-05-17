package com.example.schoolevent.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.SharedPreferences;

import com.example.schoolevent.R;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load tema sebelum layout dibuat
        loadThemePreference();

        setContentView(R.layout.activity_splash);
        BlurView blurSplash = findViewById(R.id.blur_splash);
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
        blurSplash.setupWith(rootView, new RenderScriptBlur(this))
                .setBlurRadius(25f);

        LinearLayout layoutLogo = findViewById(R.id.layout_logo);

        // Animasi masuk → logo muncul dari bawah + fade in
        layoutLogo.setTranslationY(100f);
        layoutLogo.setAlpha(0f);

        ObjectAnimator translateY = ObjectAnimator.ofFloat(
                layoutLogo, "translationY", 100f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(
                layoutLogo, "alpha", 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateY, fadeIn);
        animatorSet.setDuration(800);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();

        // Pindah ke MainActivity setelah 2.5 detik
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            // Animasi transisi keluar
            if(Build.VERSION.SDK_INT >= 34){
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                        android.R.anim.fade_in, android.R.anim.fade_out);
            }else{
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            finish();
        }, 2500);
    }

    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}