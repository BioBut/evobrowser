package com.evo.browser.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.evo.browser.R;
import com.evo.browser.utils.ThemeUtils;
import com.evo.browser.view.CenteredToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.evo.browser.activities.WebActivity.PREFERENCES;
import static com.evo.browser.activities.WebActivity.WEB_LINKS;
import static com.evo.browser.activities.WebActivity.WEB_TITLE;

public class BookmarksActivity extends AppCompatActivity {

    ArrayList<HashMap<String, String>> listRowData;

    public static String TAG_TITLE = "title";
    public static String TAG_LINK = "link";

    private CenteredToolbar mToolbar;

    ListView listView;
    SimpleAdapter adapter;
    LinearLayout linearLayout;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getCurrentTheme());
        setContentView(R.layout.activity_bookmarks);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.bookmarks);
        mToolbar.setNavigationIcon(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("night", false)
                ? R.drawable.ic_arrow_back_white_24dp : R.drawable.ic_arrow_back_black_24dp);
        mToolbar.setNavigationOnClickListener(v -> finish());

        listView = findViewById(R.id.listView);
        linearLayout = findViewById(R.id.emptyList);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        mSwipeRefreshLayout = findViewById(R.id.refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(() -> new LoadBookmarks().execute());

        new LoadBookmarks().execute();
        listView.setOnItemClickListener((parent, view, position, id) -> {

            Object o = listView.getAdapter().getItem(position);
            if (o instanceof Map) {
                Map map = (Map) o;
                Intent intent = new Intent(this, WebActivity.class);
                intent.putExtra("page_url", String.valueOf(map.get(TAG_LINK)));
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Object o = listView.getAdapter().getItem(i);
            if (o instanceof Map) {
                Map map = (Map) o;
                deleteBookmark(String.valueOf(map.get(TAG_TITLE)), String.valueOf(map.get(TAG_LINK)));
            }

            return true;
        });

    }

    private class LoadBookmarks extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            runOnUiThread(() -> {

                SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
                String jsonLink = sharedPreferences.getString(WEB_LINKS, null);
                String jsonTitle = sharedPreferences.getString(WEB_TITLE, null);
                listRowData = new ArrayList<>();

                if (jsonLink != null && jsonTitle != null) {

                    Gson gson = new Gson();
                    ArrayList<String> linkArray = gson.fromJson(jsonLink, new TypeToken<ArrayList<String>>() {
                    }.getType());

                    ArrayList<String> titleArray = gson.fromJson(jsonTitle, new TypeToken<ArrayList<String>>() {
                    }.getType());


                    for (int i = 0; i < linkArray.size(); i++) {
                        HashMap<String, String> map = new HashMap<>();

                        if (titleArray.get(i).length() == 0)
                            map.put(TAG_TITLE, getString(R.string.bookmark) + (i + 1));
                        else
                            map.put(TAG_TITLE, titleArray.get(i));

                        map.put(TAG_LINK, linkArray.get(i));
                        listRowData.add(map);
                    }

                    adapter = new SimpleAdapter(BookmarksActivity.this,
                            listRowData, R.layout.bookmark_list_row,
                            new String[]{TAG_TITLE, TAG_LINK},
                            new int[]{R.id.title, R.id.link});

                    listView.setAdapter(adapter);
                }

                linearLayout.setVisibility(View.VISIBLE);
                listView.setEmptyView(linearLayout);


            });
            return null;
        }

        protected void onPostExecute(String args) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    private void deleteBookmark(final String title, final String link) {

        new MaterialAlertDialogBuilder(BookmarksActivity.this, R.style.AlertDialogTheme)
                .setTitle(R.string.delete_t)
                .setMessage(R.string.delete_s)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
                    String jsonLink = sharedPreferences.getString(WEB_LINKS, null);
                    String jsonTitle = sharedPreferences.getString(WEB_TITLE, null);


                    if (jsonLink != null && jsonTitle != null) {


                        Gson gson = new Gson();
                        ArrayList<String> linkArray = gson.fromJson(jsonLink, new TypeToken<ArrayList<String>>() {
                        }.getType());

                        ArrayList<String> titleArray = gson.fromJson(jsonTitle, new TypeToken<ArrayList<String>>() {
                        }.getType());


                        linkArray.remove(link);
                        titleArray.remove(title);


                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(WEB_LINKS, new Gson().toJson(linkArray));
                        editor.putString(WEB_TITLE, new Gson().toJson(titleArray));
                        editor.apply();

                        new LoadBookmarks().execute();
                    }
                    dialogInterface.dismiss();
                })
                .setNeutralButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss()).show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}