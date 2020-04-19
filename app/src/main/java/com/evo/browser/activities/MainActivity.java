package com.evo.browser.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.evo.browser.R;
import com.evo.browser.utils.ThemeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText search_bar;
    private FloatingActionButton button;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Настройка для переключения тем
        setTheme(ThemeUtils.getCurrentTheme());
        setContentView(R.layout.activity_main);
        // Связка строки ввода
        search_bar = findViewById(R.id.search_bar);
        // Обработка нажатия на FAB, открытие ссылки с новостями
        button = findViewById(R.id.news);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, WebActivity.class);
            intent.putExtra("page_url", "https://news.google.com/");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, запуск Google Voice Search
        button = findViewById(R.id.search_voice_btn);
        button.setOnClickListener(v -> promptSpeechInput());
        // Обработка нажатия на FAB, открытие закладок
        button = findViewById(R.id.bookmarks);
        button.setOnClickListener(v -> {
            Intent activitySetIntent = new Intent(getApplicationContext(), BookmarksActivity.class);
            startActivity(activitySetIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, открытие ссылки с картами
        button = findViewById(R.id.map);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, WebActivity.class);
            intent.putExtra("page_url", "https://maps.google.com/");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, открытие ссылки с переводчиком
        button = findViewById(R.id.translate);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, WebActivity.class);
            intent.putExtra("page_url", "https://translate.google.com/");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, открытие настроек
        button = findViewById(R.id.settings);
        button.setOnClickListener(v -> {
            Intent activitySetIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(activitySetIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия кнопки "Enter" на клавиатуре
       search_bar.setOnEditorActionListener((v, actionId, event) -> {
           if (actionId == EditorInfo.IME_ACTION_SEARCH) {
               performSearch();
               return true;
           }
           return false;
       });
        // Анимация картинки(логотипа) при старте (входе)
        ImageView imagelogo = findViewById(R.id.ic_evo);
        Animation logoAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        imagelogo.startAnimation(logoAnimation);
        // Анимация поисковой строки при старте (входе)
        TextInputEditText search = findViewById(R.id.search_bar);
        Animation serAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        search.startAnimation(serAnimation);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_news = findViewById(R.id.news);
        Animation fabAnimationN = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_news.startAnimation(fabAnimationN);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_voice = findViewById(R.id.search_voice_btn);
        Animation fabAnimationV = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_voice.startAnimation(fabAnimationV);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_youtube = findViewById(R.id.bookmarks);
        Animation fabAnimationY = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_youtube.startAnimation(fabAnimationY);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_map = findViewById(R.id.map);
        Animation fabAnimationM = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_map.startAnimation(fabAnimationM);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_translate = findViewById(R.id.translate);
        Animation fabAnimationT = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_translate.startAnimation(fabAnimationT);
        // Анимация FAB при старте (входе)
        FloatingActionButton fab_settings = findViewById(R.id.settings);
        Animation fabAnimationS = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fab_settings.startAnimation(fabAnimationS);
    }
    // Так называемая обработка дйствия пр нажатии "Enter"
    private void performSearch() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("text", search_bar.getText().toString());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    // Голосовой поиск
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }

    }

    // Вызов Google Voice Search, преобразование сказанного в текст и ввод преобразованного в поисковую строку соответственно
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    search_bar.setText(result.get(0));
                    Intent intent = new Intent(this, WebActivity.class);
                    intent.putExtra("text", search_bar.getText().toString());
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                break;
            }

        }
    }
}
