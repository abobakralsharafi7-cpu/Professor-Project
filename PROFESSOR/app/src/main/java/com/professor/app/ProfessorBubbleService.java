package com.professor.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfessorBubbleService extends Service {
    public static final String ACTION_SHOW_BUBBLE = "com.professor.app.SHOW_BUBBLE";
    public static final String ACTION_OPEN_PANEL = "com.professor.app.OPEN_PANEL";
    public static final String ACTION_HIDE_BUBBLE = "com.professor.app.HIDE_BUBBLE";

    private static final String CHANNEL_ID = "professor_floating_channel";
    private static final int NOTIFICATION_ID = 7301;

    private WindowManager windowManager;
    private View bubbleView;
    private View panelView;
    private View closeView; 
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams closeParams;

    public static WebView sharedWebView;
    public static android.content.MutableContextWrapper contextWrapper;
    public static android.webkit.ValueCallback<Uri[]> filePathCallback;

    public static WebView getSharedWebView(Context context) {
        if (contextWrapper == null) {
            contextWrapper = new android.content.MutableContextWrapper(context);
        } else {
            contextWrapper.setBaseContext(context);
        }

        if (sharedWebView == null) {
            sharedWebView = new WebView(contextWrapper);
            WebSettings settings = sharedWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true); 
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            
            sharedWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("tg:")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { contextWrapper.getBaseContext().startActivity(intent); } catch (Exception e) {}
                        return true;
                    }
                    return false;
                }
            });

            sharedWebView.addJavascriptInterface(new ProfessorTradeStore(contextWrapper.getBaseContext()), "TradeStore");

            sharedWebView.addJavascriptInterface(new Object() {
                @android.webkit.JavascriptInterface
                public void saveBase64File(String base64Data, String filename, String mimeType) {
                    new Thread(() -> {
                        try {
                            String cleanBase64 = base64Data.replaceAll("\\s+", "");
                            byte[] bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
                            
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename);
                            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
                            }

                            Uri uri = contextWrapper.getBaseContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                            if (uri != null) {
                                java.io.OutputStream os = contextWrapper.getBaseContext().getContentResolver().openOutputStream(uri);
                                if (os != null) {
                                    os.write(bytes);
                                    os.flush();
                                    os.close();
                                }
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                                    android.widget.Toast.makeText(contextWrapper.getBaseContext(), "✅ تم الحفظ في التنزيلات بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                );
                            } else {
                                java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                                java.io.File file = new java.io.File(path, filename);
                                java.io.FileOutputStream os = new java.io.FileOutputStream(file, false);
                                os.write(bytes);
                                os.flush();
                                os.close();
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                                    android.widget.Toast.makeText(contextWrapper.getBaseContext(), "✅ تم الحفظ في التنزيلات بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                );
                            }
                        } catch (Exception e) { 
                            e.printStackTrace(); 
                        }
                    }).start();
                }

                @android.webkit.JavascriptInterface
                public void saveImage(String base64Data, String filename) {
                    new Thread(() -> {
                        try {
                            String cleanBase64 = base64Data.replaceAll("\\s+", "");
                            byte[] bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
                            
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename);
                            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES);
                            }

                            Uri uri = contextWrapper.getBaseContext().getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                            if (uri != null) {
                                java.io.OutputStream os = contextWrapper.getBaseContext().getContentResolver().openOutputStream(uri);
                                if (os != null) { os.write(bytes); os.flush(); os.close(); }
                            } else {
                                java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                                java.io.File file = new java.io.File(path, filename);
                                java.io.FileOutputStream os = new java.io.FileOutputStream(file, false);
                                os.write(bytes); os.flush(); os.close();
                            }
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                                android.widget.Toast.makeText(contextWrapper.getBaseContext(), "✅ تم حفظ الصورة في التنزيلات بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                            );
                        } catch (Exception e) { 
                            e.printStackTrace(); 
                        }
                    }).start();
                }

                @android.webkit.JavascriptInterface
                public void print() {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        android.print.PrintManager printManager = (android.print.PrintManager) contextWrapper.getBaseContext().getSystemService(Context.PRINT_SERVICE);
                        if (printManager != null && sharedWebView != null) {
                            android.print.PrintDocumentAdapter printAdapter = sharedWebView.createPrintDocumentAdapter("Professor_Report");
                            printManager.print("Professor_Report", printAdapter, new android.print.PrintAttributes.Builder().build());
                        }
                    });
                }
            }, "Android");

            sharedWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                    if (filePathCallback != null) { filePathCallback.onReceiveValue(null); }
                    filePathCallback = callback;
                    Intent intent = new Intent(contextWrapper.getBaseContext(), FileChooserActivity.class);
                    if (params.getAcceptTypes() != null && params.getAcceptTypes().length > 0) {
                        intent.putExtra("ACCEPT_TYPES", params.getAcceptTypes());
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    contextWrapper.getBaseContext().startActivity(intent);
                    return true;
                }
            });
            sharedWebView.loadUrl("file:///android_asset/professor.html");
        }
        return sharedWebView;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_SHOW_BUBBLE;
        
        if (ACTION_HIDE_BUBBLE.equals(action)) {
            if (bubbleView != null) bubbleView.setVisibility(View.GONE);
            if (panelView != null) { removeViewSafely(panelView); panelView = null; }
            return START_STICKY;
        }

        if (bubbleView == null) { showBubble(); } else { bubbleView.setVisibility(View.VISIBLE); }
        if (ACTION_OPEN_PANEL.equals(action)) { showPanel(); }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        hidePanel();
        if (sharedWebView != null) {
            if (sharedWebView.getParent() != null) { ((ViewGroup) sharedWebView.getParent()).removeView(sharedWebView); }
            sharedWebView.stopLoading(); sharedWebView.destroy(); sharedWebView = null;
        }
        contextWrapper = null;
        removeViewSafely(bubbleView); removeViewSafely(closeView); bubbleView = null;
        super.onDestroy();
    }

    private void showBubble() {
        bubbleView = buildBubbleView();
        closeView = buildCloseView();
        closeParams = new WindowManager.LayoutParams(dp(60), dp(60), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
        closeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; closeParams.y = dp(50);
        closeView.setVisibility(View.GONE); windowManager.addView(closeView, closeParams);

        bubbleParams = new WindowManager.LayoutParams(dp(72), dp(72), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START; bubbleParams.x = dp(18); bubbleParams.y = dp(140);
        windowManager.addView(bubbleView, bubbleParams);
    }

    private View buildBubbleView() {
        FrameLayout frame = new FrameLayout(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            frame.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); } });
            frame.setClipToOutline(true);
        }
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.professor_logo); logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(logo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setOnTouchListener(new BubbleTouchListener()); return frame;
    }

    private View buildCloseView() {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(); bg.setShape(GradientDrawable.OVAL); bg.setColor(Color.argb(200, 255, 0, 0)); frame.setBackground(bg);
        TextView xText = new TextView(this); xText.setText("X"); xText.setTextColor(Color.WHITE); xText.setTextSize(24); xText.setTypeface(Typeface.DEFAULT_BOLD); xText.setGravity(Gravity.CENTER);
        frame.addView(xText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return frame;
    }

    // 🟢 تم تعديل دالة إظهار النافذة العائمة لدعم "اللمس في الخلفية للتصغير"
    private void showPanel() {
        if (bubbleView != null) { bubbleView.setVisibility(View.GONE); }
        if (panelView == null) {
            panelView = buildPanelView();
            // تم جعل الخلفية تملأ الشاشة لالتقاط اللمسة بأمان
            panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                0,
                PixelFormat.TRANSLUCENT
            );
            panelParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            panelParams.windowAnimations = android.R.style.Animation_Dialog;
            windowManager.addView(panelView, panelParams);
        } else { 
            try { windowManager.addView(panelView, panelParams); } catch (Exception ignored) {} 
        }
    }

    // 🟢 تم برمجة هيكل النافذة ليحتوي على طبقة شفافة تلتقط لمسات الأصابع وزر الرجوع
    private View buildPanelView() {
        FrameLayout overlay = new FrameLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                // يعترض زر الرجوع الفعلي في الهاتف ويخفي النافذة فقط
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    hidePanel();
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
        };
        // التقاط اللمسات في المساحة الفارغة خارج إطار التطبيق
        overlay.setOnClickListener(v -> hidePanel());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(panelBackground());
        root.setClickable(true); // لكي لا تختفي النافذة عند الضغط بداخلها
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);

        Point screen = screenSize();
        int width = Math.max(dp(300), screen.x - dp(24));
        int height = Math.max(dp(420), screen.y - dp(120));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(width, height);
        rootParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        rootParams.topMargin = dp(54); // المسافة من أعلى الشاشة

        LinearLayout topBar = new LinearLayout(this); 
        topBar.setOrientation(LinearLayout.HORIZONTAL); 
        topBar.setGravity(Gravity.CENTER_VERTICAL); 
        topBar.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable topBarBg = new GradientDrawable(); 
        topBarBg.setColor(Color.rgb(8, 8, 8)); 
        topBarBg.setCornerRadii(new float[]{dp(18), dp(18), dp(18), dp(18), 0, 0, 0, 0}); 
        topBar.setBackground(topBarBg);

        TextView title = new TextView(this); title.setText("PROFESSOR ⚜️"); title.setTextColor(Color.rgb(255, 215, 0)); title.setTextSize(16); title.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button minimizeBtn = new Button(this); minimizeBtn.setText("✕ تصغير"); minimizeBtn.setTextColor(Color.WHITE); minimizeBtn.setTextSize(11); minimizeBtn.setTypeface(Typeface.DEFAULT_BOLD); minimizeBtn.setAllCaps(false);
        GradientDrawable minBg = new GradientDrawable(); minBg.setColor(Color.argb(45, 255, 255, 255)); minBg.setCornerRadius(dp(8)); minimizeBtn.setBackground(minBg); minimizeBtn.setPadding(dp(10), 0, dp(10), 0); minimizeBtn.setOnClickListener(v -> hidePanel());
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)); minParams.setMargins(0, 0, dp(8), 0); topBar.addView(minimizeBtn, minParams);

        Button maximizeBtn = new Button(this); maximizeBtn.setText("⛶ تكبير"); maximizeBtn.setTextColor(Color.BLACK); maximizeBtn.setTextSize(11); maximizeBtn.setTypeface(Typeface.DEFAULT_BOLD); maximizeBtn.setBackground(goldButtonBackground()); maximizeBtn.setPadding(dp(10), 0, dp(10), 0);
        maximizeBtn.setOnClickListener(v -> { hidePanel(); Intent intent = new Intent(ProfessorBubbleService.this, MainActivity.class); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent); });
        topBar.addView(maximizeBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));

        root.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        WebView webView = getSharedWebView(this);
        if (webView.getParent() != null) { ((ViewGroup) webView.getParent()).removeView(webView); }
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // دمج التطبيق فوق الطبقة الشفافة
        overlay.addView(root, rootParams);

        return overlay;
    }

    private void hidePanel() {
        if (panelView != null) { removeViewSafely(panelView); panelView = null; }
        if (bubbleView != null) { bubbleView.setVisibility(View.VISIBLE); }
    }

    private Notification createNotification() {
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("PROFESSOR").setContentText("الزر العائم يعمل الآن").setSmallIcon(R.drawable.ic_notification).setContentIntent(pendingIntent).setOngoing(true).build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PROFESSOR Floating Button", NotificationManager.IMPORTANCE_LOW); manager.createNotificationChannel(channel);
            }
        }
    }

    private GradientDrawable panelBackground() { GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(10, 10, 10)); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), Color.rgb(255, 215, 0)); return bg; }
    private GradientDrawable goldButtonBackground() { GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(255, 215, 0)); bg.setCornerRadius(dp(8)); return bg; }
    private int overlayType() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private Point screenSize() { Point p = new Point(); windowManager.getDefaultDisplay().getSize(p); return p; }
    private void removeViewSafely(View view) { try { if (view != null) windowManager.removeView(view); } catch (Exception ignored) {} }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private class BubbleTouchListener implements View.OnTouchListener {
        private int initialX, initialY; private float initialTouchX, initialTouchY; private long downTime; private boolean isDragging = false;
        @Override public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: initialX = bubbleParams.x; initialY = bubbleParams.y; initialTouchX = event.getRawX(); initialTouchY = event.getRawY(); downTime = System.currentTimeMillis(); isDragging = false; return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX; float dy = event.getRawY() - initialTouchY;
                    if (!isDragging && (Math.abs(dx) > dp(10) || Math.abs(dy) > dp(10))) { isDragging = true; if (closeView != null) closeView.setVisibility(View.VISIBLE); }
                    bubbleParams.x = initialX + (int) dx; bubbleParams.y = initialY + (int) dy; windowManager.updateViewLayout(bubbleView, bubbleParams);
                    if (isDragging && isOverCloseArea(event.getRawX(), event.getRawY())) { closeView.setScaleX(1.3f); closeView.setScaleY(1.3f); } else if (closeView != null) { closeView.setScaleX(1.0f); closeView.setScaleY(1.0f); }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (closeView != null) { closeView.setVisibility(View.GONE); closeView.setScaleX(1.0f); closeView.setScaleY(1.0f); }
                    if (isDragging && isOverCloseArea(event.getRawX(), event.getRawY())) { stopSelf(); return true; }
                    float movedX = Math.abs(event.getRawX() - initialTouchX); float movedY = Math.abs(event.getRawY() - initialTouchY); long elapsed = System.currentTimeMillis() - downTime;
                    if (movedX < dp(15) && movedY < dp(15) && elapsed < 350) { showPanel(); } return true;
                default: return false;
            }
        }
        private boolean isOverCloseArea(float x, float y) { Point screen = screenSize(); int closeAreaY = screen.y - dp(150); int closeAreaXStart = (screen.x / 2) - dp(80); int closeAreaXEnd = (screen.x / 2) + dp(80); return y > closeAreaY && x > closeAreaXStart && x < closeAreaXEnd; }
    }
}
