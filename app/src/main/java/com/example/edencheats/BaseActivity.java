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
            help = "Atual: " + SafStore.describe(this) + "\n\n"
                    + "No Android 13 ou superior o sistema bloqueia a pasta Android/data para apps. "
                    + "Escolha uma pasta comum (por exemplo Documentos/EdenMods): o app cria nela "
                    + "load/<TitleID>/<Mod>/. Depois copie a pasta 'load' para dentro da pasta do Eden com um "
                    + "gerenciador de arquivos que tenha acesso, ou instale o mod pelo Eden "
                    + "(Propriedades do jogo > Add-ons).";
        } else {
            help = "Atual: " + SafStore.describe(this) + "\n\n"
                    + "Na próxima tela abra Android > data > pacote do Eden > files e toque em "
                    + "\"Usar esta pasta\". O app grava em load/<TitleID>/<Mod>/. "
                    + "Se o Android não deixar usar essa pasta, escolha uma pasta comum e copie 'load' depois.";
        }
        box.setBackgroundColor(Ui.BG);
        TextView tv = Ui.text(this, help, 13, Ui.TEXT, false);
        box.addView(tv);
        TextView lbl = Ui.text(this, "\nPacote do Eden:", 12, Ui.MUTED, true);
        box.addView(lbl);
        final EditText pkg = Ui.monoEdit(this, SafStore.DEFAULT_PKG);
        pkg.setText(SafStore.pkg(this));
        box.addView(pkg);

        new AlertDialog.Builder(this)
                .setTitle("Pasta de destino")
                .setView(box)
                .setPositiveButton("Escolher pasta", (d, w) -> {
                    SafStore.setPkg(this, pkg.getText().toString());
                    SafStore.pick(this, REQ_FOLDER);
                })
                .setNegativeButton("Cancelar", null)
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
                    if (o == null) throw new java.io.IOException("Não foi possível abrir o arquivo de destino.");
                    ZipOutputStream z = new ZipOutputStream(o);
                    for (Map.Entry<String, byte[]> e : files.entrySet()) {
                        z.putNextEntry(new ZipEntry(e.getKey()));
                        z.write(e.getValue());
                        z.closeEntry();
                    }
                    z.finish();
                    msg = "ZIP salvo. Extraia dentro da pasta load do Eden.";
                } catch (Exception e) {
                    msg = "Erro ao salvar o ZIP: " + e.getMessage();
                }
                final String m = msg;
                runOnUiThread(() -> Ui.toast(BaseActivity.this, m));
            });
        }
    }
}
