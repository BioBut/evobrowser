package com.evo.browser.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cocosw.bottomsheet.BottomSheet;
import com.evo.browser.R;
import com.evo.browser.utils.ThemeUtils;
import com.evo.browser.view.CenteredToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebActivity extends AppCompatActivity {

    private WebView mWeb;
    private CenteredToolbar mToolbar;
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private static final String TAG = WebActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;
    SwipeRefreshLayout swipeRefreshLayout;
    ProgressBar progressBar;

    // Код связанный с вызовом Premissions

    public static boolean hasPermission(Context context, String... permissions)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context!=null && permissions!=null)
        {
            for(String permission : permissions)
            {
                if(ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){

        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;

            if (resultCode== Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return;
                    }
                    if (intent == null) {

                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    // Настройка WebView клиента

    private class WebViewer extends WebViewClient {

        private String currentUrl;

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            // Открытие ссылок в Google Play

            String partialUrl = "/store/apps/details?id=";
            if (url.contains(partialUrl)) {
                int pos = url.indexOf(partialUrl) + partialUrl.length();
                String appId = url.substring(pos);

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + appId));
                    WebActivity.this.startActivity(intent);
                    return true;

                } catch (ActivityNotFoundException e) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    WebActivity.this.startActivity(intent);
                    return true;
                }
            }

            // Настройка открытия ссылок в приложениях (почта, телефон, сообщения, WhatsApp)

                if (url.contains("geo:")) {
                    Uri gmmIntentUri = Uri.parse(url);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                    return true;
                }

                if (url.contains("https://www.google.com/maps/")) {
                    Uri IntentUri = Uri.parse(url);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, IntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                    return true;
                }

                if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                }

                if (url.startsWith("tel:")) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                } else {
                    view.loadUrl(url);
                }
                if (url.startsWith("sms:")) {
                     handleSMSLink(url);
                     return true;
                }
                return true;
            }

        // Некий фикс неодекватного поведения SwipeRefreshLayout

        @Override
        public void onPageFinished(WebView view, String url) {
            swipeRefreshLayout.setRefreshing(false);
            currentUrl = url;
            super.onPageFinished(view, url);
            WebActivity.this.mToolbar.setTitle(view.getTitle());
        }

        // Показ диалога об ошибке загрузки страницы (например, если Вы отправляете запрос, но при этом отсутсвует интернет соединение, то сработает этот код и покажет диалог)

        public void onReceivedError(WebView mWeb, int errorCode, String description, String failingUrl) {
            try {
                mWeb.stopLoading();
            } catch (Exception e) {
            }

            if (mWeb.canGoBack()) {
                mWeb.goBack();
            }

            new MaterialAlertDialogBuilder(WebActivity.this, R.style.AlertDialogTheme)
                    .setTitle(R.string.error_t)
                    .setMessage(R.string.error_s)
                    .setPositiveButton(R.string.reload, (dialogInterface, i) -> mWeb.reload())
                    .show();
            super.onReceivedError(mWeb, errorCode, description, failingUrl);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Настройка для переключения тем
        setTheme(ThemeUtils.getCurrentTheme());
        setContentView(R.layout.activity_web);
        // Настройка ToolBar
        mToolbar = findViewById(R.id.toolbar);
        // Меню ToolBar
        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.getMenu().findItem(R.id.back).setOnMenuItemClickListener(item -> {
            if(mWeb.canGoBack()){
                mWeb.goBack();
            }
            return false;
        });
        mToolbar.getMenu().findItem(R.id.forward).setOnMenuItemClickListener(item -> {
            if(mWeb.canGoForward()){
                mWeb.goForward();
            }
            return false;
        });
        mToolbar.getMenu().findItem(R.id.copy_link).setOnMenuItemClickListener(item -> {
            mWeb.findViewById(R.id.web);
            String url = mWeb.getUrl();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.copy, Toast.LENGTH_SHORT).show();
            return true;
        });
        mToolbar.getMenu().findItem(R.id.setting).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return false;
        });
        mToolbar.setNavigationIcon(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("night", false)
                ? R.drawable.ic_arrow_back_white_24dp : R.drawable.ic_arrow_back_black_24dp);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mWeb = findViewById(R.id.web);
        progressBar = findViewById(R.id.progressBar);
        registerForContextMenu(mWeb);
        mWeb.getSettings().setJavaScriptEnabled(true);
        mWeb.getSettings().setLoadsImagesAutomatically(true);
        mWeb.getSettings().setSupportZoom(false);
        mWeb.getSettings().setBuiltInZoomControls(false);
        mWeb.getSettings().setDisplayZoomControls(false);
        mWeb.getSettings().setLoadWithOverviewMode(true);
        mWeb.getSettings().setUseWideViewPort(true);
        mWeb.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWeb.setFocusable(true);
        mWeb.setFocusableInTouchMode(true);
        mWeb.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWeb.setBackgroundColor(Color.TRANSPARENT);
        mWeb.clearCache(false);
        mWeb.getSettings().setLoadWithOverviewMode(true);
        mWeb.getSettings().setUseWideViewPort(true);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        // Запрос разрешений для Камеры, Местоположения, Микрофона, Памяти

        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(WebActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO}, 1);
        }

        // WebView клиенет

        mWeb.setWebViewClient(new WebViewer());

        // WebChromeClient и его настройка

        mWeb.setWebChromeClient(new WebChromeClient() {

            // Реализация просмотра видео-контента в полноэкранном режиме

            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            public Bitmap getDefaultVideoPoster()
            {
                if (mCustomView == null) {
                    return null;
                }
                return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
            }

            public void onHideCustomView()
            {
                ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

            public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback)
            {
                if (this.mCustomView != null)
                {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout)getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(3846);
            }

            // Настройка ProgressBar

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);

                if (newProgress==100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            // Загрузка файлов на сайты

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(WebActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, R.string.img_chooser);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }

            private File createImageFile() throws IOException {
                @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "img_" + timeStamp + "_";
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                return File.createTempFile(imageFileName, ".jpg", storageDir);
            }

            // Доступ к геолокации

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

        });

        // Метод для SwipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener(() -> mWeb.reload());

        // Настройки WebView

        CookieManager.getInstance().setAcceptCookie(true);
        WebSettings webset = mWeb.getSettings();
        Intent intent = getIntent();
        String search_bar = intent.getStringExtra("text");
        webset.setJavaScriptEnabled(true);
        webset.setAllowFileAccess(true);
        webset.setBuiltInZoomControls(false);
        webset.setSupportZoom(true);
        webset.setUseWideViewPort(false);
        webset.setDomStorageEnabled(true);
        webset.setAllowFileAccess(true);

        // Передача введённых данных для поиска + открытие ссылок в приложении

        mWeb.loadUrl("https://google.com/search?q=" + search_bar);
        String loadurl = getIntent().getDataString();
        if (loadurl != null) {
            mWeb.loadUrl(loadurl);
        } else {
            mWeb.loadUrl("https://");
        }

        // Реализация скачивания файлов

        mWeb.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), R.string.download_toast, Toast.LENGTH_LONG).show();
        });

    }

    // Вызов контекстного меню (Long press)

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult wavierHottestResult = mWeb.getHitTestResult();
        final String DownloadImageUrl = wavierHottestResult.getExtra();
        if(wavierHottestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                wavierHottestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
        {
            if(URLUtil.isNetworkUrl(DownloadImageUrl))
            {

                new BottomSheet.Builder(this, ThemeUtils.getCurrentBottomTheme()).sheet(R.menu.menu_press).listener((dialog, which) -> {
                    switch (which) {
                        case R.id.copy_link_url:
                            String copyimageurl = wavierHottestResult.getExtra();
                            ClipboardManager manager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("label",copyimageurl);
                            manager.setPrimaryClip(clip);
                            Toast.makeText(WebActivity.this, R.string.copy, Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.send_img:
                            Picasso.get().load(DownloadImageUrl).into(new Target() {
                                @Override
                                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                    Intent i = new Intent(Intent.ACTION_SEND);
                                    i.setType("image/*");
                                    i.putExtra(Intent.EXTRA_STREAM, geologicalBitmaUri(bitmap));
                                    startActivity(Intent.createChooser(i,getString(R.string.send_image)));
                                }
                                @Override
                                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                                }
                                @Override
                                public void onPrepareLoad(Drawable placeHolderDrawable) {
                                }
                            });
                            break;
                        case R.id.download_img:
                            int Permission_all = 1;
                            String Permission[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
                            if(!hasPermission(WebActivity.this,Permission))
                            {
                                ActivityCompat.requestPermissions(WebActivity.this,Permission,Permission_all);
                            }
                            else
                            {
                                String filename = "";
                                String type = null;
                                String Mimetype = MimeTypeMap.getFileExtensionFromUrl(DownloadImageUrl);
                                filename = URLUtil.guessFileName(DownloadImageUrl,DownloadImageUrl,Mimetype);
                                if(Mimetype!=null)
                                {
                                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Mimetype);
                                }
                                if(type==null)
                                {
                                    filename = filename.replace(filename.substring(filename.lastIndexOf(".")),".png");
                                    type = "image/*";
                                }
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageUrl));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename);
                                DownloadManager managers = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                                managers.enqueue(request);
                                Toast.makeText(WebActivity.this, R.string.download, Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }).show();

        }
        }
    }

    public Uri geologicalBitmaUri(Bitmap bmp)
    {
        Uri bmpuri = null;
        try{
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"scrimmage"+ System.currentTimeMillis()+".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG,90,out);
            out.close();
            bmpuri = FileProvider.getUriForFile(getApplicationContext(),"com.evo.browser.provider",file);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return bmpuri;
    }

    protected void handleSMSLink(String url) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        String phoneNumber = url.split("[:?]")[1];

        if (!TextUtils.isEmpty(phoneNumber)){
            intent.setData(Uri.parse("smsto:" + phoneNumber));

        } else {
            intent.setData(Uri.parse("smsto:"));
        }

        if (url.contains("body=")) {
            String smsBody = url.split("body=")[1];

            try {
                smsBody = URLDecoder.decode(smsBody,"UTF-8");
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }

            if (!TextUtils.isEmpty(smsBody)){
                intent.putExtra("sms_body",smsBody);
            }
        }

        if (intent.resolveActivity(getPackageManager())!=null) {
            startActivity(intent);
        } else {
            Toast.makeText(mContext, R.string.sms_no,Toast.LENGTH_SHORT).show();
        }
    }

    // Настройки браузера

    @Override
    protected void onResume() {
        super.onResume();

        Boolean orientation = mSharedPreferences.getBoolean(getString(R.string.orientation_t), false);
        Boolean statusbar = mSharedPreferences.getBoolean(getString(R.string.status_t), false);
        Boolean screen = mSharedPreferences.getBoolean(getString(R.string.screen_t), false);
        Boolean pc_mode = mSharedPreferences.getBoolean(getString(R.string.pc_t), false);
        Boolean incognito = mSharedPreferences.getBoolean(getString(R.string.incognito_t), false);
        Boolean geo = mSharedPreferences.getBoolean(getString(R.string.geo_t), true);
        Boolean password = mSharedPreferences.getBoolean(getString(R.string.pass_t), false);
        Boolean textsize = mSharedPreferences.getBoolean(getString(R.string.font_t), false);
        Boolean zoom = mSharedPreferences.getBoolean(getString(R.string.zoom_t), false);
        Boolean supportJavaScript = mSharedPreferences.getBoolean(getString(R.string.js_t), true);
        Boolean cookies = mSharedPreferences.getBoolean(getString(R.string.cookie_t), true);

        // Настройка режима "Отключить автоповорт"

        if (orientation) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        // Настройка режима "Строка состояния"

        if (statusbar) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Настройка режима "Не отключать дисплей"

        if (screen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Настройка режима "Полная версия сайта"

        if (pc_mode) {
            mWeb.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Safari/602.1.50");
        } else {
            mWeb.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.65 Mobile Safari/537.36");
        }

        // Настройка режима "Инкогнито"

        if (incognito) {
            mWeb.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            mWeb.getSettings().setAppCacheEnabled(false);
            mWeb.clearHistory();
            mWeb.clearCache(true);
            mWeb.clearFormData();
            mWeb.getSettings().setSavePassword(false);
            mWeb.getSettings().setSaveFormData(false);

        } else {
            mWeb.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            mWeb.getSettings().setAppCacheEnabled(true);
            mWeb.clearCache(false);
            mWeb.getSettings().setSavePassword(true);
            mWeb.getSettings().setSaveFormData(true);
        }

        // Отключение доступа к местоположению

        if (geo) {
            mWeb.getSettings().setGeolocationEnabled(true);
        } else {
            mWeb.getSettings().setGeolocationEnabled(false);
        }

        // Настройка режима "Не сохранять пароли"

        if (password) {
            mWeb.getSettings().setSavePassword(false);
        } else {
            mWeb.getSettings().setSavePassword(true);
        }

        // Настройка размера текста

        if (textsize) {
            WebSettings webset = mWeb.getSettings();
            webset.setTextSize(TextSize.LARGER);
        } else {
            WebSettings webset = mWeb.getSettings();
            webset.setTextSize(TextSize.NORMAL);
        }

        // Настройка зума веб-страниц

        if (zoom) {
            mWeb.getSettings().setSupportZoom(true);
            mWeb.getSettings().setBuiltInZoomControls(true);
            mWeb.getSettings().setDisplayZoomControls(false);
        } else {
            mWeb.getSettings().setSupportZoom(false);
            mWeb.getSettings().setBuiltInZoomControls(false);
            mWeb.getSettings().setDisplayZoomControls(false);
        }

        // Настройка Cookie

        if (cookies) {
            CookieManager.getInstance().setAcceptCookie(true);
        } else {
            CookieManager.getInstance().setAcceptCookie(false);
        }

        // Настройка JavaScript

        mWeb.getSettings().setJavaScriptEnabled(supportJavaScript);
        mWeb.getSettings().setJavaScriptCanOpenWindowsAutomatically(supportJavaScript);

    }

    // Перезагрузка страницы

    public void reload(View view)
    {
        mWeb.reload();
    }

    // Обработка нажатия кнопки "Назад" + диалоговое окно с потдверждением выхода

    @Override
    public void onBackPressed() {
        if (mWeb.canGoBack()) {
            mWeb.goBack();
        } else {
            new MaterialAlertDialogBuilder(WebActivity.this, R.style.AlertDialogTheme)
                    .setTitle(R.string.exit_t)
                    .setMessage(R.string.exit_s)
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> finish())
                    .setNeutralButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();
        }
    }

    // Анимации

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}