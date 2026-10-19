package com.example.edencheats;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detalhes do jogo selecionado:
 * - Identifica com precisão as versões amigáveis (ex.: v1.3.0 Mais Recente, v1.0.0 Base).
 * - Exibe a lista completa de cheats disponíveis para a versão selecionada.
 * - Permite selecionar os cheats desejados e instalar no Eden com 1 toque.
 * - Detecta automaticamente ou via arquivo da ROM (.nsp / .xci).
 */
public class DetailActivity extends BaseActivity {
    private static final int REQ_PICK_ROM = 31;

    private Model.Game game;
    private EditText bidField;
    private Ui.RowAdapter adapter;
    private final List<Model.Item> shown = new ArrayList<>();
    private final List<String> availableBids = new ArrayList<>();
    private String detectedBid = null;

    private LinearLayout chipsContainer;
    private LinearLayout cheatPreviewCard;
    private TextView cheatSectionTitle;
    private TextView cheatLoadingText;
    private LinearLayout cheatsContainer;
    private Button installNowBtn;

    private final List<CheatParser.Block> currentBlocks = new ArrayList<>();
    private final List<CheckBox> cheatBoxes = new ArrayList<>();
    private Model.Item activeCheatItem = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        game = Session.game;
        if (game == null) {
            finish();
            return;
        }

        // Extrair todos os Build IDs distintos disponíveis neste jogo
        for (Model.Item it : game.items) {
            if (it.cheat && !it.bid.isEmpty() && !availableBids.contains(it.bid)) {
                availableBids.add(it.bid);
            }
        }

        // 1. Consultar se já existe detecção pelo EdenScanner
        detectedBid = EdenScanner.getBuildId(this, game.tid);
        String detectedVer = EdenScanner.getVersion(this, game.tid);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);

        LinearLayout root = Ui.page(this);
        sv.addView(root);

        // Header Card do Jogo
        LinearLayout gameCard = new LinearLayout(this);
        gameCard.setOrientation(LinearLayout.VERTICAL);
        gameCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        gameCard.setPadding(p, p, p, p);

        gameCard.addView(Ui.text(this, game.display(), 19, Ui.TEXT, true));

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        int mt = Ui.dp(this, 6);
        metaRow.setPadding(0, mt, 0, 0);

        String tidStr = game.tid.isEmpty() ? "TID DESCONHECIDO" : game.tid;
        metaRow.addView(Ui.badge(this, tidStr, Ui.ACCENT_BG, Ui.ACCENT, Ui.BORDER_GLOW));

        if (detectedBid != null) {
            LinearLayout.LayoutParams lpEden = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpEden.setMargins(Ui.dp(this, 6), 0, 0, 0);
            String label = "⚡ EDEN: " + VersionMap.getVersionLabel(game.tid, detectedBid, null, availableBids);
            TextView edenBadge = Ui.badge(this, label, Ui.GOOD_BG, Ui.GOOD, Ui.GOOD);
            metaRow.addView(edenBadge, lpEden);
        } else if (detectedVer != null && !detectedVer.isEmpty()) {
            LinearLayout.LayoutParams lpVer = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpVer.setMargins(Ui.dp(this, 6), 0, 0, 0);
            TextView verBadge = Ui.badge(this, "v" + detectedVer, Ui.ACCENT_BG, Ui.ACCENT, Ui.BORDER);
            metaRow.addView(verBadge, lpVer);
        }

        TextView countBadge = Ui.text(this, " · " + game.items.size() + " pacote(s)", 12, Ui.MUTED, false);
        metaRow.addView(countBadge);
        gameCard.addView(metaRow);
        root.addView(gameCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Linha de Ajuda, Detecção por ROM e CheatSlips
        LinearLayout helpRow = new LinearLayout(this);
        helpRow.setOrientation(LinearLayout.HORIZONTAL);
        Button helpBtn = Ui.button(this, "❓ Ajuda Versão", false, v -> showVersionHelpDialog());
        Button pickRomBtn = Ui.button(this, "📂 Detectar ROM", false, v -> pickRomFile());
        Button csBtn = Ui.button(this, "🌐 CheatSlips", false, v -> queryCheatSlips());
        helpRow.addView(helpBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        helpRow.addView(pickRomBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        helpRow.addView(csBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        root.addView(helpRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // Chips de Versões / Build IDs disponíveis com Rótulos Assertivos
        if (!availableBids.isEmpty()) {
            TextView chipLbl = Ui.text(this, "SELECIONE A VERSÃO DO SEU JOGO (TOQUE PARA VER OS CHEATS):", 10, Ui.MUTED, true);
            chipLbl.setTypeface(Typeface.MONOSPACE);
            root.addView(chipLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1));

            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            chipsContainer = new LinearLayout(this);
            chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
            int pChips = Ui.dp(this, 4);
            chipsContainer.setPadding(0, pChips, 0, pChips);
            hsv.addView(chipsContainer);
            root.addView(hsv, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));
        }

        // Filtro / Campo de Build ID
        TextView lbl = Ui.text(this, "BUILD ID ATIVO (16 HEX)", 11, Ui.MUTED, true);
        lbl.setTypeface(Typeface.MONOSPACE);
        root.addView(lbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1));

        bidField = Ui.monoEdit(this, "Ex.: E7E1382D5EB931B0 (16 hex)");

        // Definição inteligente do Build ID inicial:
        // 1. Detectado pelo EdenScanner
        // 2. Se não detectado e houver opções, escolhe a versão MAIS RECENTE da lista!
        // 3. Fallback para termo de busca
        String initialBid = detectedBid;
        if (initialBid == null || initialBid.isEmpty()) {
            if (!availableBids.isEmpty()) {
                // A versão mais recente costuma estar no final ou ter o maior número
                // Para Mario Odyssey: B424BE150A8E7D78 é v1.3.0
                initialBid = findBestDefaultBid(game.tid, availableBids);
            } else {
                String q = Session.query == null ? "" : Session.query.toUpperCase(Locale.ROOT);
                if (q.length() >= 4 && q.matches("[0-9A-F]+")) initialBid = q;
            }
        }
        if (initialBid != null) bidField.setText(initialBid);

        bidField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                renderChips();
                refresh();
                String cur = e.toString().trim().toUpperCase(Locale.ROOT);
                if (Util.isHex16(cur) || cur.length() >= 8) {
                    loadCheatsForBid(cur);
                }
            }
        });
        root.addView(bidField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 3));

        // PAINEL AO VIVO: Lista de Cheats Disponíveis para a Versão Ativa
        cheatPreviewCard = new LinearLayout(this);
        cheatPreviewCard.setOrientation(LinearLayout.VERTICAL);
        cheatPreviewCard.setBackground(Ui.shape(this, Ui.CARD_HOVER, 12, 1, Ui.GOOD));
        int pCheat = Ui.dp(this, 12);
        cheatPreviewCard.setPadding(pCheat, pCheat, pCheat, pCheat);

        cheatSectionTitle = Ui.text(this, "⚡ CHEATS DISPONÍVEIS NESTA VERSÃO:", 13, Ui.GOOD, true);
        cheatPreviewCard.addView(cheatSectionTitle);

        cheatLoadingText = Ui.text(this, "Carregando lista de cheats…", 12, Ui.MUTED, false);
        int plt = Ui.dp(this, 6);
        cheatLoadingText.setPadding(0, plt, 0, plt);
        cheatPreviewCard.addView(cheatLoadingText);

        // Controles de Marcar/Desmarcar
        LinearLayout cheatToggleRow = new LinearLayout(this);
        cheatToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        Button checkAll = Ui.button(this, "✓ Marcar Todos", false, v -> setAllCheatsChecked(true));
        Button uncheckAll = Ui.button(this, "✕ Desmarcar", false, v -> setAllCheatsChecked(false));
        cheatToggleRow.addView(checkAll, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        cheatToggleRow.addView(uncheckAll, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        cheatPreviewCard.addView(cheatToggleRow);

        cheatsContainer = new LinearLayout(this);
        cheatsContainer.setOrientation(LinearLayout.VERTICAL);
        int pt = Ui.dp(this, 4);
        cheatsContainer.setPadding(0, pt, 0, pt);
        cheatPreviewCard.addView(cheatsContainer);

        installNowBtn = Ui.button(this, "🚀 INSTALAR CHEATS SELECIONADOS NO EDEN", true, v -> installSelectedCheats());
        cheatPreviewCard.addView(installNowBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        root.addView(cheatPreviewCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Lista de Pacotes Registrados (para referência e outros mods)
        TextView otherLbl = Ui.text(this, "TODOS OS PACOTES DISPONÍVEIS:", 10, Ui.MUTED, true);
        otherLbl.setTypeface(Typeface.MONOSPACE);
        root.addView(otherLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1));

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((pView, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            Model.Item clicked = shown.get(pos);
            if (clicked.cheat && Util.isHex16(clicked.bid)) {
                bidField.setText(clicked.bid);
                Ui.toast(this, "Versão selecionada: " + clicked.bid);
            } else {
                openItem(clicked);
            }
        });

        // Adiciona altura fixa à listview dentro do scrollview
        int listHeight = Ui.dp(this, 260);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));

        setContentView(sv);

        renderChips();
        refresh();
        if (initialBid != null && !initialBid.isEmpty()) {
            loadCheatsForBid(initialBid);
        }
    }

    private static String findBestDefaultBid(String tid, List<String> bids) {
        if (bids.isEmpty()) return null;
        // Se for Super Mario Odyssey, prioriza v1.3.0 (B424BE150A8E7D78)
        if ("0100000000010000".equalsIgnoreCase(tid) && bids.contains("B424BE150A8E7D78")) {
            return "B424BE150A8E7D78";
        }
        // Se houver algum com rótulo "Mais Recente"
        for (String b : bids) {
            String lbl = VersionMap.getVersionLabel(tid, b, null, bids);
            if (lbl.contains("Mais Recente")) return b;
        }
        // Fallback: o último da lista
        return bids.get(bids.size() - 1);
    }

    private void renderChips() {
        if (chipsContainer == null) return;
        chipsContainer.removeAllViews();
        String current = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);

        for (final String bid : availableBids) {
            boolean isDetected = bid.equalsIgnoreCase(detectedBid);
            boolean isSelected = bid.equalsIgnoreCase(current) || current.contains(bid) || (!current.isEmpty() && bid.contains(current));

            String verLabel = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
            String chipText = (isDetected ? "⚡ " : "") + verLabel + (isDetected ? " (SEU EDEN)" : "");

            TextView chipView;
            if (isDetected || isSelected) {
                chipView = Ui.chipNeon(this, chipText, isSelected, v -> selectBid(bid, verLabel));
            } else {
                chipView = Ui.chip(this, chipText, false, v -> selectBid(bid, verLabel));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, Ui.dp(this, 6), 0);
            chipsContainer.addView(chipView, lp);
        }
    }

    private void selectBid(String bid, String verLabel) {
        bidField.setText(bid);
        bidField.setSelection(bid.length());
        EdenScanner.setUserSelectedBid(this, game.tid, bid, verLabel);
        renderChips();
        loadCheatsForBid(bid);
    }

    private void setAllCheatsChecked(boolean checked) {
        for (CheckBox cb : cheatBoxes) {
            cb.setChecked(checked);
        }
    }

    private void loadCheatsForBid(final String bid) {
        Model.Item targetItem = null;
        for (Model.Item it : game.items) {
            if (it.cheat && it.bid.equalsIgnoreCase(bid)) {
                targetItem = it;
                break;
            }
        }
        if (targetItem == null) {
            for (Model.Item it : game.items) {
                if (it.cheat && (it.bid.contains(bid) || bid.contains(it.bid))) {
                    targetItem = it;
                    break;
                }
            }
        }
        if (targetItem == null && !game.items.isEmpty()) {
            targetItem = game.items.get(0);
        }
        if (targetItem == null || targetItem.files.isEmpty()) {
            cheatLoadingText.setText("Nenhum arquivo de cheats associado a este Build ID.");
            cheatLoadingText.setVisibility(View.VISIBLE);
            installNowBtn.setVisibility(View.GONE);
            return;
        }

        final Model.Item it = targetItem;
        activeCheatItem = it;
        cheatsContainer.removeAllViews();
        cheatBoxes.clear();
        currentBlocks.clear();

        final String friendlyVer = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        cheatSectionTitle.setText("⚡ CHEATS DA VERSÃO " + friendlyVer + ":");
        cheatLoadingText.setText("Lendo cheats disponíveis para " + friendlyVer + "…");
        cheatLoadingText.setVisibility(View.VISIBLE);
        installNowBtn.setVisibility(View.GONE);

        Util.POOL.execute(() -> {
            try {
                byte[] data = Sources.read(it, it.files.get(0));
                final List<CheatParser.Block> parsed = CheatParser.parse(new String(data, StandardCharsets.UTF_8));
                runOnUiThread(() -> {
                    cheatLoadingText.setVisibility(View.GONE);
                    currentBlocks.clear();
                    currentBlocks.addAll(parsed);
                    cheatsContainer.removeAllViews();

                    if (parsed.isEmpty()) {
                        cheatsContainer.addView(Ui.text(DetailActivity.this,
                                "Nenhum código reconhecido no arquivo desta versão.", 12, Ui.MUTED, false));
                        return;
                    }

                    cheatSectionTitle.setText("⚡ " + parsed.size() + " CHEATS DISPONÍVEIS NA VERSÃO " + friendlyVer + ":");

                    for (CheatParser.Block b : parsed) {
                        CheckBox cb = new CheckBox(DetailActivity.this);
                        String lbl = b.name() + (b.master ? "  (CÓDIGO MESTRE)" : "");
                        cb.setText(lbl);
                        cb.setTextColor(Ui.TEXT);
                        cb.setTextSize(13);
                        cb.setChecked(true);
                        cb.setBackground(Ui.shape(DetailActivity.this, Ui.CARD, 8, 1, Ui.BORDER));
                        int pad = Ui.dp(DetailActivity.this, 8);
                        cb.setPadding(pad, pad, pad, pad);
                        cheatBoxes.add(cb);
                        cheatsContainer.addView(cb, Ui.lp(DetailActivity.this, ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
                    }

                    installNowBtn.setVisibility(View.VISIBLE);
                    installNowBtn.setText("🚀 INSTALAR " + parsed.size() + " CHEATS DESTA VERSÃO NO EDEN");
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    cheatLoadingText.setText("Erro ao ler cheats: " + e.getMessage());
                    cheatLoadingText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void installSelectedCheats() {
        if (activeCheatItem == null || currentBlocks.isEmpty()) {
            Ui.toast(this, "Nenhum cheat carregado para instalar.");
            return;
        }
        if (!ensureFolder()) return;

        final String bid = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(bid)) {
            Ui.toast(this, "Informe um Build ID válido com 16 dígitos.");
            return;
        }

        final List<CheatParser.Block> sel = new ArrayList<>();
        final List<CheatParser.Block> masters = new ArrayList<>();
        for (int i = 0; i < currentBlocks.size(); i++) {
            CheatParser.Block b = currentBlocks.get(i);
            if (b.master) masters.add(b);
            if (i < cheatBoxes.size() && cheatBoxes.get(i).isChecked()) sel.add(b);
        }
        if (sel.isEmpty()) {
            Ui.toast(this, "Marque ao menos um cheat para instalar.");
            return;
        }

        // Salvar a escolha assertiva no Scanner
        String verLabel = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        EdenScanner.setUserSelectedBid(this, game.tid, bid, verLabel);

        final Map<String, byte[]> files = new LinkedHashMap<>();
        for (CheatParser.Block b : sel) {
            String folder = Util.sanitize(b.name());
            String dest = folder + "/cheats/" + bid + ".txt";
            String content = CheatParser.buildSingle(b, b.master ? null : masters);
            files.put(dest, content.getBytes(StandardCharsets.UTF_8));
        }

        installNowBtn.setEnabled(false);
        installNowBtn.setText("Gravando no Eden…");
        Util.POOL.execute(() -> {
            String res;
            try {
                res = SafStore.install(this, game.tid, files);
            } catch (Exception e) {
                res = "Erro: " + e.getMessage();
            }
            final String msg = res;
            runOnUiThread(() -> {
                installNowBtn.setEnabled(true);
                installNowBtn.setText("✔ CHEATS INSTALADOS COM SUCESSO!");
                Ui.toast(this, "✔ " + msg + "\nAbra o jogo no Eden e ative em Propriedades > Add-ons!");
                renderChips();
                refresh();
            });
        });
    }

    private void showVersionHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Como Descobrir Sua Versão no Eden")
                .setMessage("No emulador Eden no seu celular:\n\n"
                        + "1. Na lista de jogos, toque e segure o dedo sobre o ícone do jogo.\n"
                        + "2. Selecione a opção 'Info' (ou Propriedades).\n"
                        + "3. Veja o número da versão (ex.: v1.3.0, v1.2.0 ou v1.0.0) ou o Build ID.\n"
                        + "4. Basta tocar no botão da sua versão acima! Os cheats compatíveis aparecem na hora.")
                .setPositiveButton("Entendi", null)
                .show();
    }

    private void pickRomFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_PICK_ROM);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_ROM && res == RESULT_OK && data != null && data.getData() != null) {
            detectVersionFromUri(data.getData());
        }
    }

    private void detectVersionFromUri(Uri uri) {
        String name = "";
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {}
        if (name.isEmpty()) name = uri.getLastPathSegment();
        if (name == null) name = "";

        String upper = name.toUpperCase(Locale.US);
        String matchedBid = null;

        // Se for Super Mario Odyssey
        if ("0100000000010000".equals(game.tid) || upper.contains("ODYSSEY") || upper.contains("0100000000010000") || upper.contains("0100000000010800")) {
            if (upper.contains("1.3") || upper.contains("196608") || upper.contains("B424BE")) {
                matchedBid = "B424BE150A8E7D78";
            } else if (upper.contains("1.2") || upper.contains("131072") || upper.contains("F5DCCD")) {
                matchedBid = "F5DCCDDB37E97724";
            } else if (upper.contains("1.0") || upper.contains("V0") || upper.contains("3CA12D")) {
                matchedBid = "3CA12DFAAF9C82DA";
            }
        }

        // Verificação genérica para outros jogos
        if (matchedBid == null) {
            for (String b : availableBids) {
                if (upper.contains(b)) {
                    matchedBid = b;
                    break;
                }
            }
        }

        if (matchedBid != null) {
            String ver = VersionMap.getVersionLabel(game.tid, matchedBid, null, availableBids);
            selectBid(matchedBid, ver);
            Ui.toast(this, "🎯 Versão detectada com sucesso da ROM: " + ver + "!");
        } else {
            Ui.toast(this, "Arquivo: " + name + "\nToque em uma das versões acima para escolher.");
        }
    }

    private void openItem(Model.Item item) {
        Session.item = item;
        String bidVal = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        Session.targetBuildId = bidVal;
        startActivity(new Intent(this, ItemActivity.class));
    }

    private void refresh() {
        String q = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        List<Model.Item> match = new ArrayList<>();
        List<Model.Item> other = new ArrayList<>();
        for (Model.Item it : game.items) {
            if (!q.isEmpty() && it.cheat && (it.bid.contains(q) || q.contains(it.bid))) match.add(it);
            else other.add(it);
        }
        shown.clear();
        shown.addAll(match);
        shown.addAll(other);

        adapter.rows.clear();
        for (Model.Item it : shown) {
            boolean m = match.contains(it);
            String verLabel = VersionMap.getVersionLabel(game.tid, it.bid, null, availableBids);
            String sub = (it.cheat ? "Cheats" : "Mod") + " · " + verLabel + " · " + it.files.size() + " arquivo(s)";
            if (m) sub += " · ⚡ 100% Compatível";
            adapter.rows.add(new String[]{(m ? "⚡ " : "") + it.title, sub, m ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
    }

    private void queryCheatSlips() {
        String bid = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(bid)) {
            Ui.toast(this, "Selecione ou insira uma Build ID válida (16 caracteres) primeiro.");
            return;
        }

        Ui.toast(this, "Consultando cheats no CheatSlips.com…");
        Util.POOL.execute(() -> {
            CheatSlips.CheatResult res = CheatSlips.fetch(this, game.tid, bid);
            runOnUiThread(() -> {
                if (res.fullContent != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("✨ Cheats do CheatSlips")
                            .setMessage("Encontrados cheats no CheatSlips para " + game.display() + " (" + bid + "):\n\n• "
                                    + String.join("\n• ", res.cheatTitles)
                                    + "\n\nDeseja instalar estes cheats diretamente na sua pasta do Eden?")
                            .setPositiveButton("📥 Instalar no Eden", (d, w) -> {
                                installCheatContentDirectly("CheatSlips Cheats", bid, res.fullContent);
                            })
                            .setNegativeButton("Fechar", null)
                            .show();
                } else if (res.requiresToken) {
                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setTitle("🔑 CheatSlips requer API Token");
                    String titles = res.cheatTitles.isEmpty() ? "Cheatsheets disponíveis na comunidade" : String.join("\n• ", res.cheatTitles);
                    b.setMessage("O CheatSlips identificou os seguintes cheats para esta versão:\n\n• "
                            + titles
                            + "\n\nO site cheatslips.com exige que você tenha uma conta (gratuita ou VIP) e forneça seu API Token pessoal para baixar o código.\n\n"
                            + "Deseja cadastrar sua conta gratuita no CheatSlips ou já possui um token?");
                    b.setPositiveButton("🔑 Colar API Token", (d, w) -> showEnterTokenDialog(bid));
                    b.setNeutralButton("🌐 Abrir CheatSlips", (d, w) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(CheatSlips.SITE_URL)));
                    });
                    b.setNegativeButton("Voltar", null);
                    b.show();
                } else if (res.quotaExceeded) {
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Limite CheatSlips Atingido")
                            .setMessage("O limite diário de downloads da sua conta gratuita no CheatSlips foi atingido. Tente novamente mais tarde ou atualize para VIP no site.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("CheatSlips.com")
                            .setMessage(res.errorMessage != null ? res.errorMessage : "Nenhum cheat encontrado no CheatSlips para esta versão.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });
    }

    private void showEnterTokenDialog(String bid) {
        EditText input = new EditText(this);
        input.setTextColor(Ui.TEXT);
        input.setHint("Cole seu API Token do CheatSlips aqui...");
        input.setHintTextColor(Ui.MUTED);
        input.setText(CheatSlips.getToken(this));

        new AlertDialog.Builder(this)
                .setTitle("Salvar API Token")
                .setView(input)
                .setPositiveButton("Salvar e Buscar", (d, w) -> {
                    CheatSlips.setToken(this, input.getText().toString());
                    Ui.toast(this, "Token salvo! Buscando novamente…");
                    queryCheatSlips();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void installCheatContentDirectly(String modName, String bid, String content) {
        if (!SafStore.hasTree(this)) {
            folderDialog();
            return;
        }
        Util.POOL.execute(() -> {
            String msg;
            try {
                Map<String, byte[]> files = new LinkedHashMap<>();
                files.put("cheats/" + bid + ".txt", content.getBytes(StandardCharsets.UTF_8));
                msg = SafStore.install(this, game.tid, modName, files);
            } catch (Exception e) {
                msg = "Falha ao instalar: " + e.getMessage();
            }
            final String finalMsg = msg;
            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("Resultado da Instalação")
                        .setMessage(finalMsg)
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }
}
