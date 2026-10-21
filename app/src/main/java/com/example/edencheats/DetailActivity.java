package com.example.edencheats;

import android.app.AlertDialog;
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
 * Detalhes do jogo selecionado - Aba de Modificações (A Forja Cibernética):
 * - Cabeçalho minimalista sem redundâncias.
 * - Caixa de comando terminal com a Build ID detectada em Verde Menta.
 * - Card informativo de topo para metadados globais do patch (sem checkbox).
 * - Checkboxes customizados estilo cyber neon.
 * - Nomes limpos dos cheats sem numeração redundante (#1., #2.).
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

    private final List<CheatParser.Block> functionalCheats = new ArrayList<>();
    private final List<CheatParser.Block> masterBlocks = new ArrayList<>();
    private CheatParser.Block infoBlock = null;
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

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);

        LinearLayout root = Ui.page(this);
        sv.addView(root);

        // 1. Cabeçalho Minimalista e Limpo (Área de Identidade do Jogo)
        LinearLayout gameCard = new LinearLayout(this);
        gameCard.setOrientation(LinearLayout.VERTICAL);
        gameCard.setBackground(Ui.shape(this, Ui.CARD, 10, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        gameCard.setPadding(p, p, p, p);

        // Nome do jogo proeminente em branco puro
        gameCard.addView(Ui.text(this, game.display(), 19, 0xFFFFFFFF, true));

        // Subtítulo técnico único sem repetições
        TextView subMeta = new TextView(this);
        int nItems = game.items.size();
        String tidText = game.tid.isEmpty() ? "TID DESCONHECIDO" : game.tid;
        subMeta.setText("TITLE ID: " + tidText + "  //  " + nItems + (nItems == 1 ? " PACOTE" : " PACOTES"));
        subMeta.setTextSize(11);
        subMeta.setTextColor(Ui.ID_COLOR); // #6272A4
        subMeta.setTypeface(Typeface.MONOSPACE);
        int mtSub = Ui.dp(this, 4);
        subMeta.setPadding(0, mtSub, 0, 0);
        gameCard.addView(subMeta);

        root.addView(gameCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // Seção Unificada de Ajuda e Detecção por ROM (sem emojis)
        LinearLayout helpRow = new LinearLayout(this);
        helpRow.setOrientation(LinearLayout.HORIZONTAL);
        View helpBtn = Ui.techCardButton(this, "Guia de Versão", "[INFO]", false, v -> showVersionHelpDialog());
        View pickRomBtn = Ui.techCardButton(this, "Detectar via ROM", "[ROM]", false, v -> pickRomFile());
        helpRow.addView(helpBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        helpRow.addView(pickRomBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        root.addView(helpRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        // 2. Painel de Status do Build ID (Seleção de Versões)
        if (!availableBids.isEmpty()) {
            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            chipsContainer = new LinearLayout(this);
            chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
            int pChips = Ui.dp(this, 2);
            chipsContainer.setPadding(0, pChips, 0, pChips);
            hsv.addView(chipsContainer);
            root.addView(hsv, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        }

        // Caixa de Terminal para o Build ID (sem títulos secundários repetidos)
        LinearLayout termBox = new LinearLayout(this);
        termBox.setOrientation(LinearLayout.HORIZONTAL);
        termBox.setGravity(Gravity.CENTER_VERTICAL);
        termBox.setBackground(Ui.shape(this, 0xFF0E121A, 8, 1, Ui.BORDER));
        int pTerm = Ui.dp(this, 10);
        termBox.setPadding(pTerm, pTerm, pTerm, pTerm);

        TextView prompt = new TextView(this);
        prompt.setText("> ");
        prompt.setTextSize(12);
        prompt.setTextColor(Ui.ACCENT);
        prompt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        termBox.addView(prompt);

        bidField = new EditText(this);
        bidField.setTextSize(12);
        bidField.setTextColor(Ui.LED_GREEN); // Verde Menta brilhante (#00FFA3)
        bidField.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bidField.setSingleLine(true);
        bidField.setBackground(null);
        bidField.setHint("[ BUILD DETECTADA: 16 CARACTERES HEX ]");
        bidField.setHintTextColor(Ui.ICON_MUTED);
        LinearLayout.LayoutParams lpBid = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        termBox.addView(bidField, lpBid);

        root.addView(termBox, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // Definição inteligente do Build ID inicial
        String initialBid = detectedBid;
        if (initialBid == null || initialBid.isEmpty()) {
            if (!availableBids.isEmpty()) {
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

        // 3. Container de Cheats e Ações Globais (Aba de Modificações)
        cheatPreviewCard = new LinearLayout(this);
        cheatPreviewCard.setOrientation(LinearLayout.VERTICAL);
        cheatPreviewCard.setBackground(Ui.shape(this, Ui.CARD, 10, 1, Ui.BORDER));
        int pCheat = Ui.dp(this, 12);
        cheatPreviewCard.setPadding(pCheat, pCheat, pCheat, pCheat);

        // Cabeçalho de Seção Fixo com Ações Globais Minimalistas
        LinearLayout sectionHead = new LinearLayout(this);
        sectionHead.setOrientation(LinearLayout.HORIZONTAL);
        sectionHead.setGravity(Gravity.CENTER_VERTICAL);

        cheatSectionTitle = new TextView(this);
        cheatSectionTitle.setText("// MODS DISPONÍVEIS NA VERSÃO ATIVA");
        cheatSectionTitle.setTextSize(11);
        cheatSectionTitle.setTextColor(Ui.ACCENT);
        cheatSectionTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        sectionHead.addView(cheatSectionTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Ações Globais Minimalistas: Linha divisória fina, sem caixas pretas pesadas
        LinearLayout actionLinks = new LinearLayout(this);
        actionLinks.setOrientation(LinearLayout.HORIZONTAL);
        actionLinks.setGravity(Gravity.CENTER_VERTICAL);

        TextView markAll = new TextView(this);
        markAll.setText("MARCAR TODOS");
        markAll.setTextSize(10);
        markAll.setTextColor(Ui.ACCENT);
        markAll.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        markAll.setClickable(true);
        markAll.setFocusable(true);
        markAll.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        markAll.setOnClickListener(v -> setAllCheatsChecked(true));
        actionLinks.addView(markAll);

        TextView div = new TextView(this);
        div.setText(" | ");
        div.setTextSize(10);
        div.setTextColor(Ui.ICON_MUTED);
        div.setTypeface(Typeface.MONOSPACE);
        actionLinks.addView(div);

        TextView unmarkAll = new TextView(this);
        unmarkAll.setText("DESMARCAR");
        unmarkAll.setTextSize(10);
        unmarkAll.setTextColor(Ui.MUTED);
        unmarkAll.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        unmarkAll.setClickable(true);
        unmarkAll.setFocusable(true);
        unmarkAll.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        unmarkAll.setOnClickListener(v -> setAllCheatsChecked(false));
        actionLinks.addView(unmarkAll);

        sectionHead.addView(actionLinks);
        cheatPreviewCard.addView(sectionHead);

        cheatLoadingText = Ui.text(this, "Carregando lista de mods…", 12, Ui.MUTED, false);
        int plt = Ui.dp(this, 8);
        cheatLoadingText.setPadding(0, plt, 0, plt);
        cheatPreviewCard.addView(cheatLoadingText);

        cheatsContainer = new LinearLayout(this);
        cheatsContainer.setOrientation(LinearLayout.VERTICAL);
        int pt = Ui.dp(this, 4);
        cheatsContainer.setPadding(0, pt, 0, pt);
        cheatPreviewCard.addView(cheatsContainer);

        installNowBtn = Ui.button(this, "INSTALAR MODS SELECIONADOS NO EDEN", true, v -> installSelectedCheats());
        cheatPreviewCard.addView(installNowBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        root.addView(cheatPreviewCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // 4. Lista de Pacotes Registrados no Catálogo (para referência)
        TextView otherLbl = Ui.text(this, "// TODOS OS PACOTES DISPONÍVEIS:", 10, Ui.MUTED, true);
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

        int listHeight = Ui.dp(this, 240);
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
        if ("0100000000010000".equalsIgnoreCase(tid) && bids.contains("B424BE150A8E7D78")) {
            return "B424BE150A8E7D78";
        }
        for (String b : bids) {
            String lbl = VersionMap.getVersionLabel(tid, b, null, bids);
            if (lbl.contains("Mais Recente")) return b;
        }
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
            String chipText = verLabel + (isDetected ? " [SEU EDEN]" : "");

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
        functionalCheats.clear();
        masterBlocks.clear();
        infoBlock = null;

        final String friendlyVer = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        cheatSectionTitle.setText("// " + friendlyVer.toUpperCase(Locale.ROOT));
        cheatLoadingText.setText("Lendo cheats disponíveis para " + friendlyVer + "…");
        cheatLoadingText.setVisibility(View.VISIBLE);
        installNowBtn.setVisibility(View.GONE);

        Util.POOL.execute(() -> {
            try {
                byte[] data = Sources.read(it, it.files.get(0));
                final List<CheatParser.Block> parsed = CheatParser.parse(new String(data, StandardCharsets.UTF_8));
                runOnUiThread(() -> {
                    cheatLoadingText.setVisibility(View.GONE);
                    cheatsContainer.removeAllViews();

                    if (parsed.isEmpty()) {
                        cheatsContainer.addView(Ui.text(DetailActivity.this,
                                "Nenhum código reconhecido no arquivo desta versão.", 12, Ui.MUTED, false));
                        return;
                    }

                    // Separação inteligente: Card Informativo de Topo vs Cheats Selecionáveis
                    for (int i = 0; i < parsed.size(); i++) {
                        CheatParser.Block b = parsed.get(i);
                        if (b.master) {
                            masterBlocks.add(b);
                            continue;
                        }
                        String upperName = b.name().toUpperCase(Locale.US);
                        boolean isGlobalInfo = (i == 0 && (upperName.contains("GLOBAL")
                                || upperName.contains("VERSION")
                                || upperName.contains("CREDIT")
                                || upperName.contains("PATCH")
                                || upperName.contains("V65536")
                                || b.lines.isEmpty()));

                        if (isGlobalInfo && infoBlock == null) {
                            infoBlock = b;
                        } else {
                            functionalCheats.add(b);
                        }
                    }

                    // Se houver dados globais do patch, renderiza como Card Informativo de Topo (SEM CHECKBOX)
                    if (infoBlock != null) {
                        LinearLayout infoCard = new LinearLayout(DetailActivity.this);
                        infoCard.setOrientation(LinearLayout.VERTICAL);
                        infoCard.setBackground(Ui.shape(DetailActivity.this, Ui.CARD, 8, 1, Ui.BORDER));
                        int pInfo = Ui.dp(DetailActivity.this, 10);
                        infoCard.setPadding(pInfo, pInfo, pInfo, pInfo);

                        TextView infoTag = new TextView(DetailActivity.this);
                        infoTag.setText("// DADOS GLOBAIS DO PATCH");
                        infoTag.setTextSize(10);
                        infoTag.setTextColor(Ui.ACCENT);
                        infoTag.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                        infoCard.addView(infoTag);

                        TextView infoName = new TextView(DetailActivity.this);
                        infoName.setText(infoBlock.name());
                        infoName.setTextSize(12);
                        infoName.setTextColor(0xFFFFFFFF);
                        infoName.setTypeface(Typeface.DEFAULT_BOLD);
                        int mTop = Ui.dp(DetailActivity.this, 4);
                        infoName.setPadding(0, mTop, 0, 0);
                        infoCard.addView(infoName);

                        cheatsContainer.addView(infoCard, Ui.lp(DetailActivity.this, ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));
                    }

                    cheatSectionTitle.setText("// " + functionalCheats.size() + " MODS DISPONÍVEIS NA VERSÃO " + friendlyVer.toUpperCase(Locale.ROOT));

                    // Renderiza os cheats funcionais com checkboxes customizados e sem números #1., #2.
                    for (CheatParser.Block b : functionalCheats) {
                        final LinearLayout cheatRow = new LinearLayout(DetailActivity.this);
                        cheatRow.setOrientation(LinearLayout.HORIZONTAL);
                        cheatRow.setGravity(Gravity.CENTER_VERTICAL);
                        cheatRow.setClickable(true);
                        cheatRow.setFocusable(true);
                        int padH = Ui.dp(DetailActivity.this, 10);
                        int padV = Ui.dp(DetailActivity.this, 9);
                        cheatRow.setPadding(padH, padV, padH, padV);

                        final CheckBox cb = new CheckBox(DetailActivity.this);
                        cb.setButtonDrawable(R.drawable.cyber_checkbox);
                        cb.setChecked(true);
                        cheatRow.addView(cb);

                        // Remove #1., #2., 01., etc. do texto
                        String rawName = b.name();
                        String cleanName = rawName.replaceAll("^(?:#?\\d+|\\d+)\\s*[\\.:\\-\\)]+\\s*", "").trim();
                        if (cleanName.isEmpty()) cleanName = rawName;

                        TextView nameView = new TextView(DetailActivity.this);
                        nameView.setText(cleanName);
                        nameView.setTextColor(0xFFFFFFFF);
                        nameView.setTextSize(13);
                        nameView.setTypeface(Typeface.DEFAULT_BOLD);
                        LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        lpName.setMargins(Ui.dp(DetailActivity.this, 10), 0, 0, 0);
                        cheatRow.addView(nameView, lpName);

                        final Runnable updateStyle = () -> {
                            if (cb.isChecked()) {
                                cheatRow.setBackground(Ui.shape(DetailActivity.this, Ui.CARD, 8, 1, Ui.ACCENT));
                            } else {
                                cheatRow.setBackground(Ui.shape(DetailActivity.this, Ui.CARD, 8, 1, Ui.BORDER));
                            }
                        };
                        updateStyle.run();

                        cb.setOnCheckedChangeListener((btn, isChecked) -> updateStyle.run());
                        cheatRow.setOnClickListener(v -> cb.toggle());

                        cheatBoxes.add(cb);
                        cheatsContainer.addView(cheatRow, Ui.lp(DetailActivity.this, ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
                    }

                    installNowBtn.setVisibility(View.VISIBLE);
                    installNowBtn.setText("INSTALAR " + functionalCheats.size() + " MODS DESTA VERSÃO NO EDEN");
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
        if (activeCheatItem == null || functionalCheats.isEmpty()) {
            Ui.toast(this, "Nenhum mod carregado para instalar.");
            return;
        }
        if (!ensureFolder()) return;

        final String bid = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!Util.isHex16(bid)) {
            Ui.toast(this, "Informe um Build ID válido com 16 dígitos.");
            return;
        }

        final List<CheatParser.Block> sel = new ArrayList<>();
        for (int i = 0; i < functionalCheats.size(); i++) {
            if (i < cheatBoxes.size() && cheatBoxes.get(i).isChecked()) {
                sel.add(functionalCheats.get(i));
            }
        }
        if (sel.isEmpty()) {
            Ui.toast(this, "Marque ao menos um mod para instalar.");
            return;
        }

        // Salvar a escolha assertiva no Scanner
        String verLabel = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        EdenScanner.setUserSelectedBid(this, game.tid, bid, verLabel);

        final Map<String, byte[]> files = new LinkedHashMap<>();
        for (CheatParser.Block b : sel) {
            String folder = Util.sanitize(b.name());
            String dest = folder + "/cheats/" + bid + ".txt";
            List<CheatParser.Block> allMasters = new ArrayList<>(masterBlocks);
            if (infoBlock != null && !allMasters.contains(infoBlock)) {
                allMasters.add(0, infoBlock);
            }
            String content = CheatParser.buildSingle(b, b.master ? null : allMasters);
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
                installNowBtn.setText("✔ MODS INSTALADOS COM SUCESSO!");
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
            Ui.toast(this, "Versão detectada com sucesso da ROM: " + ver + "!");
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
            if (m) sub += " · 100% Compatível";
            adapter.rows.add(new String[]{it.title, sub, m ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
    }
}
