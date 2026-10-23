package com.example.edencheats;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detalhes do jogo selecionado - Aba de Modificações (A Forja Cibernética):
 * - Layout declarativo via activity_cheat_manager.xml
 * - Cabeçalho minimalista sem redundâncias
 * - Console de status da Build ID detectada em Verde Menta (#00FFA3)
 * - Card informativo de topo para metadados globais do patch (sem checkbox)
 * - RecyclerView com item_cheat_layout.xml e cyber_checkbox_tint.xml
 * - Nomes limpos dos cheats sem numeração redundante (#1., #2.)
 */
public class DetailActivity extends BaseActivity {
    private static final int REQ_PICK_ROM = 31;
    private static final int TYPE_INFO = 0;
    private static final int TYPE_CHEAT = 1;

    private Model.Game game;
    private final List<String> availableBids = new ArrayList<>();
    private String detectedBid = null;
    private String currentBid = null;

    private TextView tvGameTitle;
    private TextView tvGameMeta;
    private MaterialButton btnHowToVersion;
    private MaterialButton btnDetectRom;
    private MaterialCardView cvBuildStatus;
    private TextView tvBuildStatusHeader;
    private TextView tvBuildHex;
    private TextView tvSectionTitle;
    private TextView tvSelectAll;
    private TextView tvDeselectAll;
    private RecyclerView rvCheatsList;
    private MaterialButton btnInstallCheats;

    private final List<CheatItem> cheatItems = new ArrayList<>();
    private CheatsAdapter adapter;

    private final List<CheatParser.Block> functionalCheats = new ArrayList<>();
    private final List<CheatParser.Block> masterBlocks = new ArrayList<>();
    private CheatParser.Block infoBlock = null;
    private Model.Item activeCheatItem = null;

    static class CheatItem {
        final int type;
        final CheatParser.Block block;
        final String cleanName;
        boolean checked;

        CheatItem(int type, CheatParser.Block block, String cleanName, boolean checked) {
            this.type = type;
            this.block = block;
            this.cleanName = cleanName;
            this.checked = checked;
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        game = Session.game;
        if (game == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_cheat_manager);

        // Extrair todos os Build IDs distintos disponíveis neste jogo
        for (Model.Item it : game.items) {
            if (it.cheat && !it.bid.isEmpty() && !availableBids.contains(it.bid)) {
                availableBids.add(it.bid);
            }
        }

        // 1. Consultar se já existe detecção pelo EdenScanner
        detectedBid = EdenScanner.getBuildId(this, game.tid);

        // Bind das views
        tvGameTitle = findViewById(R.id.tvGameTitle);
        tvGameMeta = findViewById(R.id.tvGameMeta);
        btnHowToVersion = findViewById(R.id.btnHowToVersion);
        btnDetectRom = findViewById(R.id.btnDetectRom);
        cvBuildStatus = findViewById(R.id.cvBuildStatus);
        tvBuildStatusHeader = findViewById(R.id.tvBuildStatusHeader);
        tvBuildHex = findViewById(R.id.tvBuildHex);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvSelectAll = findViewById(R.id.tvSelectAll);
        tvDeselectAll = findViewById(R.id.tvDeselectAll);
        rvCheatsList = findViewById(R.id.rvCheatsList);
        btnInstallCheats = findViewById(R.id.btnInstallCheats);

        // 1. Cabeçalho do Jogo
        tvGameTitle.setText(game.display());
        int nItems = game.items.size();
        String tidText = (game.tid == null || game.tid.isEmpty()) ? "TID DESCONHECIDO" : game.tid;
        tvGameMeta.setText(tidText + " • " + nItems + (nItems == 1 ? " pacote" : " pacotes"));

        // 2. Botões de Suporte
        btnHowToVersion.setOnClickListener(v -> showVersionHelpDialog());
        btnDetectRom.setOnClickListener(v -> pickRomFile());

        // 3. Card de Build ID (estilo Terminal)
        cvBuildStatus.setOnClickListener(v -> showBuildPickerDialog());

        // 4. Seleção Global
        tvSelectAll.setOnClickListener(v -> selectAll(true));
        tvDeselectAll.setOnClickListener(v -> selectAll(false));

        // 5. Configuração da RecyclerView
        rvCheatsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheatsAdapter();
        rvCheatsList.setAdapter(adapter);

        // 6. Botão de Instalação
        btnInstallCheats.setOnClickListener(v -> installSelectedCheats());

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

        if (initialBid != null && !initialBid.isEmpty()) {
            selectBid(initialBid);
        } else {
            updateBuildStatusCard();
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

    private void selectBid(String bid) {
        currentBid = bid;
        String verLabel = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        EdenScanner.setUserSelectedBid(this, game.tid, bid, verLabel);
        updateBuildStatusCard();
        loadCheatsForBid(bid);
    }

    private void updateBuildStatusCard() {
        if (currentBid == null || currentBid.isEmpty()) {
            tvBuildHex.setText("NENHUMA VERSÃO SELECIONADA");
            tvBuildStatusHeader.setText("// BUILD ID");
            tvBuildStatusHeader.setTextColor(Color.parseColor("#6272A4"));
            cvBuildStatus.setStrokeColor(Color.parseColor("#1F2637"));
            return;
        }

        boolean isDetected = currentBid.equalsIgnoreCase(detectedBid);
        String verLabel = VersionMap.getVersionLabel(game.tid, currentBid, null, availableBids);
        if (isDetected) {
            tvBuildStatusHeader.setText("// BUILD ID ATIVO MATCH [SEU EDEN]");
            tvBuildStatusHeader.setTextColor(Color.parseColor("#00FFA3")); // Verde Menta
            cvBuildStatus.setStrokeColor(Color.parseColor("#00FFA3"));
        } else {
            String suffix = availableBids.size() > 1 ? " (TOQUE PARA TROCAR)" : "";
            tvBuildStatusHeader.setText("// BUILD ID ATIVO" + suffix);
            tvBuildStatusHeader.setTextColor(Color.parseColor("#00D4FF")); // Ciano Neon
            cvBuildStatus.setStrokeColor(Color.parseColor("#00D4FF"));
        }
        tvBuildHex.setText(currentBid + " • " + verLabel);
    }

    private void showBuildPickerDialog() {
        List<String> options = new ArrayList<>();
        int checkedItem = -1;
        for (int i = 0; i < availableBids.size(); i++) {
            String b = availableBids.get(i);
            String ver = VersionMap.getVersionLabel(game.tid, b, null, availableBids);
            boolean isDetected = b.equalsIgnoreCase(detectedBid);
            options.add(ver + "  (" + b + ")" + (isDetected ? " ★ [SEU EDEN]" : ""));
            if (b.equalsIgnoreCase(currentBid)) {
                checkedItem = i;
            }
        }
        options.add("Digitar outro Build ID...");

        new AlertDialog.Builder(this)
                .setTitle("Selecionar Versão do Jogo")
                .setSingleChoiceItems(options.toArray(new String[0]), checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    if (which < availableBids.size()) {
                        selectBid(availableBids.get(which));
                    } else {
                        promptManualBuildId();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void promptManualBuildId() {
        final EditText input = new EditText(this);
        input.setHint("Ex: 0F1169AA7D0FA452");
        input.setTypeface(Typeface.MONOSPACE);
        input.setText(currentBid != null ? currentBid : "");
        new AlertDialog.Builder(this)
                .setTitle("Digitar Build ID (16 hex)")
                .setView(input)
                .setPositiveButton("Confirmar", (d, w) -> {
                    String val = input.getText().toString().trim().toUpperCase(Locale.ROOT);
                    if (Util.isHex16(val) || val.length() >= 8) {
                        selectBid(val);
                    } else {
                        Ui.toast(this, "Build ID inválido. Deve ter 16 caracteres hexadecimais.");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void selectAll(boolean checked) {
        for (CheatItem item : cheatItems) {
            if (item.type == TYPE_CHEAT) {
                item.checked = checked;
            }
        }
        adapter.notifyDataSetChanged();
        updateInstallButton();
    }

    private void updateInstallButton() {
        int count = 0;
        for (CheatItem item : cheatItems) {
            if (item.type == TYPE_CHEAT && item.checked) {
                count++;
            }
        }
        if (count == 0) {
            btnInstallCheats.setEnabled(false);
            btnInstallCheats.setText("NENHUM MOD SELECIONADO");
            btnInstallCheats.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#121620")));
            btnInstallCheats.setTextColor(Color.parseColor("#6272A4"));
        } else {
            btnInstallCheats.setEnabled(true);
            btnInstallCheats.setText("INSTALAR " + count + " MODS NO EDEN");
            btnInstallCheats.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00D4FF")));
            btnInstallCheats.setTextColor(Color.parseColor("#08090C"));
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
            tvSectionTitle.setText("// NENHUM ARQUIVO PARA ESTA VERSÃO");
            cheatItems.clear();
            adapter.notifyDataSetChanged();
            updateInstallButton();
            return;
        }

        final Model.Item it = targetItem;
        activeCheatItem = it;
        final String friendlyVer = VersionMap.getVersionLabel(game.tid, bid, null, availableBids);
        tvSectionTitle.setText("// CARREGANDO MODS DE " + friendlyVer.toUpperCase(Locale.ROOT) + "…");

        Util.POOL.execute(() -> {
            try {
                byte[] data = Sources.read(it, it.files.get(0));
                final List<CheatParser.Block> parsed = CheatParser.parse(new String(data, StandardCharsets.UTF_8));
                runOnUiThread(() -> {
                    functionalCheats.clear();
                    masterBlocks.clear();
                    infoBlock = null;
                    cheatItems.clear();

                    if (parsed.isEmpty()) {
                        tvSectionTitle.setText("// NENHUM CÓDIGO RECONHECIDO");
                        adapter.notifyDataSetChanged();
                        updateInstallButton();
                        return;
                    }

                    // Separação inteligente: Card Informativo vs Cheats Funcionais
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

                    // 1. Dados Globais do Patch (TYPE_INFO, sem checkbox)
                    if (infoBlock != null) {
                        cheatItems.add(new CheatItem(TYPE_INFO, infoBlock, infoBlock.name(), false));
                    }

                    // 2. Cheats Funcionais (TYPE_CHEAT, com checkboxes customizados)
                    for (CheatParser.Block b : functionalCheats) {
                        String rawName = b.name();
                        String cleanName = rawName.replaceAll("^(?:#?\\d+|\\d+)\\s*[\\.:\\-\\)]+\\s*", "").trim();
                        if (cleanName.isEmpty()) cleanName = rawName;
                        cheatItems.add(new CheatItem(TYPE_CHEAT, b, cleanName, true));
                    }

                    tvSectionTitle.setText("// " + functionalCheats.size() + " MODS DISPONÍVEIS NA VERSÃO ATIVA");
                    adapter.notifyDataSetChanged();
                    updateInstallButton();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    tvSectionTitle.setText("// ERRO AO CARREGAR MODS: " + e.getMessage());
                    cheatItems.clear();
                    adapter.notifyDataSetChanged();
                    updateInstallButton();
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

        if (currentBid == null || !Util.isHex16(currentBid)) {
            Ui.toast(this, "Informe um Build ID válido com 16 dígitos.");
            return;
        }

        final List<CheatParser.Block> sel = new ArrayList<>();
        for (CheatItem item : cheatItems) {
            if (item.type == TYPE_CHEAT && item.checked) {
                sel.add(item.block);
            }
        }
        if (sel.isEmpty()) {
            Ui.toast(this, "Marque ao menos um mod para instalar.");
            return;
        }

        String verLabel = VersionMap.getVersionLabel(game.tid, currentBid, null, availableBids);
        EdenScanner.setUserSelectedBid(this, game.tid, currentBid, verLabel);

        // Um único Master Code (pasta própria) + uma pasta por cheat, sem Master repetido.
        // O Eden junta todos os cheats ativos em um programa de no máximo 1024 palavras.
        final Map<String, byte[]> files = CheatParser.buildFolders(sel, masterBlocks, currentBid);
        final String warn = CheatParser.budgetWarning(sel, masterBlocks);
        if (warn != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Limite de código do Eden")
                    .setMessage(warn + "\n\nInstalar mesmo assim?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Instalar", (d, w) -> writeCheatFiles(files))
                    .show();
            return;
        }
        writeCheatFiles(files);
    }

    private void writeCheatFiles(final Map<String, byte[]> files) {
        btnInstallCheats.setEnabled(false);
        btnInstallCheats.setText("GRAVANDO NO EDEN…");
        Util.POOL.execute(() -> {
            String res;
            try {
                res = SafStore.install(this, game.tid, files);
            } catch (Exception e) {
                res = "Erro: " + e.getMessage();
            }
            final String msg = res;
            runOnUiThread(() -> {
                btnInstallCheats.setEnabled(true);
                updateInstallButton();
                Ui.toast(this, "✔ " + msg + "\nAbra o jogo no Eden e ative em Propriedades > Add-ons!"
                        + "\nMantenha o \"" + CheatParser.MASTER_FOLDER + "\" marcado.");
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
                        + "4. Toque no card de Build ID acima para selecionar a sua versão. Os mods compatíveis aparecem na hora.")
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
            selectBid(matchedBid);
            String ver = VersionMap.getVersionLabel(game.tid, matchedBid, null, availableBids);
            Ui.toast(this, "Versão detectada com sucesso da ROM: " + ver + "!");
        } else {
            Ui.toast(this, "Arquivo: " + name + "\nToque no card de Build ID acima para escolher.");
        }
    }

    // RecyclerView Adapter
    private class CheatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return cheatItems.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_INFO) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cheat_info, parent, false);
                return new InfoViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cheat_layout, parent, false);
                return new CheatViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CheatItem item = cheatItems.get(position);
            if (holder instanceof InfoViewHolder) {
                InfoViewHolder vh = (InfoViewHolder) holder;
                vh.tvInfoTitle.setText(item.cleanName);
            } else if (holder instanceof CheatViewHolder) {
                CheatViewHolder vh = (CheatViewHolder) holder;
                vh.tvCheatName.setText(item.cleanName);
                vh.cbCheatActive.setOnCheckedChangeListener(null);
                vh.cbCheatActive.setChecked(item.checked);

                // Efeito visual na borda do Card de acordo com o estado do checkbox
                updateCardVisual(vh.cvCheatCard, item.checked);

                vh.cbCheatActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.checked = isChecked;
                    updateCardVisual(vh.cvCheatCard, isChecked);
                    updateInstallButton();
                });

                vh.itemView.setOnClickListener(v -> vh.cbCheatActive.toggle());
            }
        }

        private void updateCardVisual(MaterialCardView card, boolean isChecked) {
            if (isChecked) {
                card.setStrokeColor(Color.parseColor("#00D4FF")); // Ciano Neon ativo
            } else {
                card.setStrokeColor(Color.parseColor("#1F2637")); // Borda sutil inativa
            }
        }

        @Override
        public int getItemCount() {
            return cheatItems.size();
        }
    }

    static class InfoViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInfoTitle;
        final TextView tvInfoBody;

        InfoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInfoTitle = itemView.findViewById(R.id.tvInfoTitle);
            tvInfoBody = itemView.findViewById(R.id.tvInfoBody);
        }
    }

    static class CheatViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cvCheatCard;
        final TextView tvCheatName;
        final TextView tvCheatCategory;
        final MaterialCheckBox cbCheatActive;

        CheatViewHolder(@NonNull View itemView) {
            super(itemView);
            cvCheatCard = itemView.findViewById(R.id.cvCheatCard);
            tvCheatName = itemView.findViewById(R.id.tvCheatName);
            tvCheatCategory = itemView.findViewById(R.id.tvCheatCategory);
            cbCheatActive = itemView.findViewById(R.id.cbCheatActive);
        }
    }
}
