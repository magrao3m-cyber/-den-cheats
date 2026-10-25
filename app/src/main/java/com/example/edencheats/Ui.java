package com.example.edencheats;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

/** Helpers de interface com design minimalista, tecnológico e moderno (A Forja Cibernética). */
final class Ui {
    private Ui() {}

    // Paleta Minimalista & Tecnológica (Cyber Dark / OLED / A Forja Cibernética)
    static final int BG = 0xFF0A0D14;            // Fundo escuro profundo obsidian (#0A0D14)
    static final int CARD = 0xFF121620;          // Superfície de cards tech (#121620)
    static final int CARD_GAMER = 0xFF161B26;    // Fundo elevado estilo Gamer (#161B26)
    static final int CARD_HOVER = 0xFF1A2234;    // Card selecionado / hover
    static final int BORDER = 0xFF1F2637;        // Borda sutil de 1dp (#1F2637)
    static final int BORDER_MUTED = 0xFF1A2232;  // Borda inativa / apagada
    static final int BORDER_GLOW = 0xFF00D4FF;   // Borda com brilho ciano ativo
    static final int ACCENT = 0xFF00D4FF;        // Ciano elétrico neon principal (#00D4FF)
    static final int ACCENT_BG = 0xFF0A2233;     // Fundo translúcido para badges ciano
    static final int TEXT = 0xFFF0F6FC;          // Branco puro de alto contraste (#F0F6FC)
    static final int MUTED = 0xFF8B949E;         // Cinza tecnológico legível (#8B949E)
    static final int ICON_MUTED = 0xFF5C6B89;    // Cinza-azulado médio (#5C6B89) para ícones outline
    static final int ID_COLOR = 0xFF6272A4;      // Cinza opaco (#6272A4) para Title ID
    static final int GOOD = 0xFF00F5A0;          // Verde menta neon (#00F5A0)
    static final int LED_GREEN = 0xFF00FFA3;     // Verde menta brilhante para LED (#00FFA3)
    static final int GOOD_BG = 0xFF062A1F;       // Fundo de badge de sucesso
    static final int DANGER = 0xFFFF4B6E;        // Vermelho neon para apagar/avisos
    static final int DANGER_BG = 0xFF2D0F18;
    static final int WARN = 0xFFFFB700;          // Âmbar / Ouro neon para atualizações (#FFB700)
    static final int WARN_BG = 0xFF2D2305;

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable shape(Context c, int bgColor, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) {
            g.setStroke(dp(c, strokeDp), strokeColor);
        }
        return g;
    }

    static GradientDrawable lateralStripe(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = dp(c, radiusDp);
        g.setCornerRadii(new float[]{ r, r, 0, 0, 0, 0, r, r });
        return g;
    }

    /** Fundo com ripple nativo do Android para feedback tátil visual. */
    static RippleDrawable ripple(Context c, int baseColor, int radiusDp, int strokeDp,
                                int strokeColor, int rippleColor) {
        GradientDrawable content = shape(c, baseColor, radiusDp, strokeDp, strokeColor);
        GradientDrawable mask = shape(c, Color.WHITE, radiusDp, 0, Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    /** Gradiente ciano-azul para ações primárias com acabamento premium. */
    static RippleDrawable premiumButtonBackground(Context c) {
        GradientDrawable content = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xFF45E6FF, 0xFF00A6E8, 0xFF0877D5 });
        content.setCornerRadius(dp(c, 12));
        content.setStroke(dp(c, 1), 0xFF66EFFF);
        GradientDrawable mask = shape(c, Color.WHITE, 12, 0, Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(0x550A0D14), content, mask);
    }

    /** Pequena compressão ao tocar, sem interceptar o clique do controle. */
    static void addPressEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.975f).scaleY(0.975f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
            }
            return false;
        });
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView badge(Context c, String s, int bgColor, int textColor, int strokeColor) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(11);
        t.setTextColor(textColor);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setBackground(shape(c, bgColor, 6, 1, strokeColor));
        int px = dp(c, 7);
        int py = dp(c, 3);
        t.setPadding(px, py, px, py);
        return t;
    }

    static TextView chip(Context c, String s, boolean selected, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(11);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        if (selected) {
            t.setTextColor(0xFF0A0D14);
            t.setBackground(shape(c, ACCENT, 14, 0, 0));
        } else {
            t.setTextColor(ICON_MUTED);
            t.setBackground(shape(c, 0x33121620, 14, 1, BORDER));
        }
        int px = dp(c, 11);
        int py = dp(c, 5);
        t.setPadding(px, py, px, py);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    static TextView chipNeon(Context c, String s, boolean selected, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(11);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        if (selected) {
            t.setTextColor(0xFF0A0D14);
            t.setBackground(shape(c, GOOD, 14, 0, 0));
        } else {
            t.setTextColor(GOOD);
            t.setBackground(shape(c, GOOD_BG, 14, 1, GOOD));
        }
        int px = dp(c, 10);
        int py = dp(c, 5);
        t.setPadding(px, py, px, py);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    static LinearLayout page(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, float weight, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        int m = dp(c, margin);
        p.setMargins(m, m, m, m);
        return p;
    }

    static Button button(Context c, String label, View.OnClickListener l) {
        return button(c, label, true, l);
    }

    static Button button(Context c, String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        if (primary) {
            b.setTextColor(0xFF0A0D14);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setBackground(premiumButtonBackground(c));
        } else {
            b.setTextColor(TEXT);
            b.setBackground(ripple(c, CARD, 10, 1, BORDER, 0x44FFFFFF));
        }
        int py = dp(c, 10);
        int px = dp(c, 14);
        b.setPadding(px, py, px, py);
        b.setMinHeight(dp(c, 44));
        b.setElevation(dp(c, 2));
        b.setOnClickListener(l);
        addPressEffect(b);
        return b;
    }

    /** Card Tecnológico Unificado para Ações Rápidas com LED indicador e outline ícones. */
    static LinearLayout techCardButton(Context c, String label, String badgeText, boolean hasLed, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);

        card.setBackground(ripple(c, CARD, 10, 1, hasLed ? 0xFF243346 : BORDER, 0x3300D4FF));

        int px = dp(c, 10);
        int py = dp(c, 9);
        card.setPadding(px, py, px, py);

        // Minúsculo indicador de LED brilhante #00FFA3 no canto interno
        if (hasLed) {
            View led = new View(c);
            int ledSize = dp(c, 6);
            LinearLayout.LayoutParams lpLed = new LinearLayout.LayoutParams(ledSize, ledSize);
            lpLed.setMargins(0, 0, dp(c, 6), 0);
            led.setBackground(shape(c, LED_GREEN, 3, 0, 0));
            card.addView(led, lpLed);
        }

        TextView badge = new TextView(c);
        badge.setText(badgeText);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        if (hasLed) {
            badge.setTextColor(ACCENT);
            badge.setBackground(shape(c, ACCENT_BG, 4, 1, ACCENT));
        } else {
            badge.setTextColor(ICON_MUTED);
            badge.setBackground(shape(c, 0xFF171E2C, 4, 1, BORDER));
        }
        int bPx = dp(c, 5);
        int bPy = dp(c, 2);
        badge.setPadding(bPx, bPy, bPx, bPy);
        card.addView(badge);

        TextView title = new TextView(c);
        title.setText(label);
        title.setTextSize(12);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(c, 7), 0, 0, 0);
        card.addView(title, lp);

        if (l != null) card.setOnClickListener(l);
        card.setMinimumHeight(dp(c, 48));
        card.setElevation(dp(c, 2));
        addPressEffect(card);
        return card;
    }

    /** Pílula de Ação Neutra com Indicador Colorido e Texto Branco para o Console Central. */
    static LinearLayout techActionPill(Context c, String iconSymbol, int iconColor, String label, View.OnClickListener l) {
        LinearLayout pill = new LinearLayout(c);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setClickable(true);
        pill.setFocusable(true);
        pill.setBackground(ripple(c, CARD, 8, 1, BORDER, 0x3300D4FF));

        int px = dp(c, 8);
        int py = dp(c, 5);
        pill.setPadding(px, py, px, py);

        TextView ico = new TextView(c);
        ico.setText(iconSymbol);
        ico.setTextSize(11);
        ico.setTextColor(iconColor);
        ico.setTypeface(Typeface.DEFAULT_BOLD);
        pill.addView(ico);

        TextView txt = new TextView(c);
        txt.setText(label);
        txt.setTextSize(10);
        txt.setTextColor(0xFFFFFFFF); // Texto branco puro conforme especificação
        txt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(c, 4), 0, 0, 0);
        pill.addView(txt, lp);

        if (l != null) pill.setOnClickListener(l);
        pill.setMinimumHeight(dp(c, 36));
        pill.setElevation(dp(c, 1));
        addPressEffect(pill);
        return pill;
    }

    static LinearLayout buttonRow(Context c, Button... buttons) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (Button b : buttons) {
            row.addView(b, lp(c, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        }
        return row;
    }

    static EditText edit(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(13);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setBackground(shape(c, 0xFF0E121A, 8, 1, BORDER));
        int p = dp(c, 10);
        e.setPadding(p, p, p, p);
        return e;
    }

    static EditText monoEdit(Context c, String hint) {
        EditText e = edit(c, hint);
        e.setTypeface(Typeface.MONOSPACE);
        return e;
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }

    static void alert(Context c, String title, String msg) {
        new android.app.AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(Lang.s(R.string.main_ok), null)
                .show();
    }

    /** Lista de cards com visual gamer e indicador lateral: {título, subtítulo, "1" para destacar}. */
    static final class RowAdapter extends BaseAdapter {
        private static final LruCache<String, Bitmap> COVER_CACHE = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
            @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
        };
        private static final ExecutorService COVER_EXECUTOR = Executors.newFixedThreadPool(3);
        private static final Map<String, List<CoverTarget>> COVER_TARGETS = new ConcurrentHashMap<>();
        private static final Set<String> COVER_MISSING = ConcurrentHashMap.newKeySet();

        private static final class CoverTarget {
            final ImageView image;
            final TextView fallback;

            CoverTarget(ImageView image, TextView fallback) {
                this.image = image;
                this.fallback = fallback;
            }

            void display(String url, Bitmap bitmap) {
                image.post(() -> {
                    if (url.equals(image.getTag())) {
                        image.setImageBitmap(bitmap);
                        image.setVisibility(View.VISIBLE);
                        fallback.setVisibility(View.GONE);
                    }
                });
            }
        }

        private static String normalizedTid(String tid) {
            if (tid == null) return null;
            String id = tid.trim().toUpperCase(java.util.Locale.ROOT);
            if (!id.matches("01[0-9A-F]{14}")) return null;
            if (id.endsWith("800")) id = id.substring(0, 13) + "000";
            return id;
        }

        private static String coverUrl(String tid) {
            String id = normalizedTid(tid);
            return id == null ? null : "https://art.gametdb.com/switch/coverHQ/US/" + id + ".png";
        }

        private static String iconUrl(String tid) {
            String id = normalizedTid(tid);
            return id == null ? null : "https://api.nlib.cc/nx/" + id + "/icon/256";
        }

        private static void loadCover(String tid, ImageView image, TextView fallback) {
            final String url = coverUrl(tid);
            if (url == null || COVER_MISSING.contains(url)) return;
            image.setTag(url);
            Bitmap cached = COVER_CACHE.get(url);
            if (cached != null) {
                image.setImageBitmap(cached);
                image.setVisibility(View.VISIBLE);
                fallback.setVisibility(View.GONE);
                return;
            }
            final boolean shouldLoad;
            synchronized (COVER_TARGETS) {
                List<CoverTarget> targets = COVER_TARGETS.get(url);
                shouldLoad = targets == null;
                if (targets == null) {
                    targets = new ArrayList<>();
                    COVER_TARGETS.put(url, targets);
                }
                targets.add(new CoverTarget(image, fallback));
            }
            if (!shouldLoad) return;
            COVER_EXECUTOR.execute(() -> {
                Bitmap bitmap = null;
                File artFile = null;
                try {
                    File artDir = new File(image.getContext().getCacheDir(), "game_art");
                    if (!artDir.exists()) artDir.mkdirs();
                    String id = normalizedTid(tid);
                    if (id != null) {
                        artFile = new File(artDir, id + ".jpg");
                        if (artFile.isFile() && artFile.length() > 0) {
                            bitmap = BitmapFactory.decodeFile(artFile.getAbsolutePath());
                            if (bitmap == null) artFile.delete();
                        }
                    }
                } catch (Exception ignored) {}
                String[] candidates = new String[]{url, iconUrl(tid)};
                for (String candidate : candidates) {
                    if (bitmap != null) break;
                    if (candidate == null) continue;
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(candidate).openConnection();
                        connection.setConnectTimeout(4500);
                        connection.setReadTimeout(4500);
                        connection.setInstanceFollowRedirects(true);
                        connection.setRequestProperty("User-Agent", "SwitchForge/1.7");
                        connection.connect();
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            try (java.io.InputStream in = connection.getInputStream()) {
                                bitmap = BitmapFactory.decodeStream(in);
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                    if (bitmap != null) break;
                }
                if (bitmap != null && artFile != null && !artFile.isFile()) {
                    try (FileOutputStream out = new FileOutputStream(artFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
                    } catch (Exception ignored) {}
                }
                List<CoverTarget> targets;
                synchronized (COVER_TARGETS) {
                    targets = COVER_TARGETS.remove(url);
                }
                if (bitmap == null) {
                    COVER_MISSING.add(url);
                    return;
                }
                COVER_CACHE.put(url, bitmap);
                for (CoverTarget target : targets == null
                        ? java.util.Collections.<CoverTarget>emptyList() : targets) {
                    target.display(url, bitmap);
                }
            });
        }

        final Context ctx;
        final List<String[]> rows = new ArrayList<>();

        RowAdapter(Context ctx) { this.ctx = ctx; }

        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            LinearLayout card;
            View stripe;
            FrameLayout coverBox;
            TextView coverMark;
            ImageView coverImage;
            LinearLayout inner;
            TextView title;
            TextView sub;
            TextView badgeTag;
            TextView arrow;

            if (convertView instanceof LinearLayout
                    && "switchforge_game_row".equals(convertView.getTag())) {
                card = (LinearLayout) convertView;
                stripe = card.getChildAt(0);
                coverBox = (FrameLayout) card.getChildAt(1);
                coverMark = (TextView) coverBox.getChildAt(0);
                coverImage = (ImageView) coverBox.getChildAt(1);
                inner = (LinearLayout) card.getChildAt(2);
                title = (TextView) inner.getChildAt(0);
                LinearLayout meta = (LinearLayout) inner.getChildAt(1);
                sub = (TextView) meta.getChildAt(0);
                badgeTag = (TextView) meta.getChildAt(1);
                arrow = (TextView) card.getChildAt(3);
            } else {
                card = new LinearLayout(ctx);
                card.setTag("switchforge_game_row");
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(0, 0, dp(ctx, 12), 0);
                card.setMinimumHeight(dp(ctx, 76));
                card.setFocusable(true);
                card.setClickable(true);

                stripe = new View(ctx);
                card.addView(stripe, new LinearLayout.LayoutParams(dp(ctx, 3),
                        ViewGroup.LayoutParams.MATCH_PARENT));

                coverBox = new FrameLayout(ctx);
                LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(dp(ctx, 46), dp(ctx, 64));
                coverLp.setMargins(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));

                coverMark = new TextView(ctx);
                coverMark.setText("🎮");
                coverMark.setTextSize(21);
                coverMark.setGravity(Gravity.CENTER);
                coverMark.setTextColor(ACCENT);
                coverMark.setBackground(shape(ctx, ACCENT_BG, 6, 1, BORDER_GLOW));
                coverBox.addView(coverMark, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                coverImage = new ImageView(ctx);
                coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                coverImage.setVisibility(View.GONE);
                coverImage.setBackground(shape(ctx, ACCENT_BG, 6, 1, BORDER_GLOW));
                coverBox.addView(coverImage, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                card.addView(coverBox, coverLp);

                inner = new LinearLayout(ctx);
                inner.setOrientation(LinearLayout.VERTICAL);
                inner.setGravity(Gravity.CENTER_VERTICAL);
                title = text(ctx, "", 15, TEXT, true);
                title.setMaxLines(2);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                inner.addView(title);

                LinearLayout meta = new LinearLayout(ctx);
                meta.setOrientation(LinearLayout.HORIZONTAL);
                meta.setGravity(Gravity.CENTER_VERTICAL);
                meta.setPadding(0, dp(ctx, 5), 0, 0);

                sub = new TextView(ctx);
                sub.setTextSize(10);
                sub.setTextColor(ID_COLOR);
                sub.setTypeface(Typeface.MONOSPACE);
                sub.setMaxLines(1);
                sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
                meta.addView(sub, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                badgeTag = badge(ctx, "", GOOD_BG, GOOD, GOOD);
                badgeTag.setVisibility(View.GONE);
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                badgeLp.setMargins(dp(ctx, 8), 0, 0, 0);
                meta.addView(badgeTag, badgeLp);
                inner.addView(meta);

                card.addView(inner, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                arrow = text(ctx, "›", 24, ICON_MUTED, true);
                arrow.setPadding(dp(ctx, 8), 0, 0, 0);
                card.addView(arrow);
            }

            String[] r = rows.get(i);
            boolean isInstalled = "1".equals(r[2]);
            title.setText(r[0]);
            sub.setText(r[1]);
            coverMark.setContentDescription(Lang.s(R.string.main_game_image_desc));
            coverMark.setVisibility(View.VISIBLE);
            coverImage.setTag(null);
            coverImage.setImageDrawable(null);
            coverImage.setVisibility(View.GONE);
            coverImage.setBackground(shape(ctx, isInstalled ? GOOD_BG : ACCENT_BG, 6, 1,
                    isInstalled ? GOOD : BORDER_GLOW));
            coverMark.setBackground(shape(ctx, isInstalled ? GOOD_BG : ACCENT_BG, 6, 1,
                    isInstalled ? GOOD : BORDER_GLOW));
            coverMark.setTextColor(isInstalled ? GOOD : ACCENT);
            loadCover(r.length > 3 ? r[3] : null, coverImage, coverMark);

            if (isInstalled) {
                card.setBackground(shape(ctx, CARD_GAMER, 12, 1, 0xFF24443F));
                stripe.setBackground(lateralStripe(ctx, LED_GREEN, 8));
                badgeTag.setVisibility(View.VISIBLE);
                badgeTag.setText(Lang.s(R.string.main_badge_in_eden));
                badgeTag.setTextColor(GOOD);
                badgeTag.setBackground(shape(ctx, GOOD_BG, 6, 1, GOOD));
            } else {
                card.setBackground(shape(ctx, CARD_GAMER, 12, 1, BORDER));
                stripe.setBackground(lateralStripe(ctx, BORDER_GLOW, 8));
                badgeTag.setVisibility(View.GONE);
            }

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(dp(ctx, 2), dp(ctx, 4), dp(ctx, 2), dp(ctx, 4));
            card.setLayoutParams(cardLp);
            return card;
        }
    }
}
