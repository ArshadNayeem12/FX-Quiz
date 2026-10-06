package com.ceo.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class HomeActivity extends AppCompatActivity {

    private AppOpenAd appOpenAd = null;
    private Spinner classSpinner, difficultySpinner;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MobileAds.initialize(this, initializationStatus -> {});
        loadAppOpenAd(); 

        classSpinner = findViewById(R.id.classSpinner);
        difficultySpinner = findViewById(R.id.difficultySpinner);
        startBtn = findViewById(R.id.startBtn);

        String[] classes = {"Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10"};
        String[] difficulties = {"Easy", "Normal", "Hard", "Extreme"};
        
        classSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
        difficultySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, difficulties));

        startBtn.setOnClickListener(v -> {
            String selectedClass = classSpinner.getSelectedItem().toString();
            String selectedDifficulty = difficultySpinner.getSelectedItem().toString();
            
            Intent intent = new Intent(HomeActivity.this, QuizActivity.class);
            intent.putExtra("CLASS_LEVEL", selectedClass);
            intent.putExtra("DIFFICULTY", selectedDifficulty);
            startActivity(intent);
        });
    }

    private void loadAppOpenAd() {
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(this, "ca-app-pub-3940256099942544/3419835294", request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(AppOpenAd ad) {
                    appOpenAd = ad;
                    appOpenAd.show(HomeActivity.this);
                }
            });
    }
}
