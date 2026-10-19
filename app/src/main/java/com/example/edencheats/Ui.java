package com.example.edencheats;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Helpers de interface com design minimalista, tecnológico e moderno (Cyber/Neo-Tech). */
final class Ui {
    private Ui() {}

    // Paleta Minimalista & Tecnológica (Cyber Dark / OLED)
    static final int BG = 0xFF0A0D14;            // Fundo escuro profundo obsidian
    static final int CARD = 0xFF121722;          // Superfície de cards tech
    static final int CARD_HOVER = 0xFF1A2234;    // Card selecionado / hover
    static final int BORDER = 0xFF1E2838;        // Borda sutil de componentes
    static final int BORDER_GLOW = 0xFF00D4FF;   // Borda com brilho ciano ativo
    static final int ACCENT = 0xFF00D4FF;        // Ciano elétrico neon principal
    static final int ACCENT_BG = 0xFF0A2233;     // Fundo translúcido para badges ciano
    static final int TEXT = 0xFFF0F6FC;          // Branco puro de alto contraste
    static final int MUTED = 0xFF8B949E;         // Cinza tecnológico legível
    static final int GOOD = 0xFF00F5A0;          // Verde menta neon
    static final int GOOD_BG = 0xFF062A1F;       // Fundo de badge de sucesso
    static final int DANGER = 0xFFFF4B6E;        // Vermelho neon para apagar/avisos
    static final int DANGER_BG = 0xFF2D0F18;

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
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        if (selected) {
            t.setTextColor(0xFF0A0D14);
            t.setBackground(shape(c, ACCENT, 16, 0, 0));
        } else {
            t.setTextColor(MUTED);
            t.setBackground(shape(c, CARD, 16, 1, BORDER));
        }
        int px = dp(c, 12);
        int py = dp(c, 6);
        t.setPadding(px, py, px, py);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    static TextView chipNeon(Context c, String s, boolean selected, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(12);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        if (selected) {
            t.setTextColor(0xFF0A0D14);
            t.setBackground(shape(c, GOOD, 16, 0, 0));
        } else {
            t.setTextColor(GOOD);
            t.setBackground(shape(c, GOOD_BG, 16, 1, GOOD));
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
            b.setTextColor(0xFF0A0D14); // Texto escuro sobre ciano neon para alto contraste
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setBackground(shape(c, ACCENT, 10, 0, 0));
        } else {
            b.setTextColor(TEXT);
            b.setBackground(shape(c, CARD, 10, 1, BORDER));
        }
        int py = dp(c, 10);
        int px = dp(c, 14);
        b.setPadding(px, py, px, py);
        b.setOnClickListener(l);
        return b;
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
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setBackground(shape(c, CARD, 10, 1, BORDER));
        int p = dp(c, 12);
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

    /** Lista de cards com visual tecnológico: {título, subtítulo, "1" para destacar}. */
    static final class RowAdapter extends BaseAdapter {
        private final Context ctx;
        final List<String[]> rows = new ArrayList<>();

        RowAdapter(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int i) {
            return rows.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            LinearLayout card;
            TextView title;
            TextView sub;
            TextView badgeTag;

            if (convertView instanceof LinearLayout) {
                card = (LinearLayout) convertView;
                LinearLayout inner = (LinearLayout) card.getChildAt(0);
                title = (TextView) inner.getChildAt(0);
                LinearLayout meta = (LinearLayout) inner.getChildAt(1);
                sub = (TextView) meta.getChildAt(0);
                badgeTag = (TextView) meta.getChildAt(1);
            } else {
                card = new LinearLayout(ctx);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                int p = dp(ctx, 12);
                card.setPadding(p, p, p, p);

                LinearLayout inner = new LinearLayout(ctx);
                inner.setOrientation(LinearLayout.VERTICAL);

                title = text(ctx, "", 15, TEXT, true);
                inner.addView(title);

                LinearLayout meta = new LinearLayout(ctx);
                meta.setOrientation(LinearLayout.HORIZONTAL);
                meta.setGravity(Gravity.CENTER_VERTICAL);
                int mTop = dp(ctx, 4);
                meta.setPadding(0, mTop, 0, 0);

                sub = text(ctx, "", 12, MUTED, false);
                meta.addView(sub);

                badgeTag = badge(ctx, "", ACCENT_BG, ACCENT, BORDER);
                badgeTag.setVisibility(View.GONE);
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                badgeLp.setMargins(dp(ctx, 8), 0, 0, 0);
                meta.addView(badgeTag, badgeLp);

                inner.addView(meta);
                card.addView(inner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView arrow = text(ctx, "›", 18, MUTED, true);
                arrow.setPadding(dp(ctx, 6), 0, dp(ctx, 4), 0);
                card.addView(arrow);
            }

            String[] r = rows.get(i);
            boolean isMatch = "1".equals(r[2]);

            title.setText(r[0]);
            title.setTextColor(isMatch ? GOOD : TEXT);

            sub.setText(r[1]);

            // Se for match de Build ID, destaca com card neon verde
            if (isMatch) {
                card.setBackground(shape(ctx, CARD_HOVER, 12, 1, GOOD));
                badgeTag.setVisibility(View.VISIBLE);
                badgeTag.setText("MATCH");
                badgeTag.setTextColor(GOOD);
                badgeTag.setBackground(shape(ctx, GOOD_BG, 6, 1, GOOD));
            } else {
                card.setBackground(shape(ctx, CARD, 12, 1, BORDER));
                badgeTag.setVisibility(View.GONE);
            }

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int mH = dp(ctx, 2);
            int mV = dp(ctx, 4);
            cardLp.setMargins(mH, mV, mH, mV);
            card.setLayoutParams(cardLp);

            return card;
        }
    }
}
