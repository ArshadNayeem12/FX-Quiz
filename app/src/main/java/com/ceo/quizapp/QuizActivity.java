package com.ceo.quizapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
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

public class QuizActivity extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;
    private int questionsAnswered = 0;
    private final int QUESTIONS_PER_LEVEL = 5; 
    
    private String selectedClass, selectedDifficulty;
    private TextView questionText, levelInfo;
    private Button option1, option2, option3, option4;
    private String currentAnswer = "";

    // YAHAN APNI GOOGLE AI STUDIO KI API KEY DAALNI HAI
    private static final String GEMINI_API_KEY = ""; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        selectedClass = getIntent().getStringExtra("CLASS_LEVEL");
        selectedDifficulty = getIntent().getStringExtra("DIFFICULTY");

        levelInfo = findViewById(R.id.levelInfo);
        questionText = findViewById(R.id.questionText);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);

        levelInfo.setText(selectedClass + " | " + selectedDifficulty);

        AdView mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());

        loadInterstitialAd();
        fetchAIQuestion();
        
        option1.setOnClickListener(v -> onAnswerSelected(option1.getText().toString()));
        option2.setOnClickListener(v -> onAnswerSelected(option2.getText().toString()));
        option3.setOnClickListener(v -> onAnswerSelected(option3.getText().toString()));
        option4.setOnClickListener(v -> onAnswerSelected(option4.getText().toString()));
    }

    private void fetchAIQuestion() {
        questionText.setText("Loading AI Data...");
        option1.setText("-"); option2.setText("-"); option3.setText("-"); option4.setText("-");

        executorService.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String promptText = "Generate 1 multiple-choice educational question for " + selectedClass + 
                                    " students at " + selectedDifficulty + " difficulty. " +
                                    "Provide 4 options. You MUST return ONLY a raw JSON object in this exact format, with no markdown formatting or extra text: " +
                                    "{\"question\": \"What is 2+2?\", \"options\": [\"1\", \"2\", \"3\", \"4\"], \"answer\": \"4\"}";

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

                    parseAndDisplayQuestion(response.toString());
                } else {
                    mainHandler.post(() -> questionText.setText("API Error: Server par load zyada hai."));
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_ERROR", "Error fetching from Gemini", e);
                mainHandler.post(() -> questionText.setText("Connection failed. Check Internet."));
            }
        });
    }

    private void parseAndDisplayQuestion(String apiResponse) {
        try {
            JSONObject jsonObject = new JSONObject(apiResponse);
            JSONArray candidates = jsonObject.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String rawJsonString = parts.getJSONObject(0).getString("text");

            rawJsonString = rawJsonString.replace("```json", "").replace("```", "").trim();

            JSONObject questionData = new JSONObject(rawJsonString);
            String question = questionData.getString("question");
            JSONArray options = questionData.getJSONArray("options");
            currentAnswer = questionData.getString("answer");

            mainHandler.post(() -> {
                try {
                    questionText.setText(question);
                    option1.setText(options.getString(0));
                    option2.setText(options.getString(1));
                    option3.setText(options.getString(2));
                    option4.setText(options.getString(3));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            Log.e("PARSE_ERROR", "JSON parsing failed", e);
            mainHandler.post(() -> questionText.setText("Failed to read AI data. Retrying..."));
        }
    }

    private void onAnswerSelected(String selectedOption) {
        // Logic for answer checking can be added here
        questionsAnswered++;
        if (questionsAnswered >= QUESTIONS_PER_LEVEL) {
            showInterstitialAd(); 
        } else {
            fetchAIQuestion(); 
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

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    questionsAnswered = 0; 
                    mInterstitialAd = null;
                    loadInterstitialAd(); 
                    fetchAIQuestion(); 
                }
            });
            mInterstitialAd.show(QuizActivity.this);
        } else {
            questionsAnswered = 0;
            fetchAIQuestion();
        }
    }
}
