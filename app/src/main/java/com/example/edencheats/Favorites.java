package com.example.edencheats;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Jogos favoritos (toque longo na lista): aparecem primeiro e com uma estrela. */
final class Favorites {
    private Favorites() {}

    private static final String PREFS = "eden_favorites";
    private static final String KEY = "tids";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Cópia do conjunto de Title IDs favoritos. */
    static Set<String> all(Context c) {
        return new HashSet<>(prefs(c).getStringSet(KEY, new HashSet<>()));
    }

    /** Marca ou desmarca. Devolve true se o jogo agora é favorito. */
    static boolean toggle(Context c, String tid) {
        if (tid == null || tid.isEmpty()) return false;
        Set<String> set = all(c);
        boolean now = !set.remove(tid);
        if (now) set.add(tid);
        prefs(c).edit().putStringSet(KEY, set).apply();
        return now;
    }
}
