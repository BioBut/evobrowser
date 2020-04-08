package com.evo.browser.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.evo.browser.R;


public class LicensesActivity extends AppCompatActivity {

    private WebView mWeb;

    // Настройка клиента
    private class WebViewer extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading (WebView view, String url)
        {
            view.loadUrl(url);
            return true;
        }
    }

    // Простая настройка и загрузка контента WebView
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        mWeb = findViewById(R.id.view);
        mWeb.getSettings().setJavaScriptEnabled(true);
        mWeb.loadUrl("file:///android_asset/index.html");
        mWeb.setWebViewClient(new WebViewer());
    }

    // Обработка кнопки "Назад"
    @Override
    public void onBackPressed() {
        if (mWeb.canGoBack()) {
            mWeb.goBack();}
        else {
            super.onBackPressed();
        }
    }

    // Анимации закрытия активити
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
