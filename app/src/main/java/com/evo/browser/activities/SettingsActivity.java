package com.evo.browser.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.evo.browser.R;
import com.evo.browser.fragments.SettingsFragment;
import com.evo.browser.utils.ThemeUtils;
import com.evo.browser.view.CenteredToolbar;

public class SettingsActivity extends AppCompatActivity {

    private CenteredToolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Настройка для переключения тем
        setTheme(ThemeUtils.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Настройка Toolbar
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.settings);
        mToolbar.setNavigationIcon(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("night", false)
                ? R.drawable.ic_arrow_back_white_24dp : R.drawable.ic_arrow_back_black_24dp);
        // Обработка нажатия кнопки "Назад" (в Toolbar)
        mToolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        // Менеджер фрагмента(ов)
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, SettingsFragment.newInstance())
                .commit();
    }

    // Анимаций

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
