package com.example.edencheats;

import android.app.Application;
import android.content.Context;

/** Application: guarda o contexto global para o sistema de idiomas (Lang). */
public class EdenApp extends Application {
    private static EdenApp instance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Lang.init(this);
        Ui.initTheme(this);
    }

    static Context get() {
        return instance;
    }
}
