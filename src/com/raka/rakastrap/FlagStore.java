package com.raka.rakastrap;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

// Tiga lapisan data FastFlag:
// 1) pack bawaan (aset), 2) ubahan manual pengguna di editor, 3) lapisan pengaturan
//    dari tab Grafis & Preset (hanya ikut terkirim bila saklar Terapkan Optimasi menyala).
public class FlagStore {
    public static final String[] TIERS = {"basic", "premium", "vip"};
    public static final String[] TIER_NAMES = {"Basic", "Premium", "VIP"};

    private static final Map<String, LinkedHashMap<String, String>> CACHE = new HashMap<>();

    public static LinkedHashMap<String, String> pack(Context c, String tier) {
        LinkedHashMap<String, String> m = CACHE.get(tier);
        if (m != null) return m;
        m = new LinkedHashMap<>();
        try {
            InputStream is = c.getAssets().open(tier + ".json");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            JSONObject o = new JSONObject(new String(bos.toByteArray(), "UTF-8"));
            ArrayList<String> keys = new ArrayList<>();
            Iterator<String> it = o.keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys);
            for (String k : keys) m.put(k, o.getString(k));
        } catch (Exception e) { /* pack kosong bila aset gagal dibaca */ }
        CACHE.put(tier, m);
        return m;
    }

    // ----- lapisan 2: ubahan manual -----

    private static JSONObject overrides(SharedPreferences p, String tier) {
        try { return new JSONObject(p.getString("ovr_" + tier, "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    public static String value(Context c, SharedPreferences p, String tier, String name) {
        JSONObject ov = overrides(p, tier);
        if (ov.has(name)) { try { return ov.getString(name); } catch (Exception e) { /* abaikan */ } }
        String base = pack(c, tier).get(name);
        return base == null ? "" : base;
    }

    public static boolean isOverridden(SharedPreferences p, String tier, String name) {
        return overrides(p, tier).has(name);
    }

    public static boolean isCustom(Context c, SharedPreferences p, String tier, String name) {
        return isOverridden(p, tier, name) && !pack(c, tier).containsKey(name);
    }

    public static int overrideCount(SharedPreferences p, String tier) {
        return overrides(p, tier).length();
    }

    public static void setOverride(SharedPreferences p, String tier, String name, String val) {
        JSONObject ov = overrides(p, tier);
        try { ov.put(name, val); } catch (Exception e) { /* abaikan */ }
        p.edit().putString("ovr_" + tier, ov.toString()).apply();
    }

    public static void clearOverride(SharedPreferences p, String tier, String name) {
        JSONObject ov = overrides(p, tier);
        ov.remove(name);
        p.edit().putString("ovr_" + tier, ov.toString()).apply();
    }

    public static void clearAllOverrides(SharedPreferences p, String tier) {
        p.edit().remove("ovr_" + tier).apply();
    }

    // Daftar nama untuk editor: isi pack + flag tambahan kustom, terurut.
    public static ArrayList<String> listNames(Context c, SharedPreferences p, String tier) {
        TreeSet<String> s = new TreeSet<>(pack(c, tier).keySet());
        JSONObject ov = overrides(p, tier);
        Iterator<String> it = ov.keys();
        while (it.hasNext()) s.add(it.next());
        return new ArrayList<>(s);
    }

    // ----- lapisan 3: pengaturan Grafis & Preset -----

    public static JSONObject settings(SharedPreferences p) {
        try { return new JSONObject(p.getString("settings_json", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    private static void saveSettings(SharedPreferences p, JSONObject s) {
        p.edit().putString("settings_json", s.toString()).apply();
    }

    public static boolean hasSetting(SharedPreferences p, String key) {
        return settings(p).has(key);
    }

    public static String getSetting(SharedPreferences p, String key) {
        JSONObject s = settings(p);
        if (s.has(key)) { try { return s.getString(key); } catch (Exception e) { /* abaikan */ } }
        return null;
    }

    public static void setSetting(SharedPreferences p, String key, String val) {
        JSONObject s = settings(p);
        try { s.put(key, val); } catch (Exception e) { /* abaikan */ }
        saveSettings(p, s);
    }

    public static void clearSetting(SharedPreferences p, String key) {
        JSONObject s = settings(p);
        s.remove(key);
        saveSettings(p, s);
    }

    public static void applyBundle(SharedPreferences p, Map<String, String> bundle, boolean on) {
        JSONObject s = settings(p);
        try {
            for (Map.Entry<String, String> e : bundle.entrySet()) {
                if (on) s.put(e.getKey(), e.getValue());
                else s.remove(e.getKey());
            }
        } catch (Exception e) { /* abaikan */ }
        saveSettings(p, s);
    }

    // Nilai efektif yang akan terkirim: pengaturan > ubahan manual > pack.
    public static String effValue(Context c, SharedPreferences p, String tier, String name) {
        if (p.getBoolean("apply_opt", true)) {
            String s = getSetting(p, name);
            if (s != null) return s;
        }
        return value(c, p, tier, name);
    }

    // JSON final siap ditempel / diimpor ke MasterStrap.
    public static String buildJson(Context c, SharedPreferences p, String tier) {
        LinkedHashMap<String, String> base = pack(c, tier);
        JSONObject ov = overrides(p, tier);
        JSONObject out = new JSONObject();
        try {
            for (Map.Entry<String, String> e : base.entrySet()) {
                String v = ov.has(e.getKey()) ? ov.getString(e.getKey()) : e.getValue();
                out.put(e.getKey(), v);
            }
            Iterator<String> it = ov.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (!base.containsKey(k)) out.put(k, ov.getString(k));
            }
            if (p.getBoolean("apply_opt", true)) {
                JSONObject s = settings(p);
                Iterator<String> sit = s.keys();
                while (sit.hasNext()) {
                    String k = sit.next();
                    out.put(k, s.getString(k));
                }
            }
        } catch (Exception e) { /* abaikan */ }
        return out.toString();
    }

    public static String tierName(String tier) {
        for (int i = 0; i < TIERS.length; i++) if (TIERS[i].equals(tier)) return TIER_NAMES[i];
        return tier;
    }
}
