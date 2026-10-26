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

        // Cabeçalho com botão voltar
        root.addView(Ui.headerCard(this, Lang.s(R.string.items_cus_title), Lang.s(R.string.items_cus_subtitle), Ui.MUTED), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        TextView tidLbl = Ui.text(this, Lang.s(R.string.items_cus_tid_label), 11, Ui.MUTED, true);
        tidLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(tidLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        tid = Ui.monoEdit(this, Lang.s(R.string.items_cus_tid_hint));
        root.addView(tid, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        TextView bidLbl = Ui.text(this, Lang.s(R.string.items_cus_bid_label), 11, Ui.MUTED, true);
        bidLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(bidLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        bid = Ui.monoEdit(this, Lang.s(R.string.items_cus_bid_hint));
        root.addView(bid, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        // Botão de colar com 1 toque
        Button pasteBtn = Ui.button(this, Lang.s(R.string.items_cus_paste), false, v -> pasteFromClipboard());
        root.addView(pasteBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        TextView codeLbl = Ui.text(this, Lang.s(R.string.items_cus_code_label), 11, Ui.MUTED, true);
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

        previewStatus = Ui.text(this, Lang.s(R.string.items_cus_tip),
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
                    previewStatus.setText(Lang.s(R.string.items_cus_tip_empty));
                    previewStatus.setTextColor(Ui.MUTED);
                } else {
                    StringBuilder sb = new StringBuilder(Lang.s(R.string.items_cus_detected, bl.size()) + ": ");
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

        Button install = Ui.button(this, Lang.s(R.string.items_cus_install_btn), true, v -> go(true));
        root.addView(install, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        Button export = Ui.button(this, Lang.s(R.string.items_cus_export), false, v -> go(false));
        Button folder = Ui.button(this, Lang.s(R.string.items_cus_folder), false, v -> folderDialog());
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
                    Ui.toast(this, Lang.s(R.string.items_cus_pasted));
                }
            }
        } catch (Exception e) {
            Ui.toast(this, Lang.s(R.string.items_cus_clip_err));
        }
    }

    private void go(boolean install) {
        String t = tid.getText().toString().trim().toUpperCase(Locale.ROOT);
        String bd = bid.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(t) || !Util.isHex16(bd)) {
            Ui.toast(this, Lang.s(R.string.items_cus_hex_both));
            return;
        }
        List<CheatParser.Block> blocks = CheatParser.parse(body.getText().toString());
        if (blocks.isEmpty()) {
            Ui.toast(this, Lang.s(R.string.items_cus_none));
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
            String singleText = CheatParser.buildSingle(b, null);
            String dest = modName + "/cheats/" + bd + ".txt";
            files.put(dest, singleText.getBytes(StandardCharsets.UTF_8));
        }
        // Master Code uma única vez (o Eden limita o total de cheats ativos a 1024 palavras).
        CheatParser.Block mergedMaster = CheatParser.mergeMasters(masters);
        if (mergedMaster != null && !mergedMaster.lines.isEmpty() && !masters.equals(cheats)) {
            files.put(CheatParser.MASTER_FOLDER + "/cheats/" + bd + ".txt",
                    CheatParser.buildMasterFile(mergedMaster).getBytes(StandardCharsets.UTF_8));
        }

        final String tidF = t;
        if (install) {
            status.setText(Lang.s(R.string.items_cus_installing));
            Util.POOL.execute(() -> {
                String msg;
                try {
                    msg = "✔ " + SafStore.install(this, tidF, files);
                } catch (Exception e) {
                    msg = Lang.s(R.string.items_error, e.getMessage());
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
