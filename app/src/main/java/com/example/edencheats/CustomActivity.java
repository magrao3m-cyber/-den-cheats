package com.example.edencheats;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
    private TextView previewStatus;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.page(this);
        sv.addView(root);

        // Header Card
        LinearLayout headCard = new LinearLayout(this);
        headCard.setOrientation(LinearLayout.VERTICAL);
        headCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        headCard.setPadding(p, p, p, p);
        headCard.addView(Ui.text(this, "◈ COLAR CÓDIGO MANUAL", 19, Ui.TEXT, true));
        headCard.addView(Ui.text(this, "Instale cheats copiados de fóruns ou Discord diretamente no Eden", 12, Ui.MUTED, false));
        root.addView(headCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        TextView tidLbl = Ui.text(this, "TITLE ID (16 CARACTERES HEX)", 11, Ui.MUTED, true);
        tidLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(tidLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        tid = Ui.monoEdit(this, "Ex.: 0100A6301214E000");
        root.addView(tid, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        TextView bidLbl = Ui.text(this, "BUILD ID DA VERSÃO (16 CARACTERES HEX)", 11, Ui.MUTED, true);
        bidLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(bidLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        bid = Ui.monoEdit(this, "Ex.: E7E1382D5EB931B0");
        root.addView(bid, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        // Botão de colar com 1 toque
        Button pasteBtn = Ui.button(this, "📋 Colar da Área de Transferência", false, v -> pasteFromClipboard());
        root.addView(pasteBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        TextView codeLbl = Ui.text(this, "CÓDIGO HEXADECIMAL DO CHEAT", 11, Ui.MUTED, true);
        codeLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(codeLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        body = Ui.monoEdit(this, "[Nome do Cheat]\n58000000 00C88A70\n78001000 00000090");
        body.setSingleLine(false);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.setMinLines(7);
        body.setGravity(Gravity.TOP | Gravity.START);
        root.addView(body, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        previewStatus = Ui.text(this, "Dica: Cada cheat começa com [Nome do Cheat]. Códigos na mesma linha são ajustados automaticamente.",
                11, Ui.MUTED, false);
        root.addView(previewStatus, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        body.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override
            public void afterTextChanged(Editable e) {
                List<CheatParser.Block> bl = CheatParser.parse(e.toString());
                if (bl.isEmpty()) {
                    previewStatus.setText("Dica: Cole um ou mais cheats iniciando com [Nome do Cheat].");
                    previewStatus.setTextColor(Ui.MUTED);
                } else {
                    StringBuilder sb = new StringBuilder("⚡ " + bl.size() + " cheat(s) detectado(s): ");
                    for (int i = 0; i < Math.min(bl.size(), 3); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("[").append(bl.get(i).name()).append("]");
                    }
                    if (bl.size() > 3) sb.append("...");
                    previewStatus.setText(sb.toString());
                    previewStatus.setTextColor(Ui.GOOD);
                }
            }
        });

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        root.addView(status, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        Button install = Ui.button(this, "🚀 INSTALAR NO EDEN", true, v -> go(true));
        root.addView(install, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        Button export = Ui.button(this, "📦 Exportar ZIP", false, v -> go(false));
        Button folder = Ui.button(this, "📁 Pasta", false, v -> folderDialog());
        root.addView(Ui.buttonRow(this, export, folder));

        setContentView(sv);
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    body.setText(text);
                    Ui.toast(this, "Código colado da área de transferência!");
                }
            }
        } catch (Exception e) {
            Ui.toast(this, "Erro ao acessar área de transferência.");
        }
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
            Ui.toast(this, "Nenhum cheat reconhecido. Cada cheat começa com [Nome].");
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
                    msg = "✔ " + SafStore.install(this, tidF, files);
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
