package com.example.edencheats;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
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

/** Instala um cheat colado manualmente (ex.: copiado de fóruns como o GBAtemp). */
public class CustomActivity extends BaseActivity {
    private EditText tid;
    private EditText bid;
    private EditText mod;
    private EditText body;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.page(this);
        sv.addView(root);

        root.addView(Ui.text(this, "Colar código", 20, Ui.TEXT, true));
        root.addView(Ui.text(this, "Cole um cheat no formato [Nome] + linhas hexadecimais e informe o "
                + "Title ID e o Build ID do seu jogo.", 13, Ui.MUTED, false));

        tid = field(root, "Title ID (16 caracteres)");
        bid = field(root, "Build ID (16 caracteres)");
        mod = field(root, "Nome do mod");
        mod.setText("Cheats");

        body = Ui.edit(this, "[Infinite Health]\n04000000 003EA360 D65F03C0");
        body.setSingleLine(false);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.setMinLines(8);
        body.setGravity(Gravity.TOP | Gravity.START);
        root.addView(body, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));

        status = Ui.text(this, "", 13, Ui.MUTED, false);
        root.addView(status);

        Button install = Ui.button(this, "Instalar", v -> go(true));
        Button export = Ui.button(this, "Exportar ZIP", v -> go(false));
        Button folder = Ui.button(this, "Pasta", v -> folderDialog());
        root.addView(Ui.buttonRow(this, install, export, folder));
        setContentView(sv);
    }

    private EditText field(LinearLayout root, String hint) {
        EditText e = Ui.edit(this, hint);
        root.addView(e, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2));
        return e;
    }

    private void go(boolean install) {
        String t = tid.getText().toString().trim().toUpperCase(Locale.ROOT);
        String bd = bid.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(t) || !Util.isHex16(bd)) {
            Ui.toast(this, "Title ID e Build ID precisam ter 16 caracteres hexadecimais.");
            return;
        }
        List<CheatParser.Block> blocks = CheatParser.parse(body.getText().toString());
        if (blocks.isEmpty()) {
            Ui.toast(this, "Nenhum cheat reconhecido. Cada cheat começa com uma linha [Nome].");
            return;
        }
        if (install && !ensureFolder()) return;
        List<CheatParser.Block> masters = new ArrayList<>();
        List<CheatParser.Block> cheats = new ArrayList<>();
        for (CheatParser.Block b : blocks) {
            if (b.master) masters.add(b);
            else cheats.add(b);
        }
        if (cheats.isEmpty()) cheats.addAll(masters);

        String userMod = mod != null ? mod.getText().toString().trim() : "";
        final Map<String, byte[]> files = new LinkedHashMap<>();
        for (CheatParser.Block b : cheats) {
            String modName;
            if (cheats.size() == 1 && !userMod.isEmpty() && !userMod.equalsIgnoreCase("Cheats")) {
                modName = Util.sanitize(userMod);
            } else {
                modName = Util.sanitize(b.name());
            }
            String singleText = CheatParser.buildSingle(b, b.master ? null : masters);
            String dest = modName + "/cheats/" + bd + ".txt";
            files.put(dest, singleText.getBytes(StandardCharsets.UTF_8));
        }

        final String tidF = t;
        if (install) {
            status.setText("Instalando…");
            Util.POOL.execute(() -> {
                String msg;
                try {
                    msg = "Pronto: " + SafStore.install(this, tidF, files);
                } catch (Exception e) {
                    msg = "Erro: " + e.getMessage();
                }
                final String s = msg;
                runOnUiThread(() -> status.setText(s));
            });
        } else {
            Map<String, byte[]> zip = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                zip.put(tidF + "/" + e.getKey(), e.getValue());
            }
            exportZip(tidF + "_cheats.zip", zip);
        }
    }
}
