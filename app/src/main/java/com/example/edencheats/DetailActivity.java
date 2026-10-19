package com.example.edencheats;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detalhes do jogo selecionado: exibe cheats/mods disponíveis,
 * identifica automaticamente a compatibilidade com o Build ID do Eden,
 * destaca o cheat compatível em verde neon no topo e oferece instalação com 1 toque.
 */
public class DetailActivity extends BaseActivity {
    private Model.Game game;
    private EditText bidField;
    private Ui.RowAdapter adapter;
    private final List<Model.Item> shown = new ArrayList<>();
    private final List<String> availableBids = new ArrayList<>();
    private String detectedBid = null;
    private LinearLayout chipsContainer;
    private LinearLayout matchCard;
    private TextView matchTitle;
    private TextView matchSub;
    private Button matchBtn;

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

        // Consultar o motor de varredura do Eden para saber se o jogo foi detectado
        detectedBid = EdenScanner.getBuildId(this, game.tid);
        String detectedVer = EdenScanner.getVersion(this, game.tid);

        LinearLayout root = Ui.page(this);

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
            TextView edenBadge = Ui.badge(this, "⚡ EDEN DETECTADO", Ui.GOOD_BG, Ui.GOOD, Ui.GOOD);
            metaRow.addView(edenBadge, lpEden);
        }

        if (detectedVer != null && !detectedVer.isEmpty()) {
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

        // Card de Destaque Neon do Cheat Compatível (Instalação Rápida em 1 toque)
        matchCard = new LinearLayout(this);
        matchCard.setOrientation(LinearLayout.VERTICAL);
        matchCard.setBackground(Ui.shape(this, Ui.CARD_HOVER, 12, 1, Ui.GOOD));
        int pMatch = Ui.dp(this, 12);
        matchCard.setPadding(pMatch, pMatch, pMatch, pMatch);

        LinearLayout matchHeader = new LinearLayout(this);
        matchHeader.setOrientation(LinearLayout.HORIZONTAL);
        matchHeader.setGravity(Gravity.CENTER_VERTICAL);

        matchTitle = Ui.text(this, "⚡ CHEAT 100% COMPATÍVEL COM O SEU EDEN", 14, Ui.GOOD, true);
        matchHeader.addView(matchTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView matchTag = Ui.badge(this, "MATCH", Ui.GOOD_BG, Ui.GOOD, Ui.GOOD);
        matchHeader.addView(matchTag);
        matchCard.addView(matchHeader);

        matchSub = Ui.text(this, "", 12, Ui.TEXT, false);
        int pSub = Ui.dp(this, 4);
        matchSub.setPadding(0, pSub, 0, pSub);
        matchCard.addView(matchSub);

        matchBtn = Ui.button(this, "🚀 INSTALAR CHEAT COMPATÍVEL AGORA", true, null);
        matchCard.addView(matchBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        matchCard.setVisibility(View.GONE);
        root.addView(matchCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Chips de Versões / Build IDs disponíveis
        if (!availableBids.isEmpty()) {
            TextView chipLbl = Ui.text(this, "VERSÕES DISPONÍVEIS NO BANCO (TOQUE PARA SELECIONAR):", 10, Ui.MUTED, true);
            chipLbl.setTypeface(Typeface.MONOSPACE);
            root.addView(chipLbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1));

            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            chipsContainer = new LinearLayout(this);
            chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
            int pChips = Ui.dp(this, 2);
            chipsContainer.setPadding(0, pChips, 0, pChips);
            hsv.addView(chipsContainer);
            root.addView(hsv, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));
        }

        // Filtro / Campo de Build ID
        TextView lbl = Ui.text(this, "FILTRAR PELO SEU BUILD ID", 11, Ui.MUTED, true);
        lbl.setTypeface(Typeface.MONOSPACE);
        root.addView(lbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        bidField = Ui.monoEdit(this, "Ex.: E7E1382D5EB931B0 (16 hex)");

        // 1. Prioridade: Build ID detectado pela varredura do Eden
        // 2. Se o jogo tiver apenas 1 Build ID disponível no banco, pré-seleciona ele!
        // 3. Senão, se a busca do usuário continha hex, aproveita
        if (detectedBid != null && !detectedBid.isEmpty()) {
            bidField.setText(detectedBid);
        } else if (availableBids.size() == 1) {
            bidField.setText(availableBids.get(0));
        } else {
            String q = Session.query == null ? "" : Session.query.toUpperCase(Locale.ROOT);
            if (q.length() >= 4 && q.matches("[0-9A-F]+")) bidField.setText(q);
        }

        bidField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                renderChips();
                refresh();
            }
        });
        root.addView(bidField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 3));

        TextView tip = Ui.text(this, "💡 Itens compatíveis com o seu Build ID ficam destacados em verde neon no topo.",
                11, Ui.GOOD, false);
        root.addView(tip, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((pView, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            openItem(shown.get(pos));
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));

        setContentView(root);
        renderChips();
        refresh();
    }

    private void renderChips() {
        if (chipsContainer == null) return;
        chipsContainer.removeAllViews();
        String current = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);

        for (final String bid : availableBids) {
            boolean isDetected = bid.equalsIgnoreCase(detectedBid);
            boolean isSelected = bid.equalsIgnoreCase(current) || current.contains(bid) || (!current.isEmpty() && bid.contains(current));

            String label = (isDetected ? "⚡ " : "") + bid + (isDetected ? " (EDEN)" : "");
            TextView chipView;
            if (isDetected) {
                chipView = Ui.chipNeon(this, label, isSelected, v -> {
                    bidField.setText(bid);
                    bidField.setSelection(bid.length());
                });
            } else {
                chipView = Ui.chip(this, label, isSelected, v -> {
                    bidField.setText(bid);
                    bidField.setSelection(bid.length());
                });
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, Ui.dp(this, 6), 0);
            chipsContainer.addView(chipView, lp);
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
            if (!q.isEmpty() && it.cheat && it.bid.contains(q)) match.add(it);
            else other.add(it);
        }
        shown.clear();
        shown.addAll(match);
        shown.addAll(other);

        // Atualizar Card de Ação Rápida
        if (!match.isEmpty()) {
            final Model.Item bestMatch = match.get(0);
            matchCard.setVisibility(View.VISIBLE);
            matchSub.setText("Build ID: " + bestMatch.bid + " · " + bestMatch.files.size() + " arquivo(s) prontos para instalar.");
            matchBtn.setOnClickListener(v -> openItem(bestMatch));
        } else {
            matchCard.setVisibility(View.GONE);
        }

        adapter.rows.clear();
        for (Model.Item it : shown) {
            boolean m = match.contains(it);
            String sub = (it.cheat ? "Cheats" : "Mod") + " · " + it.files.size() + " arquivo(s)";
            long size = it.totalSize();
            if (size > 0) sub += " · " + Util.mb(size);
            if (m) sub += " · ⚡ 100% Compatível com o seu Build ID";
            adapter.rows.add(new String[]{(m ? "⚡ " : "") + it.title, sub, m ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
    }
}
