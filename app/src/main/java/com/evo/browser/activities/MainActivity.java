package com.evo.browser.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.evo.browser.R;
import com.evo.browser.utils.ThemeUtils;
import com.evo.browser.utils.UpdateUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText search_bar;
    private FloatingActionButton button;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Код, который работает так, если приложение установлено на смартфоне, то автоповорот не работает, если на планшете - работает
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // Настройка для переключения тем
        setTheme(ThemeUtils.getCurrentTheme());
        new UpdateUtils().execute(this);
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
    // OTA Обновления
    public void Update(final Integer lastAppVersion) {
        runOnUiThread(() -> new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme)
                .setTitle(R.string.ota_t)
                .setMessage(R.string.ota_s)
                // При нажати на "Да", генерируем ссылку и открываем её в WebView + автоматическое скачивание
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    Intent intent = new Intent(this, WebActivity.class);
                    String apkUrl = "https://github.com/dmitrylaas/Evolution-Browser/releases/download/" + lastAppVersion + "/app-release.apk";
                    intent.putExtra("page_url", (apkUrl));
                    intent.setData(Uri.parse(apkUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    dialogInterface.dismiss();
                })
                // При нажатии на "Нет", останавливаем менедждер и скрываем диалог
                .setNeutralButton(R.string.no, (dialogInterface, i) -> {
                    // Ниже приведена строка, если хотим, чтобы уведомление, при нажатии на кнопку "Нет", больше не показывалось вообще
                    /**SettingsManager.put(MainActivity.this, "LastIgnoredUpdateVersion", lastAppVersion.toString());**/
                    dialogInterface.dismiss();
                })
                .show());
    }
}
