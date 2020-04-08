package com.evo.browser.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.evo.browser.R;
import com.evo.browser.activities.LicensesActivity;
import com.evo.browser.activities.MainActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static SettingsFragment newInstance() {
        Bundle args = new Bundle();
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        SwitchPreference darkTheme = findPreference("night");
        darkTheme.setOnPreferenceClickListener(preference -> {
            getActivity().moveTaskToBack(true);
            startActivity(new Intent(requireContext(), MainActivity.class));
            return false;
        });

        Preference licenses = findPreference("licenses");
        licenses.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), LicensesActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return false;
        });
    }
}