package com.example.edencheats;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {
    private Sources.Source[] sources;
    private Spinner spinner;
    private android.widget.EditText search;
    private LinearLayout scanTile;
    private TextView status;
    private Ui.RowAdapter adapter;
    private List<Model.Game> all = new ArrayList<>();
    private final List<Model.Game> shown = new ArrayList<>();
    private int generation = 0;
    private int current = -1;

    private ListView list;
    private Ui.ProgressStrip progress;
    private LinearLayout emptyCard;
    private TextView emptyTitle;
    private TextView emptyHint;
    private Button emptyPrimary;
    private Button emptySecondary;
    private LinearLayout setupCard;
    private TextView setupTitle;
    private TextView setupDesc;
    private Button setupBtn;

    private boolean filterMyGames = false;
    private TextView chipMyGames;
    private TextView chipAllGames;
    private TextView chipVersion;
    private TextView chipFavs;
    private boolean filterVersion = false;
    private boolean filterFavs = false;
    private LinearLayout updateBanner;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Names.ensure(this);
        sources = Sources.all();

        // Se já houver jogos detectados no Eden, abre direto na aba "Meus Jogos"
        int scannedCount = EdenScanner.getCount(this);
        if (scannedCount > 0) filterMyGames = true;

        LinearLayout root = Ui.page(this);

        // Cabeçalho: logo num quadrado de destaque, nome do app e versão
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("⚡");
        logo.setTextSize(20);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.shape(this, Ui.ACCENT, 14, 0, 0));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44));
        lpLogo.setMargins(0, 0, Ui.dp(this, 12), 0);
        header.addView(logo, lpLogo);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("SwitchForge");
        title.setTextSize(22);
        title.setTextColor(Ui.TEXT);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleCol.addView(title);

        TextView sysStatus = new TextView(this);
        sysStatus.setText(Lang.s(R.string.main_terminal_status, versionName()));
        sysStatus.setTextSize(12);
        sysStatus.setTextColor(Ui.MUTED);
        titleCol.addView(sysStatus);

        header.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int pxB = Ui.dp(this, 12);
        int pyB = Ui.dp(this, 4);

        // Seletor de idioma: pílula "🌐 PT" que abre a escolha entre Português, English e Español
        TextView langBadge = new TextView(this);
        langBadge.setText("\uD83C\uDF10 " + Lang.shortCode());
        langBadge.setTextSize(12);
        langBadge.setTextColor(Ui.TEXT);
        langBadge.setTypeface(Typeface.DEFAULT_BOLD);
        langBadge.setBackground(Ui.shape(this, Ui.CARD, 18, 1, Ui.BORDER));
        langBadge.setPadding(pxB, pyB, pxB, pyB);
        langBadge.setClickable(true);
        langBadge.setMinHeight(Ui.dp(this, 36));
        langBadge.setGravity(Gravity.CENTER);
        Ui.addPressEffect(langBadge);
        langBadge.setOnClickListener(v -> Lang.showPicker(this));
        LinearLayout.LayoutParams lpLang = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLang.setMargins(Ui.dp(this, 6), 0, 0, 0);
        header.addView(langBadge, lpLang);

        // Botão "?" : guia rápido de uso
        TextView helpBadge = new TextView(this);
        helpBadge.setText("?");
        helpBadge.setTextSize(12);
        helpBadge.setTextColor(Ui.ACCENT);
        helpBadge.setTypeface(Typeface.DEFAULT_BOLD);
        helpBadge.setGravity(Gravity.CENTER);
        helpBadge.setBackground(Ui.shape(this, Ui.CARD, 18, 1, Ui.BORDER));
        helpBadge.setMinWidth(Ui.dp(this, 36));
        helpBadge.setMinHeight(Ui.dp(this, 36));
        helpBadge.setClickable(true);
        helpBadge.setContentDescription(Lang.s(R.string.ux_help_title));
        Ui.addPressEffect(helpBadge);
        helpBadge.setOnClickListener(v -> Ui.helpDialog(this));
        LinearLayout.LayoutParams lpHelp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpHelp.setMargins(Ui.dp(this, 6), 0, 0, 0);
        header.addView(helpBadge, lpHelp);

        root.addView(header, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Barra de busca fixa no topo: continua visível enquanto a lista rola.
        search = Ui.edit(this, Lang.s(R.string.main_search_hint));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                applyFilter();
            }
        });
        root.addView(Ui.searchBox(this, search), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // Faixa fina de progresso: acende enquanto o app carrega a base ou varre o Eden.
        progress = new Ui.ProgressStrip(this);
        LinearLayout.LayoutParams lpProg = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 3));
        lpProg.setMargins(Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 4));
        root.addView(progress, lpProg);

        // Tudo abaixo vira o "cabeçalho" da lista e rola junto com ela: sobra mais espaço para os jogos.
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);

        // Aviso de versão nova (aparece só quando existe um release mais recente no GitHub).
        updateBanner = new LinearLayout(this);
        updateBanner.setOrientation(LinearLayout.VERTICAL);
        updateBanner.setBackground(Ui.shape(this, Ui.ACCENT_BG, 18, 1, Ui.alpha(Ui.GOOD, 0x99)));
        int pBan = Ui.dp(this, 14);
        updateBanner.setPadding(pBan, pBan, pBan, pBan);
        updateBanner.setVisibility(View.GONE);
        top.addView(updateBanner, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));
        showUpdateBanner(UpdateCheck.cached(this, versionName()));
        UpdateCheck.check(this, versionName(), this::showUpdateBanner);

        // Cartão "Próximo passo": guia quem acabou de instalar (1. pasta do Eden, 2. varrer jogos).
        setupCard = new LinearLayout(this);
        setupCard.setOrientation(LinearLayout.VERTICAL);
        setupCard.setBackground(Ui.shape(this, Ui.ACCENT_BG, 18, 1, Ui.alpha(Ui.ACCENT, 0x88)));
        int pSetup = Ui.dp(this, 14);
        setupCard.setPadding(pSetup, pSetup, pSetup, pSetup);
        setupTitle = Ui.text(this, "", 15, Ui.TEXT, true);
        setupCard.addView(setupTitle);
        setupDesc = Ui.text(this, "", 12, Ui.MUTED, false);
        setupDesc.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        setupCard.addView(setupDesc);
        setupBtn = Ui.button(this, "", true, null);
        setupCard.addView(setupBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0));
        top.addView(setupCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));
        updateSetupCard();

        // 1. Ações rápidas em grade adaptável (3 colunas em telas largas, 2 no celular).
        View[] quickActions = new View[] {
                scanTile = Ui.tile(this, "🔎", Lang.s(R.string.main_btn_scan), Lang.s(R.string.ux2_sub_scan), true, v -> doScan()),
                Ui.tile(this, "🧩", Lang.s(R.string.main_btn_active_cheats), Lang.s(R.string.ux2_sub_mods), false, v ->
                        startActivity(new Intent(this, InstalledActivity.class))),
                Ui.tile(this, "💾", Lang.s(R.string.main_btn_saves), Lang.s(R.string.ux2_sub_saves), false, v ->
                        startActivity(new Intent(this, SaveManagerActivity.class))),
                Ui.tile(this, "📋", Lang.s(R.string.main_btn_paste_code), Lang.s(R.string.ux2_sub_paste), false, v ->
                        startActivity(new Intent(this, CustomActivity.class))),
                Ui.tile(this, "🛠", Lang.s(R.string.main_btn_tools), Lang.s(R.string.ux2_sub_tools), false, v ->
                        startActivity(new Intent(this, ToolsActivity.class)))
        };
        int actionColumns = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2;
        for (int start = 0; start < quickActions.length; start += actionColumns) {
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < actionColumns && start + col < quickActions.length; col++) {
                actionRow.addView(quickActions[start + col],
                        Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
            }
            top.addView(actionRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        }
        // Apoio ao projeto: card dourado em largura total, separado das ferramentas.
        top.addView(Ui.supportCard(this, "☕", Lang.s(R.string.main_btn_coffee),
                        Lang.s(R.string.main_support_hint), v -> Util.openDonation(this)),
                Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 2. Console Unificado de Controle (Filtros, Servidor, Trio de Ações e Busca)
        LinearLayout console = new LinearLayout(this);
        console.setOrientation(LinearLayout.VERTICAL);
        console.setBackground(Ui.shape(this, Ui.CARD, 18, 1, Ui.BORDER));
        int pConsole = Ui.dp(this, 10);
        console.setPadding(pConsole, pConsole, pConsole, pConsole);

        // Linha Superior: Cabeçalho do Console + Trio Unificado de Ações
        LinearLayout consoleTop = new LinearLayout(this);
        consoleTop.setOrientation(LinearLayout.HORIZONTAL);
        consoleTop.setGravity(Gravity.CENTER_VERTICAL);

        // Faixa de ações: três botões iguais, com ícone e texto curto.
        View fpsPill = Ui.actionButton(this, "⚡", Ui.GOOD, "60 FPS", v -> {
            if (spinner != null) {
                for (int i = 0; i < sources.length; i++) {
                    if (sources[i].label.contains("60FPS")) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        });
        View refreshPill = Ui.actionButton(this, "🔄", Ui.ACCENT, Lang.s(R.string.main_pill_reload), v -> load(true));
        View updatePill = Ui.actionButton(this, "🚀", Ui.WARN, Lang.s(R.string.main_pill_update), v -> showUpdateDialog());
        consoleTop.addView(fpsPill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lpRef = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpRef.setMargins(Ui.dp(this, 6), 0, 0, 0);
        consoleTop.addView(refreshPill, lpRef);
        LinearLayout.LayoutParams lpUpd = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpUpd.setMargins(Ui.dp(this, 6), 0, 0, 0);
        consoleTop.addView(updatePill, lpUpd);
        console.addView(consoleTop);

        // Seletor de Servidor em container tech escuro
        LinearLayout spinnerBox = new LinearLayout(this);
        spinnerBox.setOrientation(LinearLayout.HORIZONTAL);
        spinnerBox.setGravity(Gravity.CENTER_VERTICAL);
        spinnerBox.setBackground(Ui.shape(this, Ui.INPUT_BG, 12, 1, Ui.BORDER));
        spinnerBox.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));

        spinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (Sources.Source s : sources) labels.add(s.label);
        ArrayAdapter<String> sa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getDropDownView(int pos, View cv, ViewGroup parent) {
                View v = super.getDropDownView(pos, cv, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(pos == spinner.getSelectedItemPosition() ? Ui.ACCENT : Ui.TEXT);
                    tv.setTextSize(13);
                    tv.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
                    tv.setBackgroundColor(0x00000000);
                    int ph = Ui.dp(MainActivity.this, 16);
                    int pv = Ui.dp(MainActivity.this, 12);
                    tv.setPadding(ph, pv, ph, pv);
                }
                return v;
            }

            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Ui.ACCENT);
                    ((TextView) v).setTextSize(12);
                    ((TextView) v).setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
                }
                return v;
            }
        };
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPopupBackgroundDrawable(Ui.shape(this, Ui.CARD, 18, 1, Ui.BORDER));
        spinner.setAdapter(sa);
        spinnerBox.addView(spinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        console.addView(spinnerBox, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Abas de Filtro: [ ◈ Meus Jogos (X) ] vs [ ◈ Todos os Jogos ]
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        chipMyGames = Ui.chip(this, Lang.s(R.string.main_chip_my_games, scannedCount), filterMyGames, v -> {
            filterMyGames = true;
            updateFilterChips();
            applyFilter();
            Ui.replayListAnimation(list);
        });

        chipAllGames = Ui.chip(this, Lang.s(R.string.main_chip_all_games), !filterMyGames, v -> {
            filterMyGames = false;
            updateFilterChips();
            applyFilter();
            Ui.replayListAnimation(list);
        });

        LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChip.setMargins(0, 0, Ui.dp(this, 8), 0);
        filterRow.addView(chipMyGames, lpChip);
        filterRow.addView(chipAllGames, lpChip);

        // Filtros extras: só jogos com cheats para a versão instalada, e só favoritos.
        chipVersion = Ui.chip(this, Lang.s(R.string.ux3_chip_version), filterVersion, v -> {
            filterVersion = !filterVersion;
            updateFilterChips();
            applyFilter();
            Ui.replayListAnimation(list);
        });
        chipFavs = Ui.chip(this, Lang.s(R.string.ux3_chip_favs), filterFavs, v -> {
            filterFavs = !filterFavs;
            updateFilterChips();
            applyFilter();
            Ui.replayListAnimation(list);
        });
        filterRow.addView(chipVersion, lpChip);
        filterRow.addView(chipFavs);
        android.widget.HorizontalScrollView filterScroll = new android.widget.HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.addView(filterRow);
        console.addView(filterScroll, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        top.addView(console, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        top.addView(status);

        emptyCard = buildEmptyCard();
        top.addView(emptyCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        list = new ListView(this);
        list.addHeaderView(top, null, false);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        Ui.styleList(list);
        list.setOnItemClickListener((p, v, pos, id) -> {
            int idx = pos - list.getHeaderViewsCount();
            if (idx < 0 || idx >= shown.size()) return;
            Session.game = shown.get(idx);
            startActivity(new Intent(this, DetailActivity.class));
        });
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            int idx = pos - list.getHeaderViewsCount();
            if (idx < 0 || idx >= shown.size()) return false;
            Model.Game g = shown.get(idx);
            boolean now = Favorites.toggle(this, g.tid);
            Ui.toast(this, Lang.s(now ? R.string.ux2_fav_added : R.string.ux2_fav_removed, g.display()));
            applyFilter();
            return true;
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));

        setContentView(root);
        root.post(this::maybeAutoSetup);
        root.post(this::maybeAutoScan);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current) {
                    current = position;
                    load(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onFolderChanged() {
        super.onFolderChanged();
        doScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFilterChips();
        updateSetupCard();
        applyFilter();
    }

    private void updateFilterChips() {
        int count = EdenScanner.getCount(this);
        chipMyGames.setText(Lang.s(R.string.main_chip_my_games, count));
        if (filterMyGames) {
            chipMyGames.setTextColor(Ui.ON_ACCENT);
            chipMyGames.setBackground(Ui.shape(this, Ui.ACCENT, 14, 0, 0));
            chipAllGames.setTextColor(Ui.ICON_MUTED);
            chipAllGames.setBackground(Ui.shape(this, Ui.PILL_BG, 14, 1, Ui.BORDER));
        } else {
            chipAllGames.setTextColor(Ui.ON_ACCENT);
            chipAllGames.setBackground(Ui.shape(this, Ui.ACCENT, 14, 0, 0));
            chipMyGames.setTextColor(Ui.ICON_MUTED);
            chipMyGames.setBackground(Ui.shape(this, Ui.PILL_BG, 14, 1, Ui.BORDER));
        }
        styleToggle(chipVersion, filterVersion);
        styleToggle(chipFavs, filterFavs);
    }

    private void styleToggle(TextView chip, boolean on) {
        if (chip == null) return;
        chip.setTextColor(on ? Ui.ON_ACCENT : Ui.ICON_MUTED);
        chip.setBackground(on ? Ui.shape(this, Ui.ACCENT, 14, 0, 0) : Ui.shape(this, Ui.PILL_BG, 14, 1, Ui.BORDER));
    }

    /** Jogo tem cheat para a versão (Build ID) que o Eden detectou no aparelho? */
    private boolean hasMyVersion(Model.Game g) {
        String bid = EdenScanner.getBuildId(this, g.tid);
        if (bid == null || bid.isEmpty()) return false;
        for (Model.Item it : g.items) {
            if (it.cheat && bid.equalsIgnoreCase(it.bid)) return true;
        }
        return false;
    }

    /** Faixa "Nova versão disponível" no topo da lista (dispensável). */
    private void showUpdateBanner(final UpdateCheck.Info info) {
        if (updateBanner == null || info == null) return;
        updateBanner.removeAllViews();
        updateBanner.addView(Ui.text(this, Lang.s(R.string.ux3_update_title, info.tag), 15, Ui.TEXT, true));
        TextView d = Ui.text(this, Lang.s(R.string.ux3_update_desc), 12, Ui.MUTED, false);
        d.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        updateBanner.addView(d);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button dl = Ui.button(this, Lang.s(R.string.ux3_update_btn), true, v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.url)));
            } catch (Exception ignored) {
                Ui.toast(this, info.url);
            }
        });
        Button later = Ui.button(this, Lang.s(R.string.ux3_update_dismiss), false, v -> {
            UpdateCheck.dismiss(this, info);
            updateBanner.setVisibility(View.GONE);
        });
        row.addView(dl, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        row.addView(later, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        updateBanner.addView(row);
        updateBanner.setVisibility(View.VISIBLE);
    }

    /**
     * Primeira abertura: se o Eden estiver instalado e a pasta ainda não foi configurada, oferece
     * configurar na hora. Depois que a pasta é escolhida, onFolderChanged() já varre os jogos sozinho.
     */
    private void maybeAutoSetup() {
        if (isFinishing() || SafStore.getTree(this) != null || EdenDetector.wasAsked(this)) return;
        final EdenDetector.Found f = EdenDetector.best(this);
        if (f == null) return; // Eden não encontrado: tenta de novo na próxima abertura
        EdenDetector.markAsked(this);
        new android.app.AlertDialog.Builder(this)
                .setTitle(Lang.s(R.string.setup_found_title))
                .setMessage(Lang.s(R.string.setup_found_msg, f.label, f.pkg))
                .setPositiveButton(Lang.s(R.string.setup_btn_configure), (d, w) -> {
                    SafStore.setPkg(this, f.pkg);
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        folderDialog(); // Android 13+: explica como escolher uma pasta comum
                    } else {
                        SafStore.pick(this, REQ_FOLDER); // já abre apontando para a pasta do Eden
                    }
                })
                .setNegativeButton(Lang.s(R.string.setup_btn_later), null)
                .show();
    }

    /** Atualiza o cartão "Próximo passo" conforme o que já foi configurado. */
    private void updateSetupCard() {
        if (setupCard == null) return;
        // Passo 1 em destaque; a varredura só acende depois que a pasta do Eden estiver configurada.
        if (scanTile != null) {
            boolean ready = SafStore.getTree(this) != null;
            Ui.tileState(scanTile, ready, ready);
        }
        if (SafStore.getTree(this) == null) {
            setupTitle.setText(Lang.s(R.string.ux_next_title_folder));
            setupDesc.setText(Lang.s(R.string.ux_next_desc_folder));
            setupBtn.setText(Lang.s(R.string.ux_next_btn_folder));
            setupBtn.setOnClickListener(v -> startFolderSetup());
            setupCard.setVisibility(View.VISIBLE);
        } else if (EdenScanner.getCount(this) == 0) {
            setupTitle.setText(Lang.s(R.string.ux_next_title_scan));
            setupDesc.setText(Lang.s(R.string.ux_next_desc_scan));
            setupBtn.setText(Lang.s(R.string.ux_next_btn_scan));
            setupBtn.setOnClickListener(v -> doScan());
            setupCard.setVisibility(View.VISIBLE);
        } else {
            setupCard.setVisibility(View.GONE);
        }
    }

    /** Configura a pasta: usa o Eden detectado e abre o seletor já apontando para ele (Android 12-). */
    private void startFolderSetup() {
        EdenDetector.Found f = EdenDetector.best(this);
        if (f != null) SafStore.setPkg(this, f.pkg);
        if (f != null && android.os.Build.VERSION.SDK_INT < 33) {
            SafStore.pick(this, REQ_FOLDER);
        } else {
            folderDialog();
        }
    }

    /** Ao abrir o app: atualiza a lista de jogos sozinho (a cada 6 h, ou 10 min enquanto estiver vazia). */
    private void maybeAutoScan() {
        if (isFinishing() || SafStore.getTree(this) == null) return;
        long limit = EdenScanner.getCount(this) == 0 ? 10 * 60 * 1000L : 6 * 60 * 60 * 1000L;
        if (System.currentTimeMillis() - EdenDetector.lastScan(this) < limit) return;
        doScan(true);
    }

    private void doScan() {
        doScan(false);
    }

    private void doScan(final boolean silent) {
        if (SafStore.getTree(this) == null) {
            if (!silent) folderDialog();
            return;
        }
        status.setText(Lang.s(R.string.main_scan_starting));
        status.setVisibility(View.VISIBLE);
        emptyCard.setVisibility(View.GONE);
        progress.setActive(true);
        Util.POOL.execute(() -> {
            try {
                final int count = EdenScanner.scanEden(this, msg -> runOnUiThread(() -> status.setText(msg)));
                EdenDetector.markScanned(this);
                runOnUiThread(() -> {
                    progress.setActive(false);
                    if (!silent) Ui.toast(this, Lang.s(R.string.main_scan_done, count));
                    if (count > 0 && !silent) filterMyGames = true;
                    updateFilterChips();
                    updateSetupCard();
                    applyFilter();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    progress.setActive(false);
                    status.setText(Lang.s(R.string.main_scan_error, e.getMessage()));
                    if (!silent) Ui.toast(this, Lang.s(R.string.main_error_prefix, e.getMessage()));
                });
            }
        });
    }

    private void load(final boolean refresh) {
        if (current < 0) return;
        final Sources.Source src = sources[current];
        final int gen = ++generation;
        all = new ArrayList<>();
        applyFilter();
        status.setText(Lang.s(R.string.main_syncing_db));
        progress.setActive(true);
        Util.POOL.execute(() -> {
            try {
                final List<Model.Game> games = src.load(MainActivity.this, refresh, msg -> runOnUiThread(() -> {
                    if (gen == generation) status.setText(msg);
                }));

                // Se houver pasta configurada mas ainda nenhum jogo escaneado, realiza a primeira varredura silenciosa
                if (SafStore.getTree(MainActivity.this) != null && EdenScanner.getCount(MainActivity.this) == 0) {
                    try {
                        EdenScanner.scanEden(MainActivity.this, null);
                    } catch (Exception ignored) {}
                }

                runOnUiThread(() -> {
                    if (gen != generation) return;
                    progress.setActive(false);
                    all = games;
                    updateFilterChips();
                    applyFilter();
                    Ui.replayListAnimation(list);
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    if (gen != generation) return;
                    progress.setActive(false);
                    status.setText(Lang.s(R.string.main_connection_error, e.getMessage()));
                });
            }
        });
    }

    private static boolean matches(Model.Game g, String q) {
        if (g.display().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (g.tid.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (Model.Item it : g.items) {
            if (!it.bid.isEmpty() && it.bid.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        Session.query = q;
        shown.clear();
        adapter.rows.clear();
        emptyCard.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);

        // Favoritos (toque longo no jogo) aparecem primeiro e com uma estrela.
        final java.util.Set<String> favs = Favorites.all(this);
        List<Model.Game> ordered = all;
        if (!favs.isEmpty()) {
            ordered = new ArrayList<>(all);
            java.util.Collections.sort(ordered, (a, b) ->
                    Boolean.compare(favs.contains(b.tid), favs.contains(a.tid)));
        }
        for (Model.Game g : ordered) {
            boolean isInst = EdenScanner.isInstalled(this, g.tid);
            if (filterMyGames && !isInst) continue;
            if (filterFavs && !favs.contains(g.tid)) continue;
            if (filterVersion && !hasMyVersion(g)) continue;
            if (!q.isEmpty() && !matches(g, q)) continue;

            shown.add(g);
            int n = g.items.size();
            String id = g.tid.isEmpty() ? Lang.s(R.string.main_unknown_tid) : g.tid;
            String detectedBid = EdenScanner.getBuildId(this, g.tid);

            if (g.name == null || g.name.isEmpty() || g.name.equalsIgnoreCase(g.tid)) {
                String known = Names.get(g.tid);
                if (known != null && !known.isEmpty()) {
                    g.name = known;
                } else {
                    String scannedTitle = EdenScanner.getTitle(this, g.tid);
                    if (scannedTitle != null && !scannedTitle.isEmpty()) {
                        g.name = scannedTitle;
                    }
                }
            }

            String sub = Lang.s(n == 1 ? R.string.main_sub_package_one : R.string.main_sub_package_many, id, n);
            if (detectedBid != null) {
                sub = "⚡ BID: " + detectedBid + " · " + sub;
            }
            String title = (favs.contains(g.tid) ? "⭐ " : "") + g.display();
            adapter.rows.add(new String[]{title, sub, isInst ? "1" : "0", g.tid});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;

        int totalInstalled = EdenScanner.getCount(this);
        String s = Lang.s(R.string.main_status_count, shown.size(), all.size())
                + (filterMyGames ? " " + Lang.s(R.string.main_status_filtering, totalInstalled) : "");
        if (shown.isEmpty() && filterMyGames) {
            s = Lang.s(R.string.main_none_found, totalInstalled);
        } else if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\n" + Lang.s(R.string.main_build_id_not_found);
        }
        status.setText(s);
        updateEmptyState(q);
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** Cartão de "nada por aqui" com botões que resolvem o problema (varrer, ver todos, limpar busca). */
    private LinearLayout buildEmptyCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.shape(this, Ui.CARD, 14, 1, Ui.BORDER));
        int p = Ui.dp(this, 20);
        card.setPadding(p, p, p, p);

        TextView ico = Ui.text(this, "🎮", 34, Ui.ACCENT, false);
        ico.setGravity(Gravity.CENTER);
        card.addView(ico);

        emptyTitle = Ui.text(this, "", 16, Ui.TEXT, true);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        card.addView(emptyTitle);

        emptyHint = Ui.text(this, "", 13, Ui.MUTED, false);
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setPadding(0, 0, 0, Ui.dp(this, 10));
        card.addView(emptyHint);

        emptyPrimary = Ui.button(this, "", true, null);
        card.addView(emptyPrimary, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        emptySecondary = Ui.button(this, "", false, null);
        card.addView(emptySecondary, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        card.setVisibility(View.GONE);
        return card;
    }

    private void updateEmptyState(String q) {
        if (!shown.isEmpty()) return;
        final String raw = search.getText().toString().trim();
        if (!q.isEmpty()) {
            emptyTitle.setText(Lang.s(R.string.main_empty_search_title));
            String hint = Lang.s(R.string.main_empty_search_hint, raw);
            if (Util.isHex16(q.toUpperCase(Locale.ROOT))) hint += "\n" + Lang.s(R.string.main_build_id_not_found);
            emptyHint.setText(hint);
            emptyPrimary.setText(Lang.s(R.string.main_empty_btn_clear));
            emptyPrimary.setOnClickListener(v -> search.setText(""));
        } else if (filterMyGames) {
            if (setupCard != null && setupCard.getVisibility() == View.VISIBLE) return; // o cartão "Próximo passo" já guia
            emptyTitle.setText(Lang.s(R.string.main_empty_mine_title));
            emptyHint.setText(Lang.s(R.string.main_empty_mine_hint));
            emptyPrimary.setText(Lang.s(R.string.main_empty_btn_scan));
            emptyPrimary.setOnClickListener(v -> doScan());
        } else {
            return;
        }
        emptySecondary.setVisibility(filterMyGames ? View.VISIBLE : View.GONE);
        emptySecondary.setText(Lang.s(R.string.main_empty_btn_all));
        emptySecondary.setOnClickListener(v -> {
            filterMyGames = false;
            updateFilterChips();
            applyFilter();
            Ui.replayListAnimation(list);
        });
        status.setVisibility(View.GONE);
        emptyCard.setVisibility(View.VISIBLE);
    }

    private void showUpdateDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p);

        root.addView(Ui.text(this, Lang.s(R.string.main_update_task_title), 18, Ui.TEXT, true));
        root.addView(Ui.text(this, Lang.s(R.string.main_update_task_desc), 12, Ui.MUTED, false));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.shape(this, Ui.CARD, 8, 1, Ui.BORDER));
        int pb = Ui.dp(this, 10);
        box.setPadding(pb, pb, pb, pb);
        TextView infoTxt = Ui.text(this, AutoUpdater.getLastUpdateInfo(this), 12, Ui.TEXT, false);
        box.addView(infoTxt);
        root.addView(box, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        TextView progTxt = Ui.text(this, "", 12, Ui.ACCENT, false);
        root.addView(progTxt, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        Button runBtn = Ui.button(this, Lang.s(R.string.main_update_start), true, null);
        root.addView(runBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        b.setView(root);
        final android.app.AlertDialog dlg = b.create();

        runBtn.setOnClickListener(v -> {
            if (AutoUpdater.isRunning()) {
                Ui.toast(this, Lang.s(R.string.main_update_running));
                return;
            }
            runBtn.setEnabled(false);
            runBtn.setText(Lang.s(R.string.main_update_updating));
            AutoUpdater.runUpdate(this, new AutoUpdater.UpdateListener() {
                @Override
                public void onProgress(final String message, final int percent) {
                    runOnUiThread(() -> progTxt.setText(message + " (" + percent + "%)"));
                }

                @Override
                public void onSuccess(final AutoUpdater.UpdateResult result) {
                    runOnUiThread(() -> {
                        dlg.dismiss();
                        Ui.alert(MainActivity.this, Lang.s(R.string.main_update_done_title), result.summary());
                        load(true);
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(() -> {
                        runBtn.setEnabled(true);
                        runBtn.setText(Lang.s(R.string.main_update_retry));
                        progTxt.setText(Lang.s(R.string.main_error_prefix, error));
                        Ui.toast(MainActivity.this, error);
                    });
                }
            });
        });

        dlg.show();
    }
}
