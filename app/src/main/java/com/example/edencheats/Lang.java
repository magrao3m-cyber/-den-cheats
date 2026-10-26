package com.example.edencheats;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sistema de idiomas do SwitchForge.
 * Idiomas: Português (padrão), English e Español. A escolha é salva e vale para todo o app.
 *
 * Uso nos textos: {@code Lang.s(R.string.nome)} ou {@code Lang.s(R.string.nome, arg1, arg2)}.
 * Funciona em qualquer classe (não precisa de Context). Os textos ficam em
 * res/values (PT), res/values-en (EN) e res/values-es (ES).
 */
final class Lang {
    private Lang() {}

    static final String PT = "pt";
    static final String EN = "en";
    static final String ES = "es";
    static final String[] CODES = {PT, EN, ES};
    /** Nomes de cada idioma escritos no próprio idioma (não são traduzidos). */
    static final String[] NAMES = {"Português", "English", "Español"};

    private static final String PREFS = "lang_prefs";
    private static final String KEY = "lang";

    private static volatile String current = PT;
    private static final Map<String, Context> CACHE = new HashMap<>();

    /** Carrega o idioma salvo. Chamado uma vez ao abrir o app. */
    static void init(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        current = valid(sp.getString(KEY, PT));
    }

    private static String valid(String code) {
        for (String c : CODES) if (c.equals(code)) return c;
        return PT;
    }

    /** Código do idioma atual: "pt", "en" ou "es". */
    static String code() {
        return current;
    }

    static Locale locale() {
        return localeOf(current);
    }

    private static Locale localeOf(String code) {
        switch (code) {
            case EN: return new Locale("en");
            case ES: return new Locale("es");
            default: return new Locale("pt", "BR");
        }
    }

    /** Nome do idioma atual (para mostrar no seletor). */
    static String currentName() {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(current)) return NAMES[i];
        return NAMES[0];
    }

    /** Sigla curta do idioma atual em maiúsculas (PT, EN, ES). */
    static String shortCode() {
        return current.toUpperCase(Locale.ROOT);
    }

    /** Aplica o idioma escolhido a um Context (usado pelas Activities em attachBaseContext). */
    static Context wrap(Context base) {
        Locale l = locale();
        Locale.setDefault(l);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(l);
        return base.createConfigurationContext(cfg);
    }

    private static Context localized() {
        Context app = EdenApp.get();
        if (app == null) throw new IllegalStateException("EdenApp não inicializado");
        String code = current;
        synchronized (CACHE) {
            Context c = CACHE.get(code);
            if (c == null) {
                Configuration cfg = new Configuration(app.getResources().getConfiguration());
                cfg.setLocale(localeOf(code));
                c = app.createConfigurationContext(cfg);
                CACHE.put(code, c);
            }
            return c;
        }
    }

    /**
     * Texto traduzido no idioma atual. Sempre formata com String.format, então um
     * "%" literal deve ser escrito como "%%" nos arquivos strings.xml.
     */
    static String s(int resId, Object... args) {
        return localized().getString(resId, args);
    }

    /** Salva o idioma e reinicia o app na tela inicial para aplicar em todas as telas. */
    static void set(Activity a, String code) {
        String newCode = valid(code);
        if (newCode.equals(current)) return;
        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, newCode).apply();
        current = newCode;
        synchronized (CACHE) {
            CACHE.clear();
        }
        Intent i = new Intent(a, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
        a.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** Caixa de diálogo para o usuário escolher o idioma. */
    static void showPicker(Activity a) {
        int checked = 0;
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(current)) checked = i;
        new AlertDialog.Builder(a)
                .setTitle(s(R.string.lang_picker_title))
                .setSingleChoiceItems(NAMES, checked, (d, which) -> {
                    d.dismiss();
                    set(a, CODES[which]);
                })
                .setNegativeButton(s(R.string.lang_cancel), null)
                .show();
    }
}
