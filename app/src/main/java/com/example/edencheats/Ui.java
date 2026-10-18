package com.example.edencheats;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
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

/** Helpers de interface (a UI inteira é montada em código). */
final class Ui {
    private Ui() {}

    static final int BG = 0xFF14141C;
    static final int CARD = 0xFF1F1F2B;
    static final int ACCENT = 0xFF9C6BFF;
    static final int TEXT = 0xFFF2F2F8;
    static final int MUTED = 0xFFA4A4BC;
    static final int GOOD = 0xFF6BE3A0;

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
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
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setColor(ACCENT);
        g.setCornerRadius(dp(c, 10));
        b.setBackground(g);
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
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 10));
        e.setBackground(g);
        int p = dp(c, 12);
        e.setPadding(p, p, p, p);
        return e;
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }

    /** Lista de duas linhas: {título, subtítulo, "1" para destacar}. */
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
            LinearLayout row;
            TextView t1;
            TextView t2;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
                t1 = (TextView) row.getChildAt(0);
                t2 = (TextView) row.getChildAt(1);
            } else {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.VERTICAL);
                int p = dp(ctx, 12);
                row.setPadding(p, p, p, p);
                t1 = text(ctx, "", 16, TEXT, true);
                t2 = text(ctx, "", 12, MUTED, false);
                row.addView(t1);
                row.addView(t2);
            }
            String[] r = rows.get(i);
            t1.setText(r[0]);
            t1.setTextColor("1".equals(r[2]) ? GOOD : TEXT);
            t2.setText(r[1]);
            return row;
        }
    }
}
