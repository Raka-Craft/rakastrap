package com.raka.rakastrap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String ROBLOX_PKG = "com.roblox.client";
    private static final int REQ_SAVE_JSON = 77;
    private static final int REQ_IMPORT_JSON = 78;

    // Flag terverifikasi dari daftar resmi Roblox / pack hasil audit.
    static final String F_MSAA = "FIntDebugForceMSAASamples";
    static final String F_FRM = "DFIntDebugFRMQualityLevelOverride";
    static final String F_FPS = "DFIntTaskSchedulerTargetFps";
    static final String F_PREF_VK = "FFlagDebugGraphicsPreferVulkan";
    static final String F_PREF_GL = "FFlagDebugGraphicsPreferOpenGL";
    static final String F_DIS_VK = "FFlagDebugGraphicsDisableVulkan";
    static final String F_DIS_GL = "FFlagDebugGraphicsDisableOpenGL";
    static final String F_POSTFX = "FFlagDisablePostFx";
    static final String F_GRASS_MIN = "FIntFRMMinGrassDistance";
    static final String F_GRASS_MAX = "FIntFRMMaxGrassDistance";
    static final String F_SHADOW = "FIntRenderShadowIntensity";
    static final String F_SKYGRAY = "FFlagDebugSkyGray";
    static final String F_DISPFPS = "FFlagDebugDisplayFPS";
    static final String F_TEX_OTA = "FIntTerrainOTAMaxTextureSize";
    static final String F_TEX_UI = "FIntUITextureMaxRenderTextureSize";
    static final String F_LOD = "DFIntCSGLevelOfDetailSwitchingDistance";
    static final String F_MESH_ADAPT = "FFlagAdaptiveMeshCacheSizeForAndroid";
    static final String F_PRELOAD1 = "DFFlagEnablePreloading";
    static final String F_PRELOAD2 = "DFFlagEnableMeshPreloading2";
    static final String F_ANIMCACHE = "FFlagAnimationClipMemCacheEnabled";
    static final String F_GOM_MIN = "DFIntGraphicsOptimizationModeMinFrameTimeTargetMs";
    static final String F_GOM_MAX = "DFIntGraphicsOptimizationModeMaxFrameTimeTargetMs";

    private SharedPreferences prefs;
    private LinearLayout pages;
    private Button[] navBtns = new Button[5];
    private TextView robloxStatusView, shizukuStatusView;
    private TextView homePackInfo;
    private String flagTier = "vip";
    private FlagAdapter flagAdapter;
    private EditText flagSearch;
    private TextView flagCountView;
    private TextView resStateView;
    private View grafRoot;
    private TextView grafEngineInfo, grafFpsInfo;
    private View presetRoot;
    private Switch gmPowerSw, gmBattSw;
    private LinearLayout resBanner;
    private PowerManager.WakeLock cpuLock, displayLock;
    private WifiManager.WifiLock wifiLock;
    private final Handler handler = new Handler();

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("rakastrap", MODE_PRIVATE);
        if (!prefs.contains("apply_opt")) prefs.edit().putBoolean("apply_opt", true).apply();
        flagTier = prefs.getString("active_pack", "vip");
        if (!flagTier.equals("basic") && !flagTier.equals("premium") && !flagTier.equals("vip")) flagTier = "vip";

        FrameLayout root = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.bg_anime);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(178, 8, 6, 16));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(14), dp(14), dp(14), dp(10));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("RakaStrap");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        main.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Launcher Roblox + Pack FastFlag punya Raka. Semua fitur gratis, tanpa key, tanpa iklan.");
        sub.setTextSize(13);
        sub.setTextColor(Color.rgb(206, 188, 255));
        main.addView(sub);
        robloxStatusView = statusLine();
        main.addView(robloxStatusView);
        shizukuStatusView = statusLine();
        main.addView(shizukuStatusView);

        resBanner = new LinearLayout(this);
        resBanner.setOrientation(LinearLayout.HORIZONTAL);
        resBanner.setGravity(Gravity.CENTER_VERTICAL);
        resBanner.setPadding(dp(10), dp(6), dp(6), dp(6));
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(Color.rgb(127, 29, 29));
        bgd.setCornerRadius(dp(10));
        resBanner.setBackground(bgd);
        LinearLayout.LayoutParams blp0 = new LinearLayout.LayoutParams(-1, -2);
        blp0.topMargin = dp(6);
        resBanner.setLayoutParams(blp0);
        TextView bt = new TextView(this);
        bt.setText("Resolusi HP sedang diturunkan oleh RakaStrap.");
        bt.setTextColor(Color.WHITE);
        bt.setTextSize(12.5f);
        resBanner.addView(bt, new LinearLayout.LayoutParams(0, -2, 1));
        Button bb = bigButton("KEMBALIKAN");
        bb.setTextSize(12.5f);
        bb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { restoreResolution(); } });
        resBanner.addView(bb, new LinearLayout.LayoutParams(dp(124), dp(42)));
        resBanner.setVisibility(View.GONE);
        main.addView(resBanner);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, dp(10), 0, dp(8));
        String[] labels = {"Beranda", "FastFlag", "Grafis", "Game Mode", "Preset"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Button b = bigButton(labels[i]);
            b.setTextSize(12.5f);
            b.setPadding(dp(2), dp(8), dp(2), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1);
            if (i > 0) lp.leftMargin = dp(5);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(idx); } });
            navBtns[i] = b;
            nav.addView(b);
        }
        main.addView(nav);

        pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        main.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1));

        buildHome();
        buildFlags();
        buildGrafis();
        buildGameMode();
        buildPreset();
        showTab(0);
        try { rikka.shizuku.Shizuku.addRequestPermissionResultListener(permListener); } catch (Throwable t) { /* abaikan */ }
        refreshStatus();
    }

    private TextView statusLine() {
        TextView t = new TextView(this);
        t.setTextSize(12.5f);
        t.setTextColor(Color.rgb(255, 213, 109));
        t.setPadding(0, dp(2), 0, 0);
        return t;
    }

    private void refreshStatus() {
        boolean rb = false;
        try { rb = getPackageManager().getLaunchIntentForPackage(ROBLOX_PKG) != null; } catch (Throwable t) { /* abaikan */ }
        robloxStatusView.setText(rb ? "Roblox Support FastFlag: Terdeteksi (package com.roblox.client). Tombol MAIN menuju ke client ini." : "Roblox Support FastFlag: Tidak terdeteksi di HP ini. Pasang client MasterStrap dulu.");
        robloxStatusView.setTextColor(rb ? Color.rgb(109, 255, 143) : Color.rgb(255, 138, 128));
        new Thread(new Runnable() { public void run() {
            final boolean alive = Priv.shizukuAlive();
            final boolean granted = alive && Priv.shizukuGranted();
            runOnUiThread(new Runnable() { public void run() {
                if (granted) { shizukuStatusView.setText("Shizuku: Aktif. Fitur Game Mode bisa bekerja."); shizukuStatusView.setTextColor(Color.rgb(109, 255, 143)); }
                else if (alive) { shizukuStatusView.setText("Shizuku: Terdeteksi, izin belum diberikan. Minta izin di tab Game Mode."); shizukuStatusView.setTextColor(Color.rgb(255, 213, 109)); }
                else { shizukuStatusView.setText("Shizuku: Tidak aktif. Sebagian fitur Game Mode butuh Shizuku (atau root)."); shizukuStatusView.setTextColor(Color.rgb(255, 138, 128)); }
            } });
        } }).start();
    }

    private final rikka.shizuku.Shizuku.OnRequestPermissionResultListener permListener =
        new rikka.shizuku.Shizuku.OnRequestPermissionResultListener() {
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                Toast.makeText(MainActivity.this,
                    grantResult == PackageManager.PERMISSION_GRANTED ? "Izin Shizuku diberikan." : "Izin Shizuku ditolak.",
                    Toast.LENGTH_SHORT).show();
                refreshStatus();
            }
        };

    private void showTab(int idx) {
        for (int i = 0; i < pages.getChildCount(); i++) pages.getChildAt(i).setVisibility(i == idx ? View.VISIBLE : View.GONE);
        for (int i = 0; i < navBtns.length; i++) navBtns[i].setAlpha(i == idx ? 1f : 0.55f);
        if (idx == 0) updateHomePackInfo();
        if (idx == 2) refreshGrafis();
        if (idx == 4) refreshPreset();
    }

    // ---------- komponen tampilan ----------

    private Button bigButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(126, 34, 206), Color.rgb(220, 38, 38)});
        gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), Color.rgb(248, 113, 113));
        b.setBackground(gd);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        return b;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(215, 18, 14, 34));
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), Color.rgb(94, 74, 160));
        c.setBackground(gd);
        c.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView hText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, 0, 0, dp(4));
        return t;
    }

    private TextView infoText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(13);
        t.setTextColor(Color.rgb(216, 208, 240));
        t.setPadding(0, dp(2), 0, dp(6));
        return t;
    }

    private LinearLayout switchRow(final String key, boolean def, String title, String desc) {
        LinearLayout rowBox = new LinearLayout(this);
        rowBox.setOrientation(LinearLayout.VERTICAL);
        rowBox.setPadding(0, dp(4), 0, dp(4));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.WHITE);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        Switch s = new Switch(this);
        s.setChecked(prefs.getBoolean(key, def));
        s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) { prefs.edit().putBoolean(key, on).apply(); }
        });
        top.addView(s);
        rowBox.addView(top);
        rowBox.addView(infoText(desc));
        return rowBox;
    }

    // Saklar untuk fitur berbasis FastFlag: menyala = flag ditulis ke lapisan pengaturan.
    private LinearLayout settingSwitchRow(final Map<String, String> bundle, String title, String desc) {
        LinearLayout rowBox = new LinearLayout(this);
        rowBox.setOrientation(LinearLayout.VERTICAL);
        rowBox.setPadding(0, dp(4), 0, dp(4));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.WHITE);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        Switch s = new Switch(this);
        boolean on = !bundle.isEmpty();
        for (Map.Entry<String, String> e : bundle.entrySet()) {
            String cur = FlagStore.getSetting(prefs, e.getKey());
            if (!e.getValue().equals(cur)) { on = false; break; }
        }
        s.setChecked(on);
        s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                FlagStore.applyBundle(prefs, bundle, checked);
                Toast.makeText(MainActivity.this, "Pengaturan disimpan. Salin ulang JSON pack ke MasterStrap.", Toast.LENGTH_SHORT).show();
            }
        });
        top.addView(s);
        rowBox.addView(top);
        rowBox.addView(infoText(desc));
        return rowBox;
    }

    private static Map<String, String> bundle(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private String activeTier() { return prefs.getString("active_pack", "vip"); }

    // ---------- BERANDA ----------

    private void buildHome() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        sv.addView(box);

        LinearLayout c1 = card();
        c1.addView(hText("Pack FastFlag Aktif"));
        c1.addView(infoText("Pilih pack yang mau dipakai. Pack ini yang disalin / disimpan untuk dipasang di MasterStrap, dan yang dibuka di tab FastFlag."));
        final String[] tiers = {"basic", "premium", "vip"};
        final String[] names = {"Basic - 100 flag, paling ringan", "Premium - 250 flag, seimbang", "VIP - 670 flag, maksimal"};
        for (int i = 0; i < 3; i++) {
            final String tier = tiers[i];
            final Button rb = bigButton("");
            rb.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            rb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                prefs.edit().putString("active_pack", tier).apply();
                flagTier = tier;
                updateHomePackInfo();
                refreshPackButtons(box);
                refreshGrafis();
                Toast.makeText(MainActivity.this, "Pack aktif: " + FlagStore.tierName(tier), Toast.LENGTH_SHORT).show();
            } });
            rb.setTag("pack_" + tier);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
            lp.bottomMargin = dp(6);
            rb.setLayoutParams(lp);
            c1.addView(rb);
        }
        homePackInfo = infoText("");
        c1.addView(homePackInfo);
        box.addView(c1);

        Button play = bigButton("MAIN ROBLOX");
        play.setTextSize(18);
        play.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { launchRoblox(); } });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(58));
        plp.bottomMargin = dp(10);
        play.setLayoutParams(plp);
        box.addView(play);

        LinearLayout c2 = card();
        c2.addView(hText("Pasang FastFlag"));
        c2.addView(infoText("Roblox hanya membaca FastFlag lewat client MasterStrap, jadi pack dari sini dipasang lewat MasterStrap: 1) Salin JSON pack aktif, 2) buka MasterStrap lalu masuk FastFlag Editor, 3) Import JSON / tempel, 4) main dari MasterStrap atau tombol MAIN ROBLOX di sini. Semua pengaturan di tab Grafis dan Preset ikut masuk ke JSON ini selama saklar Terapkan Optimasi menyala."));
        Button copy = bigButton("Salin JSON Pack Aktif");
        copy.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String tier = activeTier();
            String json = FlagStore.buildJson(MainActivity.this, prefs, tier);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("FastFlag " + FlagStore.tierName(tier), json));
            Toast.makeText(MainActivity.this, "JSON " + FlagStore.tierName(tier) + " tersalin. Tempel di FastFlag Editor MasterStrap.", Toast.LENGTH_LONG).show();
        } });
        c2.addView(copy);
        Button save = bigButton("Simpan File JSON Pack Aktif");
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(8);
        save.setLayoutParams(slp);
        save.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE, FlagStore.tierName(activeTier()) + "_Fixlag_RakaStrap.json");
            startActivityForResult(i, REQ_SAVE_JSON);
        } });
        c2.addView(save);
        box.addView(c2);

        LinearLayout c3 = card();
        c3.addView(hText("Tentang"));
        c3.addView(infoText("RakaStrap versi 1.3, launcher dan pengelola pack FastFlag Roblox punya Raka (Basic, Premium, VIP hasil audit). Semua fitur gratis: di MasterStrap sebagian fitur ini terkunci Premium / butuh License Key, di sini tidak ada key dan tidak ada iklan. Aplikasi ini tidak mengubah Roblox dari dalam; client yang membaca FastFlag tetap MasterStrap."));
        box.addView(c3);

        pages.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        refreshPackButtons(box);
        updateHomePackInfo();
    }

    private void refreshPackButtons(View root) {
        String active = activeTier();
        String[] tiers = {"basic", "premium", "vip"};
        String[] names = {"Basic - 100 flag, paling ringan", "Premium - 250 flag, seimbang", "VIP - 670 flag, maksimal"};
        for (int i = 0; i < 3; i++) {
            View v = root.findViewWithTag("pack_" + tiers[i]);
            if (v instanceof Button) ((Button) v).setText((tiers[i].equals(active) ? "● " : "○ ") + names[i]);
        }
    }

    private void updateHomePackInfo() {
        if (homePackInfo == null) return;
        String tier = activeTier();
        int total = FlagStore.pack(this, tier).size();
        int ov = FlagStore.overrideCount(prefs, tier);
        int st = FlagStore.settings(prefs).length();
        homePackInfo.setText("Pack aktif sekarang: " + FlagStore.tierName(tier) + " (" + total + " flag"
            + (ov > 0 ? ", " + ov + " ubahan manual" : "")
            + (st > 0 && prefs.getBoolean("apply_opt", true) ? ", " + st + " pengaturan Grafis/Preset aktif" : "") + ").");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_SAVE_JSON) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(FlagStore.buildJson(this, prefs, activeTier()).getBytes("UTF-8"));
                os.close();
                Toast.makeText(this, "File JSON tersimpan.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Gagal menyimpan file.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_IMPORT_JSON) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                importJson(new String(bos.toByteArray(), "UTF-8"));
            } catch (Exception e) {
                Toast.makeText(this, "Gagal membaca file JSON.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importJson(String text) {
        try {
            JSONObject o = new JSONObject(text);
            int count = 0;
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = o.get(k);
                String val = (v instanceof Boolean) ? (((Boolean) v) ? "True" : "False") : String.valueOf(v);
                FlagStore.setOverride(prefs, flagTier, k, val);
                count++;
            }
            if (flagAdapter != null) flagAdapter.reload(flagSearch == null ? "" : flagSearch.getText().toString());
            updateHomePackInfo();
            Toast.makeText(this, count + " flag diimpor ke pack " + FlagStore.tierName(flagTier) + ".", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "JSON tidak valid. Pastikan isinya format {\"NamaFlag\":\"nilai\"}.", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- TAB FASTFLAG (EDITOR) ----------

    private void buildFlags() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, 0, dp(6));
        for (int i = 0; i < 3; i++) {
            final String tier = FlagStore.TIERS[i];
            Button b = bigButton(FlagStore.TIER_NAMES[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
            if (i > 0) lp.leftMargin = dp(6);
            b.setLayoutParams(lp);
            b.setTag("chip_" + tier);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                flagTier = tier;
                refreshChips();
                flagAdapter.reload(flagSearch.getText().toString());
            } });
            chips.addView(b);
        }
        box.addView(chips);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 0, 0, dp(6));
        Button add = bigButton("+ Tambah");
        add.setTextSize(13);
        add.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { addFlagDialog(); } });
        Button impFile = bigButton("Impor File");
        impFile.setTextSize(13);
        impFile.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            startActivityForResult(i, REQ_IMPORT_JSON);
        } });
        Button impPaste = bigButton("Tempel JSON");
        impPaste.setTextSize(13);
        impPaste.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pasteJsonDialog(); } });
        Button clear = bigButton("Hapus Semua");
        clear.setTextSize(13);
        clear.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Hapus Semua Ubahan?")
                .setMessage("Semua ubahan manual dan flag tambahan di pack " + FlagStore.tierName(flagTier) + " dihapus, kembali ke pack asli. Lapisan pengaturan Grafis tidak ikut terhapus.")
                .setPositiveButton("Hapus", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) {
                    FlagStore.clearAllOverrides(prefs, flagTier);
                    flagAdapter.reload(flagSearch.getText().toString());
                    updateHomePackInfo();
                } })
                .setNegativeButton("Batal", null)
                .show();
        } });
        Button[] ab = {add, impFile, impPaste, clear};
        for (int i = 0; i < ab.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
            if (i > 0) lp.leftMargin = dp(5);
            ab[i].setLayoutParams(lp);
            actions.addView(ab[i]);
        }
        box.addView(actions);

        flagSearch = new EditText(this);
        flagSearch.setHint("Cari nama flag...");
        flagSearch.setSingleLine(true);
        flagSearch.setTextColor(Color.WHITE);
        flagSearch.setHintTextColor(Color.rgb(150, 140, 180));
        GradientDrawable sgd = new GradientDrawable();
        sgd.setColor(Color.argb(215, 18, 14, 34));
        sgd.setCornerRadius(dp(10));
        sgd.setStroke(dp(1), Color.rgb(94, 74, 160));
        flagSearch.setBackground(sgd);
        flagSearch.setPadding(dp(10), dp(6), dp(10), dp(6));
        box.addView(flagSearch);

        flagCountView = infoText("");
        box.addView(flagCountView);

        ListView lv = new ListView(this);
        lv.setDivider(null);
        flagAdapter = new FlagAdapter();
        lv.setAdapter(flagAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                editFlag(flagAdapter.names.get(pos));
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1));

        flagSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) { flagAdapter.reload(s.toString()); }
        });

        pages.addView(box, new LinearLayout.LayoutParams(-1, -1));
        refreshChips();
    }

    private void refreshChips() {
        LinearLayout box = (LinearLayout) pages.getChildAt(1);
        for (String t : FlagStore.TIERS) {
            View v = box.findViewWithTag("chip_" + t);
            if (v != null) v.setAlpha(t.equals(flagTier) ? 1f : 0.55f);
        }
    }

    private class FlagAdapter extends BaseAdapter {
        ArrayList<String> names = new ArrayList<>();
        FlagAdapter() { reload(""); }
        void reload(String q) {
            names.clear();
            String needle = q == null ? "" : q.trim().toLowerCase();
            for (String k : FlagStore.listNames(MainActivity.this, prefs, flagTier)) {
                if (needle.isEmpty() || k.toLowerCase().contains(needle)) names.add(k);
            }
            if (flagCountView != null) {
                int ov = FlagStore.overrideCount(prefs, flagTier);
                flagCountView.setText("Pack " + FlagStore.tierName(flagTier) + ": " + names.size() + " flag tampil" + (ov > 0 ? " • " + ov + " diubah/ditambah" : "") + ". Ketuk flag untuk mengubah nilainya.");
            }
            notifyDataSetChanged();
        }
        public int getCount() { return names.size(); }
        public Object getItem(int pos) { return names.get(pos); }
        public long getItemId(int pos) { return pos; }
        public View getView(int pos, View convertView, ViewGroup parent) {
            LinearLayout row;
            TextView nameT, valT;
            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(10), dp(9), dp(10), dp(9));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.argb(190, 16, 12, 30));
                gd.setCornerRadius(dp(8));
                row.setBackground(gd);
                nameT = new TextView(MainActivity.this);
                nameT.setTextSize(12);
                nameT.setTextColor(Color.rgb(226, 220, 245));
                nameT.setTag("n");
                valT = new TextView(MainActivity.this);
                valT.setTextSize(12.5f);
                valT.setTypeface(Typeface.DEFAULT_BOLD);
                valT.setGravity(Gravity.RIGHT);
                valT.setTag("v");
                row.addView(nameT, new LinearLayout.LayoutParams(0, -2, 1));
                LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-2, -2);
                vlp.leftMargin = dp(8);
                row.addView(valT, vlp);
                row.setLayoutParams(new AbsListView.LayoutParams(-1, -2));
            } else {
                row = (LinearLayout) convertView;
                nameT = (TextView) row.findViewWithTag("n");
                valT = (TextView) row.findViewWithTag("v");
            }
            String name = names.get(pos);
            boolean ovr = FlagStore.isOverridden(prefs, flagTier, name);
            boolean custom = FlagStore.isCustom(MainActivity.this, prefs, flagTier, name);
            nameT.setText(name + (custom ? "  [tambahan]" : ""));
            valT.setText(FlagStore.value(MainActivity.this, prefs, flagTier, name) + (ovr ? " *" : ""));
            valT.setTextColor(ovr ? Color.rgb(255, 213, 109) : Color.rgb(147, 197, 253));
            return row;
        }
    }

    private void editFlag(final String name) {
        final String cur = FlagStore.value(this, prefs, flagTier, name);
        final boolean isBool = cur.equals("True") || cur.equals("False");
        final boolean custom = FlagStore.isCustom(this, prefs, flagTier, name);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        final Switch sw = new Switch(this);
        final EditText et = new EditText(this);
        if (isBool) {
            sw.setText(cur.equals("True") ? "Nilai: True (nyala)" : "Nilai: False (mati)");
            sw.setChecked(cur.equals("True"));
            sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton b, boolean on) { sw.setText(on ? "Nilai: True (nyala)" : "Nilai: False (mati)"); }
            });
            box.addView(sw);
        } else {
            et.setText(cur);
            et.setSingleLine(true);
            box.addView(et);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(name);
        b.setView(box);
        b.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                String nv = isBool ? (sw.isChecked() ? "True" : "False") : et.getText().toString().trim();
                if (nv.isEmpty()) { Toast.makeText(MainActivity.this, "Nilai tidak boleh kosong.", Toast.LENGTH_SHORT).show(); return; }
                FlagStore.setOverride(prefs, flagTier, name, nv);
                flagAdapter.reload(flagSearch.getText().toString());
                updateHomePackInfo();
                Toast.makeText(MainActivity.this, name + " = " + nv, Toast.LENGTH_SHORT).show();
            }
        });
        if (custom) {
            b.setNeutralButton("Hapus Flag", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    FlagStore.clearOverride(prefs, flagTier, name);
                    flagAdapter.reload(flagSearch.getText().toString());
                    updateHomePackInfo();
                }
            });
        } else if (FlagStore.isOverridden(prefs, flagTier, name)) {
            b.setNeutralButton("Kembalikan ke Asli", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    FlagStore.clearOverride(prefs, flagTier, name);
                    flagAdapter.reload(flagSearch.getText().toString());
                    updateHomePackInfo();
                }
            });
        }
        b.setNegativeButton("Batal", null);
        b.show();
    }

    private void addFlagDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        final EditText nameEt = new EditText(this);
        nameEt.setHint("Nama flag, contoh: FFlagContohBaru");
        nameEt.setSingleLine(true);
        box.addView(nameEt);
        final EditText valEt = new EditText(this);
        valEt.setHint("Nilai, contoh: True / False / 120");
        valEt.setSingleLine(true);
        box.addView(valEt);
        new AlertDialog.Builder(this)
            .setTitle("Tambah Flag ke Pack " + FlagStore.tierName(flagTier))
            .setView(box)
            .setPositiveButton("Tambah", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String n = nameEt.getText().toString().trim();
                    String v = valEt.getText().toString().trim();
                    if (n.isEmpty() || v.isEmpty()) { Toast.makeText(MainActivity.this, "Nama dan nilai wajib diisi.", Toast.LENGTH_SHORT).show(); return; }
                    if (n.contains(" ") || n.contains("_")) { Toast.makeText(MainActivity.this, "Nama flag tidak boleh ada spasi atau garis bawah, nanti tidak dibaca MasterStrap.", Toast.LENGTH_LONG).show(); return; }
                    FlagStore.setOverride(prefs, flagTier, n, v);
                    flagAdapter.reload(flagSearch.getText().toString());
                    updateHomePackInfo();
                    Toast.makeText(MainActivity.this, "Flag " + n + " ditambahkan.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void pasteJsonDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        final EditText et = new EditText(this);
        et.setHint("{\"NamaFlag\":\"nilai\", ...}");
        et.setMinLines(4);
        box.addView(et);
        new AlertDialog.Builder(this)
            .setTitle("Tempel JSON ke Pack " + FlagStore.tierName(flagTier))
            .setView(box)
            .setPositiveButton("Impor", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { importJson(et.getText().toString()); }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    // ---------- TAB GRAFIS (FF SETTINGS) ----------

    private Button optButton(String tag, String label) {
        Button b = bigButton(label);
        b.setTag(tag);
        b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.bottomMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private void markOpt(String tag, boolean active, String label) {
        if (grafRoot == null) return;
        View v = grafRoot.findViewWithTag(tag);
        if (v instanceof Button) ((Button) v).setText((active ? "● " : "○ ") + label);
    }

    private void buildGrafis() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        sv.addView(box);
        grafRoot = sv;

        LinearLayout c0 = card();
        c0.addView(switchRow("apply_opt", true, "Terapkan Optimasi Roblox",
            "Mengikuti saklar yang sama seperti di MasterStrap. Saat menyala, semua pengaturan di tab Grafis dan Preset masuk ke JSON yang disalin ke MasterStrap. Saat mati, yang terkirim hanya pack asli plus ubahan manualmu di editor FastFlag, seperti Roblox konfigurasi normal."));
        box.addView(c0);

        LinearLayout c1 = card();
        c1.addView(hText("Kualitas Grafis"));
        c1.addView(infoText("Anti-aliasing (MSAA) menghaluskan tepi gambar. Makin besar angkanya makin halus tapi makin berat. Otomatis berarti ikut nilai pack."));
        final String[][] msaa = {{"auto", "Otomatis"}, {"0", "Off / 1x"}, {"2", "2x"}, {"4", "4x"}, {"8", "8x"}};
        for (final String[] pair : msaa) {
            Button b = optButton("gmsaa_" + pair[0], pair[1]);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                if (pair[0].equals("auto")) FlagStore.clearSetting(prefs, F_MSAA);
                else FlagStore.setSetting(prefs, F_MSAA, pair[0]);
                refreshGrafis();
                Toast.makeText(MainActivity.this, "MSAA: " + pair[1] + ". Salin ulang JSON ke MasterStrap.", Toast.LENGTH_SHORT).show();
            } });
            c1.addView(b);
        }
        c1.addView(infoText("Level kualitas render tetap Roblox (FRM), dari 1 sampai 21 seperti di MasterStrap. Level 1 paling ringan. Otomatis berarti ikut nilai pack."));
        final String[][] frm = {{"auto", "Otomatis"}, {"1", "Level 1 (paling ringan)"}, {"3", "Level 3"}, {"5", "Level 5"}, {"8", "Level 8"}, {"10", "Level 10 (seimbang)"}, {"21", "Level 21 (paling tinggi)"}};
        for (final String[] pair : frm) {
            Button b = optButton("gfrm_" + pair[0], pair[1]);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                if (pair[0].equals("auto")) FlagStore.clearSetting(prefs, F_FRM);
                else FlagStore.setSetting(prefs, F_FRM, pair[0]);
                refreshGrafis();
                Toast.makeText(MainActivity.this, "FRM: " + pair[1] + ". Salin ulang JSON ke MasterStrap.", Toast.LENGTH_SHORT).show();
            } });
            c1.addView(b);
        }
        box.addView(c1);

        LinearLayout c2 = card();
        c2.addView(hText("Mesin Render"));
        c2.addView(infoText("Otomatis membiarkan Roblox memilih. Vulkan dan OpenGL memasang pasangan FastFlag API grafis Android yang cocok untuk peluncuran berikutnya. Vulkan terbukti lancar di HP-mu."));
        final String[][] eng = {{"auto", "Otomatis"}, {"vk", "Vulkan"}, {"gl", "OpenGL"}};
        for (final String[] pair : eng) {
            Button b = optButton("geng_" + pair[0], pair[1]);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { setEngine(pair[0]); } });
            c2.addView(b);
        }
        grafEngineInfo = infoText("");
        c2.addView(grafEngineInfo);
        box.addView(c2);

        LinearLayout c3 = card();
        c3.addView(hText("Resolusi Grafis Dinamis"));
        c3.addView(switchRow("graf_res_on", false, "Pakai Resolusi Grafis Pilihan",
            "Saat tombol MAIN ditekan, resolusi render diturunkan ke pilihan di bawah lewat Shizuku atau root, lalu Roblox dibuka. Gambar jadi lebih buram tapi jauh lebih ringan. Ini pengaturan sistem (wm size), terpisah dari FastFlag. Pilihan di MasterStrap bekerja di dalam client-nya; di sini efek yang sama dicapai dari sisi sistem. Pilihan paling rendah di sini 360p demi keamanan, karena cara sistem ini mengubah resolusi SELURUH HP, bukan cuma dalam game: angka di bawah itu membuat semua tulisan dan tombol raksasa dan HP susah dipakai. Selalu ada tombol Kembalikan Resolusi Normal dan banner merah di atas aplikasi selama resolusi sedang diturunkan."));
        LinearLayout rowR = new LinearLayout(this);
        rowR.setOrientation(LinearLayout.HORIZONTAL);
        final int[] resVals = {360, 480, 720, 1080};
        for (final int rv : resVals) {
            Button b = bigButton(rv + "p");
            b.setTextSize(12.5f);
            b.setTag("gres_" + rv);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
            lp.rightMargin = dp(4);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                prefs.edit().putInt("graf_res_h", rv).apply();
                refreshGrafis();
                Toast.makeText(MainActivity.this, "Resolusi grafis dipilih: " + rv + "p.", Toast.LENGTH_SHORT).show();
            } });
            rowR.addView(b);
        }
        c3.addView(rowR);
        resStateView = infoText("");
        c3.addView(resStateView);
        Button restore = bigButton("Kembalikan Resolusi Normal");
        restore.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { restoreResolution(); } });
        c3.addView(restore);
        box.addView(c3);

        LinearLayout c4 = card();
        c4.addView(hText("Preferensi Roblox"));
        c4.addView(settingSwitchRow(bundle(F_FRM, "1"), "Paksa Grafis Terendah",
            "Memaksa kualitas grafis ke level 1 saat kamu meluncurkan Roblox. Di MasterStrap fitur ini menambal lewat client-nya; di sini level yang sama dipasang lewat FastFlag FRM, tanpa root."));
        box.addView(c4);

        LinearLayout c5 = card();
        c5.addView(hText("Performa Mobile"));
        c5.addView(infoText("Target frame rate (global) untuk peluncuran berikutnya, sama seperti kolom angka di MasterStrap (30 sampai 9999). FPS asli tetap tergantung game, GPU, dan layar."));
        final EditText fpsEt = new EditText(this);
        fpsEt.setSingleLine(true);
        fpsEt.setTextColor(Color.WHITE);
        fpsEt.setHintTextColor(Color.rgb(150, 140, 180));
        fpsEt.setHint("Target FPS, contoh: 120");
        c5.addView(fpsEt);
        grafFpsInfo = infoText("");
        c5.addView(grafFpsInfo);
        Button fpsSet = bigButton("Terapkan Target FPS");
        fpsSet.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            try {
                int val = Integer.parseInt(fpsEt.getText().toString().trim());
                if (val < 30) val = 30;
                if (val > 9999) val = 9999;
                FlagStore.setSetting(prefs, F_FPS, String.valueOf(val));
                refreshGrafis();
                Toast.makeText(MainActivity.this, "Target FPS = " + val + ". Salin ulang JSON ke MasterStrap.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Isi angka dulu, contoh: 120.", Toast.LENGTH_SHORT).show();
            }
        } });
        c5.addView(fpsSet);
        Button fpsClear = bigButton("Target FPS Ikut Pack");
        LinearLayout.LayoutParams fclp = new LinearLayout.LayoutParams(-1, -2);
        fclp.topMargin = dp(8);
        fpsClear.setLayoutParams(fclp);
        fpsClear.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            FlagStore.clearSetting(prefs, F_FPS);
            refreshGrafis();
        } });
        c5.addView(fpsClear);
        c5.addView(settingSwitchRow(bundle(F_MSAA, "0", F_POSTFX, "True", F_GRASS_MIN, "0", F_GRASS_MAX, "0", F_MESH_ADAPT, "True"),
            "Boost Grafis Sekali Sentuh", "Mengurangi anti-aliasing, efek visual, dan rumput, sambil mengoptimalkan memori model 3D untuk mengutamakan FPS."));
        c5.addView(settingSwitchRow(bundle(F_LOD, "50"),
            "Kurangi Detail Model 3D", "Model 3D yang jauh diganti versi sederhananya lebih cepat, mengurangi beban render dan memori."));
        c5.addView(settingSwitchRow(bundle(F_TEX_OTA, "256", F_TEX_UI, "256"),
            "Kurangi Beban Tekstur", "Melewati tekstur resolusi sangat tinggi untuk mengurangi pemakaian RAM dan memori grafis."));
        c5.addView(settingSwitchRow(bundle(F_MESH_ADAPT, "True"),
            "Optimalkan Memori Model 3D", "Menyeimbangkan otomatis memori yang dipakai model 3D di perangkat Android."));
        c5.addView(settingSwitchRow(bundle(F_PRELOAD1, "True", F_PRELOAD2, "True"),
            "Muat Model 3D Lebih Awal", "Model 3D dimuat lebih awal untuk mengurangi objek yang muncul terlambat saat bermain."));
        c5.addView(settingSwitchRow(bundle(F_GOM_MIN, "16", F_GOM_MAX, "25"),
            "Stabilkan Frame Rate (Grafis Adaptif)", "Mode optimasi grafis Roblox menyesuaikan kualitas mengikuti waktu frame GPU supaya gameplay tetap stabil saat suasana ramai atau berat."));
        box.addView(c5);

        LinearLayout c6 = card();
        c6.addView(hText("Render dan Efek Visual"));
        c6.addView(settingSwitchRow(bundle(F_SHADOW, "0"),
            "Matikan Bayangan", "Mematikan bayangan dalam game (intensitas bayangan 0) untuk mengurangi beban GPU, terutama di perangkat kelas bawah."));
        c6.addView(settingSwitchRow(bundle(F_POSTFX, "True"),
            "Kurangi Efek Visual", "Menghapus bloom, blur, depth-of-field, dan efek sejenis untuk mengurangi beban GPU (menonaktifkan PostFX)."));
        c6.addView(settingSwitchRow(bundle(F_GRASS_MIN, "0", F_GRASS_MAX, "0"),
            "Hapus Rumput", "Jarak render rumput diatur ke nol, menghilangkan pekerjaan grafis di suasana luar ruangan."));
        c6.addView(settingSwitchRow(bundle(F_SKYGRAY, "True"),
            "Langit Abu (SkyGray)", "Mengganti langit normal dengan latar abu sederhana untuk suasana yang lebih ringan."));
        c6.addView(settingSwitchRow(bundle(F_DISPFPS, "True"),
            "Tampilkan FPS", "Mengaktifkan penghitung FPS bawaan Roblox."));
        box.addView(c6);

        LinearLayout c7 = card();
        c7.addView(hText("Antarmuka dan Animasi"));
        c7.addView(settingSwitchRow(bundle(F_ANIMCACHE, "True"),
            "Percepat Animasi Berulang", "Data animasi disimpan di memori supaya animasi yang sama tidak perlu di-decode berulang kali."));
        box.addView(c7);

        LinearLayout c8 = card();
        c8.addView(hText("Yang Sengaja Tidak Ada"));
        c8.addView(infoText("Beberapa fitur MasterStrap tidak kutampilkan di sini karena di luar client MasterStrap sendiri jalannya tidak ada, dan aku tidak mau ada tombol yang tidak berguna: Kurangi Gerakan dan Haptik dalam game (menambal file pengaturan Roblox, di MasterStrap pun butuh root), No Texture (memakai flag teks/string yang justru ditolak editor MasterStrap), Desync / pengurang latensi / perbaikan hitreg jaringan (flag resminya bernama garis bawah atau tidak tersedia untuk format JSON), dan Mods font / shader / stretch screen (menambal client secara langsung)."));
        box.addView(c8);

        pages.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        refreshGrafis();
    }

    private void setEngine(String mode) {
        if (mode.equals("auto")) {
            FlagStore.clearSetting(prefs, F_PREF_VK);
            FlagStore.clearSetting(prefs, F_PREF_GL);
            FlagStore.clearSetting(prefs, F_DIS_VK);
            FlagStore.clearSetting(prefs, F_DIS_GL);
        } else if (mode.equals("vk")) {
            FlagStore.setSetting(prefs, F_PREF_VK, "True");
            FlagStore.setSetting(prefs, F_PREF_GL, "False");
            FlagStore.setSetting(prefs, F_DIS_VK, "False");
            FlagStore.setSetting(prefs, F_DIS_GL, "True");
        } else {
            FlagStore.setSetting(prefs, F_PREF_VK, "False");
            FlagStore.setSetting(prefs, F_PREF_GL, "True");
            FlagStore.setSetting(prefs, F_DIS_VK, "False");
            FlagStore.setSetting(prefs, F_DIS_GL, "False");
        }
        refreshGrafis();
        Toast.makeText(this, "Mesin render: " + (mode.equals("vk") ? "Vulkan" : mode.equals("gl") ? "OpenGL" : "Otomatis") + ". Salin ulang JSON pack ke MasterStrap.", Toast.LENGTH_LONG).show();
    }

    private void refreshGrafis() {
        if (grafRoot == null) return;
        String tier = activeTier();
        // MSAA
        String msaa = FlagStore.getSetting(prefs, F_MSAA);
        String[][] msaaOpts = {{"auto", "Otomatis"}, {"0", "Off / 1x"}, {"2", "2x"}, {"4", "4x"}, {"8", "8x"}};
        for (String[] pair : msaaOpts) markOpt("gmsaa_" + pair[0], pair[0].equals(msaa == null ? "auto" : msaa), pair[1]);
        // FRM
        String frm = FlagStore.getSetting(prefs, F_FRM);
        String[][] frmOpts = {{"auto", "Otomatis"}, {"1", "Level 1 (paling ringan)"}, {"3", "Level 3"}, {"5", "Level 5"}, {"8", "Level 8"}, {"10", "Level 10 (seimbang)"}, {"21", "Level 21 (paling tinggi)"}};
        for (String[] pair : frmOpts) markOpt("gfrm_" + pair[0], pair[0].equals(frm == null ? "auto" : frm), pair[1]);
        // Mesin render
        boolean engSet = FlagStore.hasSetting(prefs, F_PREF_VK);
        String pvk = FlagStore.getSetting(prefs, F_PREF_VK);
        markOpt("geng_auto", !engSet, "Otomatis");
        markOpt("geng_vk", engSet && "True".equals(pvk), "Vulkan");
        markOpt("geng_gl", engSet && !"True".equals(pvk), "OpenGL");
        if (grafEngineInfo != null) grafEngineInfo.setText("Nilai efektif di pack " + FlagStore.tierName(tier) + ": PreferVulkan=" + FlagStore.effValue(this, prefs, tier, F_PREF_VK) + ", PreferOpenGL=" + FlagStore.effValue(this, prefs, tier, F_PREF_GL) + ", DisableVulkan=" + FlagStore.effValue(this, prefs, tier, F_DIS_VK) + ", DisableOpenGL=" + FlagStore.effValue(this, prefs, tier, F_DIS_GL) + ".");
        // FPS info
        if (grafFpsInfo != null) {
            String f = FlagStore.getSetting(prefs, F_FPS);
            grafFpsInfo.setText(f != null ? "Target FPS dari pengaturan: " + f + "." : "Target FPS sekarang ikut pack: " + FlagStore.value(this, prefs, tier, F_FPS) + ".");
        }
        // Resolusi chips
        int rh = prefs.getInt("graf_res_h", 720);
        for (int v : new int[]{360, 480, 720, 1080}) {
            View b = grafRoot.findViewWithTag("gres_" + v);
            if (b != null) b.setAlpha(v == rh ? 1f : 0.55f);
        }
        updateResState();
    }

    private void updateResState() {
        if (resStateView != null) {
            int ow = prefs.getInt("orig_w", 0);
            resStateView.setText(ow > 0 ? "Resolusi asli tersimpan: " + ow + "x" + prefs.getInt("orig_h", 0) + ". Tombol di bawah mengembalikannya." : "Belum ada penurunan resolusi yang tersimpan.");
        }
        updateResBanner();
    }

    private void updateResBanner() {
        if (resBanner != null) resBanner.setVisibility(prefs.getInt("orig_w", 0) > 0 ? View.VISIBLE : View.GONE);
    }

    private void restoreResolution() {
        new Thread(new Runnable() { public void run() {
            Shell.Result r = Priv.exec("wm size reset", false);
            prefs.edit().remove("orig_w").remove("orig_h").apply();
            final boolean ok = r != null && r.code == 0;
            runOnUiThread(new Runnable() { public void run() {
                Toast.makeText(MainActivity.this, ok ? "Resolusi dikembalikan." : "Gagal. Butuh Shizuku aktif atau root.", Toast.LENGTH_SHORT).show();
                updateResState();
            } });
        } }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        updateResBanner();
    }

    // ---------- TAB GAME MODE ----------

    private void buildGameMode() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        sv.addView(box);

        LinearLayout c1 = card();
        c1.addView(hText("Shizuku"));
        c1.addView(infoText("Sebagian fitur Game Mode memakai izin tingkat ADB dari Shizuku (tanpa root, tanpa mengubah sistem). Aktifkan Shizuku lewat Wireless Debugging, lalu minta izin di sini. Di MasterStrap fitur-fitur semacam ini sebagian terkunci Premium; di sini gratis."));
        Button izin = bigButton("Minta Izin Shizuku");
        izin.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            if (!Priv.shizukuAlive()) {
                Toast.makeText(MainActivity.this, "Shizuku belum aktif. Pasang aplikasi Shizuku lalu aktifkan lewat Wireless Debugging.", Toast.LENGTH_LONG).show();
                Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                if (i != null) startActivity(i);
                return;
            }
            if (Priv.shizukuGranted()) { Toast.makeText(MainActivity.this, "Izin Shizuku sudah diberikan.", Toast.LENGTH_SHORT).show(); refreshStatus(); return; }
            try { rikka.shizuku.Shizuku.requestPermission(1001); Toast.makeText(MainActivity.this, "Permintaan izin dikirim ke Shizuku.", Toast.LENGTH_SHORT).show(); }
            catch (Throwable t) { Toast.makeText(MainActivity.this, "Gagal meminta izin Shizuku.", Toast.LENGTH_SHORT).show(); }
        } });
        c1.addView(izin);
        box.addView(c1);

        LinearLayout c2 = card();
        c2.addView(hText("Boost Aman (Stabilitas dan Latensi)"));
        c2.addView(switchRow("gm_freemem", false, "Bersihkan Memori dan Cache Sebelum Main",
            "Menjalankan pembersihan proses latar dan cache sistem (am kill-all dan pm trim-caches lewat Shizuku) tepat sebelum Roblox dibuka, supaya RAM lega. Bukan pembersih berkala."));
        c2.addView(switchRow("boost_stopbg", false, "Hentikan Aplikasi Latar",
            "Aplikasi pihak ketiga lain dihentikan paksa supaya RAM dan CPU lega untuk Roblox. Lewat Shizuku atau root, diterapkan saat MAIN ditekan."));
        c2.addView(switchRow("gm_wifi", false, "Wi-Fi Latensi Rendah Saat Membuka Roblox",
            "Meminta mode Wi-Fi latensi rendah Android selama Roblox dibuka (hanya bekerja saat tersambung Wi-Fi). Dilepas otomatis setelah 90 detik."));
        c2.addView(switchRow("gm_cpuwake", false, "Jaga CPU Tetap Bangun Saat Roblox Dibuka",
            "Mencegah prosesor tertidur sampai 60 detik selama Roblox mulai. Tidak meng-overclock dan tidak mengubah kecepatan CPU."));
        c2.addView(switchRow("gm_hotguard", false, "Jeda Boost Saat HP Terlalu Panas",
            "Suhu perangkat dicek sebelum boost jalan. Kalau terdeteksi panas berat (43 derajat atau lebih), semua boost peluncuran dilewati sampai perangkat dingin."));
        box.addView(c2);

        LinearLayout c3 = card();
        c3.addView(hText("Sesi Main"));
        c3.addView(switchRow("gm_display", false, "Layar Tetap Nyala Selama Sesi",
            "Meminta kunci layar menyala selama sesi main (maksimal 2 jam, lalu lepas sendiri)."));
        // Dua saklar daya yang saling meniadakan:
        LinearLayout powerRow = switchRow("gm_power", false, "Daya Tanpa Batas untuk Roblox",
            "Roblox dimasukkan daftar putih penghemat baterai Android (deviceidle) lewat Shizuku, jadi tidak dibatasi sistem selama sesi. Batas panas Android tetap berlaku.");
        gmPowerSw = findSwitch(powerRow);
        c3.addView(powerRow);
        LinearLayout battRow = switchRow("gm_batt", false, "Roblox Hemat Baterai di Latar",
            "Kebalikannya: Roblox dicabut dari daftar putih penghemat baterai, membiarkan Android menghemat daya saat game berjalan di latar.");
        gmBattSw = findSwitch(battRow);
        c3.addView(battRow);
        if (gmPowerSw != null) gmPowerSw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                prefs.edit().putBoolean("gm_power", on).apply();
                if (on && gmBattSw != null && gmBattSw.isChecked()) gmBattSw.setChecked(false);
            }
        });
        if (gmBattSw != null) gmBattSw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                prefs.edit().putBoolean("gm_batt", on).apply();
                if (on && gmPowerSw != null && gmPowerSw.isChecked()) gmPowerSw.setChecked(false);
            }
        });
        c3.addView(switchRow("gm_prio", false, "Prioritas CPU untuk Roblox (Root)",
            "Proses Roblox yang sedang berjalan diberi prioritas CPU lebih tinggi (renice). Butuh root penuh; tanpa root langkah ini dilewati dengan jujur saat MAIN ditekan."));
        c3.addView(switchRow("boost_dnd", false, "Mode Jangan Ganggu",
            "Notifikasi dan panggilan disenyapkan selama bermain. Memakai izin Jangan Ganggu Android (bukan root)."));
        Button dndOff = bigButton("Matikan Jangan Ganggu");
        dndOff.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { setDnd(false); } });
        c3.addView(dndOff);
        box.addView(c3);

        Button play2 = bigButton("BOOST & MAIN ROBLOX");
        play2.setTextSize(17);
        play2.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { launchRoblox(); } });
        box.addView(play2);

        pages.addView(sv, new LinearLayout.LayoutParams(-1, -1));
    }

    private Switch findSwitch(View v) {
        if (v instanceof Switch) return (Switch) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                Switch s = findSwitch(g.getChildAt(i));
                if (s != null) return s;
            }
        }
        return null;
    }

    // ---------- TAB PRESET ----------

    private static class PresetDef {
        final String name, kind;
        final Map<String, String> data;
        PresetDef(String n, String k, Map<String, String> d) { name = n; kind = k; data = d; }
    }

    private List<PresetDef> presetDefs() {
        List<PresetDef> l = new ArrayList<>();
        Map<String, String> comp = bundle(F_FRM, "1", F_MSAA, "0", F_FPS, "240", F_POSTFX, "True",
            F_GRASS_MIN, "0", F_GRASS_MAX, "0", F_SHADOW, "0", F_SKYGRAY, "True", F_DISPFPS, "True");
        Map<String, String> heavy = bundle(F_FRM, "1", F_MSAA, "0", F_FPS, "120", F_POSTFX, "True",
            F_GRASS_MIN, "0", F_GRASS_MAX, "0", F_SHADOW, "0", F_TEX_OTA, "256", F_TEX_UI, "256",
            F_PRELOAD1, "True", F_PRELOAD2, "True", F_MESH_ADAPT, "True");
        l.add(new PresetDef("Blox Fruits", "Berat", heavy));
        String[] compGames = {"The Strongest Battlegrounds", "Rivals", "Volleyball Legends", "Blade Ball",
            "Basketball Legends", "Bedwars", "Blue Lock Rivals", "Evade", "Football Fusion 2",
            "Jujutsu Shenanigans", "Murder Mystery 2", "NFL Universe Football", "Pure Soccer",
            "Timebomb Duels", "Touch Football", "TPS Street Soccer", "Volleyball 4.2"};
        for (String g : compGames) l.add(new PresetDef(g, "Kompetitif", comp));
        return l;
    }

    private Set<String> presetAllKeys() {
        Set<String> s = new HashSet<>();
        for (PresetDef d : presetDefs()) s.addAll(d.data.keySet());
        return s;
    }

    private void buildPreset() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        sv.addView(box);
        presetRoot = sv;

        LinearLayout c0 = card();
        c0.addView(hText("Preset per Game"));
        c0.addView(infoText("Di MasterStrap, preset game seperti ini terkunci Premium. Di RakaStrap semuanya gratis. Menekan Boost memasang bundel pengaturan yang cocok untuk game itu ke lapisan pengaturan (FRM 1, MSAA mati, bayangan mati, dan kawan-kawan). Setelah itu salin ulang JSON pack aktif ke MasterStrap, lalu main. Tanpa foto game di sini: daftar ini teks saja supaya bersih."));
        box.addView(c0);

        for (final PresetDef def : presetDefs()) {
            LinearLayout row = card();
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView nm = new TextView(this);
            nm.setText(def.name);
            nm.setTextSize(15.5f);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            nm.setTextColor(Color.WHITE);
            texts.addView(nm);
            TextView kd = new TextView(this);
            kd.setText("Profil: " + def.kind);
            kd.setTextSize(12);
            kd.setTextColor(Color.rgb(200, 190, 230));
            texts.addView(kd);
            top.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            Button b = bigButton("Boost");
            b.setTag("preset_" + def.name);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(104), dp(46));
            b.setLayoutParams(blp);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { togglePreset(def); } });
            top.addView(b);
            row.addView(top);
            box.addView(row);
        }

        pages.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        refreshPreset();
    }

    private void togglePreset(PresetDef def) {
        String active = prefs.getString("active_preset", "");
        Map<String, String> clearMap = new LinkedHashMap<>();
        for (String k : presetAllKeys()) clearMap.put(k, "");
        if (def.name.equals(active)) {
            FlagStore.applyBundle(prefs, clearMap, false);
            prefs.edit().remove("active_preset").apply();
            Toast.makeText(this, "Preset " + def.name + " dibatalkan.", Toast.LENGTH_SHORT).show();
        } else {
            FlagStore.applyBundle(prefs, clearMap, false);
            FlagStore.applyBundle(prefs, def.data, true);
            prefs.edit().putString("active_preset", def.name).apply();
            Toast.makeText(this, "Preset " + def.name + " terpasang. Salin ulang JSON pack ke MasterStrap, lalu main.", Toast.LENGTH_LONG).show();
        }
        refreshPreset();
        updateHomePackInfo();
    }

    private void refreshPreset() {
        if (presetRoot == null) return;
        String active = prefs.getString("active_preset", "");
        for (PresetDef def : presetDefs()) {
            View v = presetRoot.findViewWithTag("preset_" + def.name);
            if (v instanceof Button) {
                boolean on = def.name.equals(active);
                ((Button) v).setText(on ? "Batal" : "Boost");
                v.setAlpha(on ? 1f : 0.75f);
            }
        }
    }

    // ---------- AKSI MAIN + BOOST ----------

    private int batteryTempTenths() {
        try {
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) return bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        } catch (Throwable t) { /* abaikan */ }
        return 0;
    }

    private void launchRoblox() {
        Toast.makeText(this, "Menyiapkan boost...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() { public void run() {
            final List<String> notes = new ArrayList<>();
            boolean hot = prefs.getBoolean("gm_hotguard", false) && batteryTempTenths() >= 430;
            if (hot) {
                notes.add("Boost dilewati: perangkat terdeteksi panas.");
            } else {
                if (prefs.getBoolean("graf_res_on", false)) {
                    Shell.Result r = applyGrafResolution();
                    notes.add(r != null && r.code == 0 ? null : "Resolusi grafis gagal (Shizuku/root tidak siap).");
                }
                if (prefs.getBoolean("gm_freemem", false)) {
                    Priv.exec("am kill-all; pm trim-caches 999G", false);
                }
                if (prefs.getBoolean("boost_stopbg", false)) {
                    Shell.Result r = stopBackground();
                    notes.add(r != null && r.code == 0 ? null : "Stop aplikasi latar gagal (Shizuku/root tidak siap).");
                }
                if (prefs.getBoolean("gm_power", false)) {
                    Priv.exec("cmd deviceidle whitelist +" + ROBLOX_PKG, false);
                }
                if (prefs.getBoolean("gm_batt", false)) {
                    Priv.exec("cmd deviceidle whitelist -" + ROBLOX_PKG, false);
                }
                if (prefs.getBoolean("gm_prio", false)) {
                    if (Priv.hasRootCached()) {
                        Priv.exec("for pid in $(pidof " + ROBLOX_PKG + "); do renice -n -15 -p $pid 2>/dev/null; done; true", true);
                    } else {
                        notes.add("Prioritas CPU dilewati: butuh root.");
                    }
                }
            }
            runOnUiThread(new Runnable() { public void run() {
                for (String n : notes) if (n != null) Toast.makeText(MainActivity.this, n, Toast.LENGTH_LONG).show();
                if (prefs.getBoolean("gm_wifi", false)) acquireWifiLowLatency();
                if (prefs.getBoolean("gm_cpuwake", false)) acquireCpuWake();
                if (prefs.getBoolean("gm_display", false)) acquireDisplayWake();
                if (prefs.getBoolean("boost_dnd", false)) setDnd(true);
                Intent i = getPackageManager().getLaunchIntentForPackage(ROBLOX_PKG);
                if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
                else Toast.makeText(MainActivity.this, "Roblox tidak terdeteksi. Pasang dulu client Roblox/MasterStrap.", Toast.LENGTH_LONG).show();
                updateResState();
            } });
        } }).start();
    }

    private void acquireWifiLowLatency() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return;
            if (wifiLock == null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "rakastrap");
                wifiLock.setReferenceCounted(false);
            }
            if (!wifiLock.isHeld()) wifiLock.acquire();
            handler.postDelayed(new Runnable() { public void run() {
                try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Throwable t) { /* abaikan */ }
            } }, 90000);
        } catch (Throwable t) { /* abaikan */ }
    }

    private void acquireCpuWake() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;
            if (cpuLock == null) cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rakastrap:cpu");
            cpuLock.acquire(60000);
        } catch (Throwable t) { /* abaikan */ }
    }

    private void acquireDisplayWake() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;
            if (displayLock == null) displayLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "rakastrap:display");
            displayLock.acquire(2 * 60 * 60 * 1000L);
        } catch (Throwable t) { /* abaikan */ }
    }

    private Shell.Result applyGrafResolution() {
        Shell.Result r = new Shell.Result();
        Shell.Result cur = Priv.exec("wm size", false);
        if (cur.code != 0) { r.code = 1; r.out = cur.out; return r; }
        int w = 0, h = 0;
        for (String part : cur.out.split("[^0-9x]+")) {
            if (part.contains("x")) {
                String[] xy = part.split("x");
                try { w = Integer.parseInt(xy[0]); h = Integer.parseInt(xy[1]); } catch (Exception e) { /* abaikan */ }
                if (w > 0 && h > 0) break;
            }
        }
        if (w <= 0 || h <= 0) { r.code = 1; r.out = "PARSE_FAIL"; return r; }
        int ow = prefs.getInt("orig_w", 0), oh = prefs.getInt("orig_h", 0);
        if (ow != w || oh != h) prefs.edit().putInt("orig_w", w).putInt("orig_h", h).apply();
        int targetH = prefs.getInt("graf_res_h", 720);
        if (targetH < 360) targetH = 360;
        if (targetH >= h) { r.code = 0; r.out = "SKIP"; return r; }
        int nw = Math.max(100, Math.round((float) w * targetH / (float) h));
        Shell.Result x = Priv.exec("wm size " + nw + "x" + targetH, false);
        if (x.code == 0) { r.code = 0; r.out = "RES_OK " + nw + "x" + targetH; }
        else { r.code = 1; r.out = x.out; }
        return r;
    }

    private Shell.Result stopBackground() {
        Set<String> keep = new HashSet<>();
        keep.add(getPackageName());
        keep.add(ROBLOX_PKG);
        keep.add("moe.shizuku.privileged.api");
        keep.add("com.google.android.inputmethod.latin");
        keep.add("com.android.inputmethod.latin");
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = getPackageManager().resolveActivity(home, 0);
            if (ri != null && ri.activityInfo != null) keep.add(ri.activityInfo.packageName);
        } catch (Throwable t) { /* abaikan */ }
        StringBuilder sb = new StringBuilder("n=0");
        int n = 0;
        try {
            for (ApplicationInfo ai : getPackageManager().getInstalledApplications(0)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (keep.contains(ai.packageName)) continue;
                sb.append("; am force-stop ").append(ai.packageName).append(" 2>/dev/null && n=$((n+1))");
                n++;
            }
        } catch (Throwable t) { /* abaikan */ }
        sb.append("; echo CANDIDATES:").append(n);
        return Priv.exec(sb.toString(), false);
    }

    private void setDnd(boolean on) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (!nm.isNotificationPolicyAccessGranted()) {
            Toast.makeText(this, "Izin Jangan Ganggu belum diberikan. Kamu diarahkan ke pengaturannya.", Toast.LENGTH_LONG).show();
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); } catch (Throwable t) { /* abaikan */ }
            return;
        }
        nm.setInterruptionFilter(on ? NotificationManager.INTERRUPTION_FILTER_NONE : NotificationManager.INTERRUPTION_FILTER_ALL);
        Toast.makeText(this, on ? "Mode Jangan Ganggu menyala." : "Mode Jangan Ganggu mati.", Toast.LENGTH_SHORT).show();
    }
}
