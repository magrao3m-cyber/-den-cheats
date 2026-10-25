package com.example.edencheats;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Funções comuns: escolher pasta de destino e exportar ZIP. */
public abstract class BaseActivity extends AppCompatActivity {
    static final int REQ_FOLDER = 11;
    static final int REQ_EXPORT = 12;

    private Map<String, byte[]> pendingExport;

    /** Aplica o idioma escolhido pelo usuário (Português, English ou Español). */
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(Lang.wrap(newBase));
    }

    // ---- Transições suaves entre telas e entrada animada dos blocos ----

    private boolean isOwnScreen(Intent intent) {
        return intent != null && intent.getComponent() != null
                && getPackageName().equals(intent.getComponent().getPackageName());
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, android.os.Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        if (isOwnScreen(intent) && Ui.animationsEnabled(this)) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (Ui.animationsEnabled(this)) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    public void setContentView(android.view.View view) {
        super.setContentView(view);
        android.view.View target = view;
        if (view instanceof android.widget.ScrollView) {
            target = ((android.widget.ScrollView) view).getChildAt(0);
        }
        if (target instanceof android.view.ViewGroup) Ui.staggerIn((android.view.ViewGroup) target);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        android.view.View content = findViewById(android.R.id.content);
        if (content instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) content;
            if (g.getChildCount() > 0 && g.getChildAt(0) instanceof android.view.ViewGroup) {
                Ui.staggerIn((android.view.ViewGroup) g.getChildAt(0));
            }
        }
    }

    /**
     * O AppCompat reaplica a configuração do sistema por cima do contexto; sem isto, os textos dos
     * layouts XML (@string) voltariam ao idioma do aparelho em vez do idioma escolhido no app.
     */
    @Override
    public void applyOverrideConfiguration(android.content.res.Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            overrideConfiguration.setLocale(Lang.locale());
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    /** Chamado depois que o usuário escolhe uma pasta de destino. */
    protected void onFolderChanged() {}

    boolean ensureFolder() {
        if (SafStore.getTree(this) != null) return true;
        folderDialog();
        return false;
    }

    void folderDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, p / 2, p, 0);

        String help;
        if (Build.VERSION.SDK_INT >= 33) {
            help = Lang.s(R.string.main_folder_help_new, SafStore.describe(this));
        } else {
            help = Lang.s(R.string.main_folder_help_old, SafStore.describe(this));
        }
        TextView tv = Ui.text(this, help, 13, Ui.TEXT, false);
        box.addView(tv);
        TextView lbl = Ui.text(this, "\n" + Lang.s(R.string.main_eden_package_label), 12, Ui.MUTED, true);
        box.addView(lbl);
        final EditText pkg = Ui.monoEdit(this, SafStore.DEFAULT_PKG);
        pkg.setText(SafStore.pkg(this));
        box.addView(pkg);

        new AlertDialog.Builder(this)
                .setTitle(Lang.s(R.string.main_folder_title))
                .setView(box)
                .setPositiveButton(Lang.s(R.string.main_folder_choose), (d, w) -> {
                    SafStore.setPkg(this, pkg.getText().toString());
                    SafStore.pick(this, REQ_FOLDER);
                })
                .setNegativeButton(Lang.s(R.string.main_cancel), null)
                .show();
    }

    /** Salva os arquivos num ZIP escolhido pelo usuário (chaves = caminhos dentro do ZIP). */
    void exportZip(String fileName, Map<String, byte[]> files) {
        pendingExport = files;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_FOLDER) {
            if (res == RESULT_OK && data != null) {
                Ui.toast(this, SafStore.onPicked(this, data));
                onFolderChanged();
            }
        } else if (req == REQ_EXPORT) {
            final Map<String, byte[]> files = pendingExport;
            pendingExport = null;
            if (res != RESULT_OK || data == null || data.getData() == null || files == null) return;
            final Uri target = data.getData();
            Util.POOL.execute(() -> {
                String msg;
                try (OutputStream o = getContentResolver().openOutputStream(target)) {
                    if (o == null) throw new java.io.IOException(Lang.s(R.string.main_open_target_failed));
                    ZipOutputStream z = new ZipOutputStream(o);
                    for (Map.Entry<String, byte[]> e : files.entrySet()) {
                        z.putNextEntry(new ZipEntry(e.getKey()));
                        z.write(e.getValue());
                        z.closeEntry();
                    }
                    z.finish();
                    msg = Lang.s(R.string.main_zip_saved);
                } catch (Exception e) {
                    msg = Lang.s(R.string.main_zip_error, e.getMessage());
                }
                final String m = msg;
                runOnUiThread(() -> Ui.toast(BaseActivity.this, m));
            });
        }
    }
}
