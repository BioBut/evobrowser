package com.evo.browser.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.evo.browser.R;
import com.evo.browser.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
            Uri uri = Uri.parse("https://news.google.com/");
            Intent intent = new Intent (Intent.ACTION_VIEW, uri);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, запуск Google Voice Search
        button = findViewById(R.id.search_voice_btn);
        button.setOnClickListener(v -> promptSpeechInput());
        // Обработка нажатия на FAB, открытие ссылки YouTube
        button = findViewById(R.id.youtube);
        button.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://youtube.com/");
            Intent intent = new Intent (Intent.ACTION_VIEW, uri);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, открытие ссылки с картами
        button = findViewById(R.id.map);
        button.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://maps.google.com/");
            Intent intent = new Intent (Intent.ACTION_VIEW, uri);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        // Обработка нажатия на FAB, открытие ссылки с переводчиком
        button = findViewById(R.id.translate);
        button.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://translate.google.com/");
            Intent intent = new Intent (Intent.ACTION_VIEW, uri);
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
       // Код связывающий показание первого, предупреждающего диалога
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean firstStart = prefs.getBoolean("firstStart", true);

        if (firstStart) {
            showStartDialog();
        }

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
    // Единичный показ предупреждающего диалога (при самом первом запуске приложения)
    private void showStartDialog() {
        new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme)
                .setTitle(R.string.dialog_t)
                .setMessage(R.string.dialog_s)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("firstStart", false);
        editor.apply();
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
