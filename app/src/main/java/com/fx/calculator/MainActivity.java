package com.fx.calculator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;
    private int calcCount = 0;
    private final int ADS_INTERVAL = 3; 
    
    private EditText inputMath;
    private TextView tvResult;
    private Button btnSolve;

    private static final String GEMINI_API_KEY = "YAHAN_APNI_API_KEY_DAAL"; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {});

        inputMath = findViewById(R.id.inputMath);
        tvResult = findViewById(R.id.tvResult);
        btnSolve = findViewById(R.id.btnSolve);

        AdView mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());

        loadInterstitialAd();
        
        btnSolve.setOnClickListener(v -> {
            String equation = inputMath.getText().toString().trim();
            if(!equation.isEmpty()){
                solveEquation(equation);
            }
        });
    }

    private void solveEquation(String equation) {
        tvResult.setText("FX AI is calculating step-by-step...\nAnalyzing roots and equations...");
        btnSolve.setEnabled(false);

        executorService.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String promptText = "Act as an advanced ultra-modern math solver. Solve the following equation/problem step-by-step: '" + equation + "'. Clearly show all equations, roots, formulas used, and the final answer. Format the text beautifully using newlines, but DO NOT use markdown blocks like ```json or ```. Return plain readable text.";

                JSONObject part = new JSONObject();
                part.put("text", promptText);
                JSONArray parts = new JSONArray();
                parts.put(part);
                JSONObject content = new JSONObject();
                content.put("parts", parts);
                JSONArray contents = new JSONArray();
                contents.put(content);
                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    parseAndDisplayResult(response.toString());
                } else {
                    mainHandler.post(() -> {
                        tvResult.setText("API Error: Server load is high.");
                        btnSolve.setEnabled(true);
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvResult.setText("Connection failed. Check Internet.");
                    btnSolve.setEnabled(true);
                });
            }
        });
    }

    private void parseAndDisplayResult(String apiResponse) {
        try {
            JSONObject jsonObject = new JSONObject(apiResponse);
            JSONArray candidates = jsonObject.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String solution = parts.getJSONObject(0).getString("text");

            mainHandler.post(() -> {
                tvResult.setText(solution);
                btnSolve.setEnabled(true);
                checkAndShowAd();
            });

        } catch (Exception e) {
            mainHandler.post(() -> {
                tvResult.setText("Failed to parse AI response.");
                btnSolve.setEnabled(true);
            });
        }
    }

    private void checkAndShowAd() {
        calcCount++;
        if (calcCount >= ADS_INTERVAL) {
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        calcCount = 0; 
                        mInterstitialAd = null;
                        loadInterstitialAd(); 
                    }
                });
                mInterstitialAd.show(MainActivity.this);
            }
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) { mInterstitialAd = interstitialAd; }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mInterstitialAd = null; }
            });
    }
}
