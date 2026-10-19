package com.example.edencheats;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mostra um arquivo de cheats (com seleção individual) ou um mod, e instala/exporta. */
public class ItemActivity extends BaseActivity {
    private Model.Game game;
    private Model.Item item;
    private EditText tidField;
    private EditText bidField;
    private EditText modField;
    private TextView modLabel;
    private CheckBox separateFoldersCb;
    private TextView status;
    private LinearLayout cheatList;
    private final List<CheatParser.Block> blocks = new ArrayList<>();
    private final List<CheckBox> boxes = new ArrayList<>();
    private boolean loaded = false;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        game = Session.game;
        item = Session.item;
        if (game == null || item == null) {
            finish();
            return;
        }
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.page(this);
        sv.addView(root);

        root.addView(Ui.text(this, game.display(), 20, Ui.TEXT, true));
        root.addView(Ui.text(this, item.title, 14, Ui.MUTED, false));

        root.addView(Ui.text(this, "Title ID (16 caracteres)", 12, Ui.MUTED, false));
        tidField = Ui.edit(this, "16 caracteres, ex.: 0100BA4026E94000");
        tidField.setText(game.tid);
        root.addView(tidField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));

        root.addView(Ui.text(this, "Build ID (16 caracteres, maiúsculas)", 12, Ui.MUTED, false));
        bidField = Ui.edit(this, "16 caracteres, ex.: E7E1382D5EB931B0");
        String initialBid = Session.targetBuildId;
        if ((initialBid == null || !Util.isHex16(initialBid)) && item.bid != null && !item.bid.isEmpty()) {
            initialBid = item.bid;
        }
        if (initialBid != null) bidField.setText(initialBid.toUpperCase(Locale.ROOT));
        root.addView(bidField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));

        if (item.cheat) {
            separateFoldersCb = new CheckBox(this);
            separateFoldersCb.setText("Criar uma pasta para cada cheat (recomendado para Eden)");
            separateFoldersCb.setTextColor(Ui.TEXT);
            separateFoldersCb.setChecked(true);
            separateFoldersCb.setOnCheckedChangeListener((cb, checked) -> {
                if (modField != null) modField.setVisibility(checked ? View.GONE : View.VISIBLE);
                if (modLabel != null) modLabel.setVisibility(checked ? View.GONE : View.VISIBLE);
            });
            root.addView(separateFoldersCb);

            modLabel = Ui.text(this, "Nome da pasta do mod (agrupa todos os cheats)", 12, Ui.MUTED, false);
            modLabel.setVisibility(View.GONE);
            root.addView(modLabel);

            modField = Ui.edit(this, "Cheats");
            modField.setText(item.modName != null ? item.modName : "Cheats");
            modField.setVisibility(View.GONE);
            root.addView(modField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));
        } else {
            root.addView(Ui.text(this, "Nome da pasta do mod (aparece em Add-ons no Eden)", 12, Ui.MUTED, false));
            modField = Ui.edit(this, "Mod");
            modField.setText(item.modName != null ? item.modName : "Mod");
            root.addView(modField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));
        }

        status = Ui.text(this, "", 13, Ui.MUTED, false);
        root.addView(status);

        if (item.cheat) {
            Button all = Ui.button(this, "Marcar todos", v -> setAll(true));
            Button none = Ui.button(this, "Desmarcar todos", v -> setAll(false));
            root.addView(Ui.buttonRow(this, all, none));
            cheatList = new LinearLayout(this);
            cheatList.setOrientation(LinearLayout.VERTICAL);
            root.addView(cheatList);
        } else {
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (Model.FileRef f : item.files) {
                if (n++ >= 60) {
                    sb.append("… e mais ").append(item.files.size() - 60).append(" arquivo(s)");
                    break;
                }
                sb.append(f.dest).append('\n');
            }
            root.addView(Ui.text(this, item.files.size() + " arquivo(s) · " + Util.mb(item.totalSize())
                    + "\n\n" + sb, 12, Ui.TEXT, false));
        }

        Button install = Ui.button(this, "Instalar no Eden", v -> doInstall());
        Button export = Ui.button(this, "Exportar ZIP", v -> doExport());
        Button folder = Ui.button(this, "Pasta", v -> folderDialog());
        root.addView(Ui.buttonRow(this, install, export, folder));

        setContentView(sv);
        if (item.cheat) loadCheats();
    }

    private void setStatus(final String s) {
        runOnUiThread(() -> status.setText(s));
    }

    private void setAll(boolean v) {
        for (CheckBox c : boxes) c.setChecked(v);
    }

    private void loadCheats() {
        status.setText("Lendo cheats…");
        Util.POOL.execute(() -> {
            try {
                byte[] data = Sources.read(item, item.files.get(0));
                final List<CheatParser.Block> parsed = CheatParser.parse(new String(data, StandardCharsets.UTF_8));
                runOnUiThread(() -> {
                    blocks.clear();
                    blocks.addAll(parsed);
                    for (CheatParser.Block bl : parsed) {
                        CheckBox cb = new CheckBox(this);
                        String label = bl.name() + (bl.master ? "  (código mestre)" : "");
                        cb.setText(label);
                        cb.setTextColor(Ui.TEXT);
                        cb.setChecked(true);
                        boxes.add(cb);
                        cheatList.addView(cb);
                    }
                    loaded = true;
                    status.setText(parsed.size() + " cheat(s) neste arquivo. Desmarque os que não quiser.");
                });
            } catch (final Exception e) {
                setStatus("Erro ao ler os cheats: " + e.getMessage());
            }
        });
    }

    private String currentTid() {
        String tid = tidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(tid)) {
            Ui.toast(this, "Informe o Title ID com 16 caracteres hexadecimais.");
            return null;
        }
        return tid;
    }

    private String currentBid() {
        String bid = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(bid)) {
            Ui.toast(this, "Informe o Build ID com 16 caracteres hexadecimais.");
            return null;
        }
        return bid;
    }

    private Map<String, byte[]> prepare(Util.Progress p) throws java.io.IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        String bid = currentBid();
        if (bid == null) return null;

        if (item.cheat) {
            List<CheatParser.Block> sel = new ArrayList<>();
            List<CheatParser.Block> masters = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                CheatParser.Block b = blocks.get(i);
                if (b.master) masters.add(b);
                if (boxes.get(i).isChecked()) sel.add(b);
            }
            if (sel.isEmpty()) return null;

            boolean separate = separateFoldersCb != null && separateFoldersCb.isChecked();
            if (separate) {
                for (CheatParser.Block b : sel) {
                    String folder = Util.sanitize(b.name());
                    String dest = folder + "/cheats/" + bid + ".txt";
                    String content = CheatParser.buildSingle(b, b.master ? null : masters);
                    out.put(dest, content.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                String mod = Util.sanitize(modField.getText().toString());
                String dest = mod + "/cheats/" + bid + ".txt";
                String content = CheatParser.build(sel);
                out.put(dest, content.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            String mod = Util.sanitize(modField.getText().toString());
            int n = item.files.size();
            int i = 0;
            for (Model.FileRef r : item.files) {
                i++;
                p.onProgress("Baixando " + i + "/" + n + "…");
                out.put(mod + "/" + r.dest, Sources.read(item, r));
            }
        }
        return out;
    }

    private void start(final boolean install) {
        if (busy) return;
        final String tid = currentTid();
        if (tid == null) return;
        final String bid = currentBid();
        if (bid == null) return;

        if (item.cheat && !loaded) {
            Ui.toast(this, "Aguarde a leitura dos cheats.");
            return;
        }
        if (install && !ensureFolder()) return;

        if (!item.cheat && item.totalSize() > 50L * 1048576L) {
            new AlertDialog.Builder(this)
                    .setTitle("Mod grande")
                    .setMessage("Este mod tem " + Util.mb(item.totalSize()) + ". Baixar mesmo assim?")
                    .setPositiveButton("Baixar", (d, w) -> run(install, tid))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            run(install, tid);
        }
    }

    private void run(final boolean install, final String tid) {
        busy = true;
        status.setText("Preparando…");
        Util.POOL.execute(() -> {
            try {
                final Map<String, byte[]> files = prepare(this::setStatus);
                if (files == null || files.isEmpty()) {
                    runOnUiThread(() -> Ui.toast(this, "Selecione ao menos um cheat."));
                    return;
                }
                if (install) {
                    String msg = SafStore.install(this, tid, files);
                    setStatus("Pronto: " + msg + "\nAbra o jogo no Eden e ative os cheats em Propriedades > Add-ons.");
                } else {
                    final Map<String, byte[]> zipFiles = new LinkedHashMap<>();
                    for (Map.Entry<String, byte[]> e : files.entrySet()) {
                        zipFiles.put(tid + "/" + e.getKey(), e.getValue());
                    }
                    runOnUiThread(() -> {
                        status.setText("Escolha onde salvar o ZIP…");
                        exportZip(tid + "_cheats.zip", zipFiles);
                    });
                }
            } catch (Exception e) {
                setStatus("Erro: " + e.getMessage());
            } finally {
                busy = false;
            }
        });
    }

    private void doInstall() {
        start(true);
    }

    private void doExport() {
        start(false);
    }
}
