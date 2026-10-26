package com.example.edencheats;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
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

    /** Respeita a configuração "Escala de duração da animação" do Android (0 = animações desligadas). */
    static boolean animationsEnabled(Context c) {
        try {
            return android.provider.Settings.Global.getFloat(c.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception e) {
            return true;
        }
    }

    /** Efeito de toque: o controle "afunda" ao pressionar, volta com mola e vibra de leve. */
    static void addPressEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            int a = event.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                if (v.isEnabled()) {
                    v.animate().setStartDelay(0).scaleX(0.96f).scaleY(0.96f).setDuration(90)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(240)
                        .setInterpolator(new OvershootInterpolator(2.4f)).start();
            }
            return false;
        });
    }

    /** Pulsa suavemente um indicador (LED) enquanto a tela estiver aberta. */
    static void pulse(View v) {
        final ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0.3f);
        a.setDuration(1000);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View x) {
                if (animationsEnabled(x.getContext())) a.start();
            }
            @Override public void onViewDetachedFromWindow(View x) {
                a.cancel();
                x.setAlpha(1f);
            }
        });
    }

    /** Entrada em cascata: cada bloco da tela aparece subindo suavemente, um após o outro. */
    static void staggerIn(ViewGroup parent) {
        if (parent == null || !animationsEnabled(parent.getContext())) return;
        final int dy = dp(parent.getContext(), 18);
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View v = parent.getChildAt(i);
            if (v instanceof ListView) continue; // a lista tem a própria animação
            v.setAlpha(0f);
            v.setTranslationY(dy);
            v.animate().setStartDelay(Math.min(i, 8) * 50L).alpha(1f).translationY(0f)
                    .setDuration(340).setInterpolator(new DecelerateInterpolator(1.6f)).start();
            // Rede de segurança: nunca deixa um bloco invisível se a animação for interrompida.
            v.postDelayed(() -> {
                if (v.getAlpha() < 1f) v.setAlpha(1f);
                if (v.getTranslationY() != 0f) v.setTranslationY(0f);
            }, 1500);
        }
    }

    /** Lista com cards: ripple por cima da linha e entrada animada dos itens. */
    static void styleList(ListView list) {
        Context c = list.getContext();
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(ripple(c, Color.TRANSPARENT, 12, 0, 0, 0x2600D4FF));
        list.setDrawSelectorOnTop(true);
        list.setClipToPadding(false);
        list.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        if (!animationsEnabled(c)) return;
        AnimationSet set = new AnimationSet(true);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(300);
        TranslateAnimation slide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.ABSOLUTE, dp(c, 26), Animation.ABSOLUTE, 0f);
        slide.setDuration(340);
        set.addAnimation(fade);
        set.addAnimation(slide);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        LayoutAnimationController lac = new LayoutAnimationController(set, 0.08f);
        list.setLayoutAnimation(lac);
    }

    /** Reexecuta a animação de entrada dos itens (ex.: ao terminar de carregar ou trocar o filtro). */
    static void replayListAnimation(ListView list) {
        if (list != null && animationsEnabled(list.getContext())) list.scheduleLayoutAnimation();
    }

    /** Faixa fina de progresso indeterminado (luz ciano correndo), para operações em andamento. */
    static final class ProgressStrip extends View {
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private ValueAnimator anim;
        private float t = 0f;
        private boolean active = false;

        ProgressStrip(Context c) {
            super(c);
            bar.setColor(ACCENT);
            track.setColor(BORDER);
            setVisibility(View.INVISIBLE);
        }

        void setActive(boolean on) {
            if (on == active) return;
            active = on;
            if (on) {
                setVisibility(View.VISIBLE);
                start();
            } else {
                stop();
                setVisibility(View.INVISIBLE);
            }
        }

        private void start() {
            if (anim != null) return;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1100);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addUpdateListener(a -> {
                t = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        private void stop() {
            if (anim != null) {
                anim.cancel();
                anim = null;
            }
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (active) start();
        }

        @Override protected void onDetachedFromWindow() {
            stop();
            super.onDetachedFromWindow();
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float r = h / 2f;
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, r, r, track);
            float bw = w * 0.35f;
            float x = -bw + t * (w + bw);
            canvas.save();
            canvas.clipRect(0, 0, w, h);
            rect.set(x, 0, x + bw, h);
            canvas.drawRoundRect(rect, r, r, bar);
            canvas.restore();
        }
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
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        if (selected) {
            t.setTextColor(0xFF0A0D14);
            t.setBackground(shape(c, ACCENT, 14, 0, 0));
        } else {
            t.setTextColor(ICON_MUTED);
            t.setBackground(shape(c, 0x33121620, 14, 1, BORDER));
        }
        int px = dp(c, 13);
        int py = dp(c, 7);
        t.setPadding(px, py, px, py);
        t.setMinHeight(dp(c, 36));
        t.setGravity(Gravity.CENTER);
        if (l != null) {
            t.setOnClickListener(l);
            t.setClickable(true);
            addPressEffect(t);
        }
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
        int px = dp(c, 12);
        int py = dp(c, 7);
        t.setPadding(px, py, px, py);
        t.setMinHeight(dp(c, 36));
        t.setGravity(Gravity.CENTER);
        if (l != null) {
            t.setOnClickListener(l);
            t.setClickable(true);
            addPressEffect(t);
        }
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
        // Botão desativado fica visivelmente apagado (o código chama setEnabled(false) durante operações).
        Button b = new Button(c) {
            @Override public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                setAlpha(enabled ? 1f : 0.45f);
            }
        };
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        if (primary) {
            b.setTextColor(0xFF0A0D14);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setBackground(premiumButtonBackground(c));
            b.setElevation(dp(c, 5));
            if (Build.VERSION.SDK_INT >= 28) {
                b.setOutlineSpotShadowColor(0xFF00D4FF);   // brilho ciano sob o botão principal
                b.setOutlineAmbientShadowColor(0x6600D4FF);
            }
        } else {
            b.setTextColor(TEXT);
            b.setBackground(ripple(c, CARD, 12, 1, 0xFF2A3550, 0x3300D4FF));
            b.setElevation(dp(c, 2));
        }
        int py = dp(c, 10);
        int px = dp(c, 14);
        b.setPadding(px, py, px, py);
        b.setMinHeight(dp(c, 48));
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
            pulse(led);
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
        card.setMinimumHeight(dp(c, 52));
        card.setElevation(dp(c, 2));
        addPressEffect(card);
        return card;
    }

    /** Card de apoio ao projeto (doação): destaque dourado em largura total. */
    static LinearLayout supportCard(Context c, String emoji, String label, String hint, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(ripple(c, WARN_BG, 12, 1, 0xFF6B520F, 0x33FFB700));
        int px = dp(c, 14);
        int py = dp(c, 10);
        card.setPadding(px, py, px, py);

        TextView ico = text(c, emoji, 22, WARN, false);
        card.addView(ico);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(c, label, 14, TEXT, true));
        if (hint != null && !hint.isEmpty()) col.addView(text(c, hint, 11, 0xFFC9A44A, false));
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpCol.setMargins(dp(c, 12), 0, 0, 0);
        card.addView(col, lpCol);

        card.addView(text(c, "›", 24, WARN, true));
        if (l != null) card.setOnClickListener(l);
        card.setMinimumHeight(dp(c, 56));
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

        int px = dp(c, 9);
        int py = dp(c, 6);
        pill.setPadding(px, py, px, py);

        TextView ico = new TextView(c);
        ico.setText(iconSymbol);
        ico.setTextSize(11);
        ico.setTextColor(iconColor);
        ico.setTypeface(Typeface.DEFAULT_BOLD);
        pill.addView(ico);

        TextView txt = new TextView(c);
        txt.setText(label);
        txt.setTextSize(11);
        txt.setTextColor(0xFFFFFFFF); // Texto branco puro conforme especificação
        txt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(c, 4), 0, 0, 0);
        pill.addView(txt, lp);

        if (l != null) pill.setOnClickListener(l);
        pill.setMinimumHeight(dp(c, 40));
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
        final Context ctx = c;
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setHighlightColor(0x5500D4FF);
        e.setBackground(shape(c, 0xFF0E121A, 10, 1, BORDER));
        int p = dp(c, 12);
        e.setPadding(p, p, p, p);
        e.setMinHeight(dp(c, 48));
        // Destaque ciano na borda enquanto o campo está em foco.
        e.setOnFocusChangeListener((v, has) -> v.setBackground(has
                ? shape(ctx, 0xFF0F1620, 10, 2, ACCENT)
                : shape(ctx, 0xFF0E121A, 10, 1, BORDER)));
        return e;
    }

    /** Caixa de busca com ícone, botão de limpar (✕) e destaque ao focar. O EditText é reaproveitado. */
    static LinearLayout searchBox(final Context c, final EditText e) {
        final LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(shape(c, CARD, 12, 1, BORDER));
        box.setPadding(dp(c, 12), 0, dp(c, 4), 0);

        TextView ico = text(c, "\uD83D\uDD0D", 15, ICON_MUTED, false);
        LinearLayout.LayoutParams lpIco = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpIco.setMargins(0, 0, dp(c, 8), 0);
        box.addView(ico, lpIco);

        e.setBackground(null);
        e.setPadding(0, dp(c, 13), 0, dp(c, 13));
        e.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        e.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                v.clearFocus();
                return true;
            }
            return false;
        });
        box.addView(e, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView clear = text(c, "✕", 16, MUTED, true);
        clear.setGravity(Gravity.CENTER);
        clear.setVisibility(View.GONE);
        clear.setContentDescription(Lang.s(R.string.main_search_clear));
        clear.setBackground(ripple(c, Color.TRANSPARENT, 20, 0, 0, 0x33FFFFFF));
        clear.setClickable(true);
        clear.setOnClickListener(v -> e.setText(""));
        box.addView(clear, new LinearLayout.LayoutParams(dp(c, 42), dp(c, 42)));

        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable ed) {
                clear.setVisibility(ed.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });
        e.setOnFocusChangeListener((v, has) -> box.setBackground(has
                ? shape(c, 0xFF0F1620, 12, 2, ACCENT)
                : shape(c, CARD, 12, 1, BORDER)));
        return box;
    }

    /** Cabeçalho das telas internas: botão voltar (←) grande + título + subtítulo. */
    static LinearLayout headerCard(final Activity a, String title, String subtitle, int subColor) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(shape(a, CARD, 14, 1, BORDER));
        int p = dp(a, 10);
        row.setPadding(p, p, dp(a, 14), p);

        TextView back = text(a, "←", 22, ACCENT, true);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(a, ACCENT_BG, 12, 1, 0xFF14455C, 0x3300D4FF));
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription(Lang.s(R.string.main_back));
        back.setOnClickListener(v -> a.finish());
        addPressEffect(back);
        LinearLayout.LayoutParams lpBack = new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44));
        lpBack.setMargins(0, 0, dp(a, 12), 0);
        row.addView(back, lpBack);

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(a, title, 19, TEXT, true));
        if (subtitle != null && !subtitle.isEmpty()) col.addView(text(a, subtitle, 12, subColor, false));
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    static EditText monoEdit(Context c, String hint) {
        EditText e = edit(c, hint);
        e.setTypeface(Typeface.MONOSPACE);
        return e;
    }

    private static final String SNACK_TAG = "switchforge_snack";

    private static Activity activityOf(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    /** Aviso rápido: barra animada na parte de baixo (cor pelo tipo), toque para fechar. */
    static void toast(Context c, String s) {
        final Activity a = activityOf(c);
        if (a == null || a.isFinishing()) {
            Toast.makeText(c.getApplicationContext(), s, Toast.LENGTH_LONG).show();
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            a.runOnUiThread(() -> toast(a, s));
            return;
        }
        try {
            showSnack(a, s);
        } catch (Exception e) {
            Toast.makeText(a.getApplicationContext(), s, Toast.LENGTH_LONG).show();
        }
    }

    private static void showSnack(final Activity a, String msg) {
        final ViewGroup host = a.findViewById(android.R.id.content);
        if (host == null) throw new IllegalStateException("sem conteúdo");
        View old = host.findViewWithTag(SNACK_TAG);
        if (old != null) host.removeView(old);

        String t = msg == null ? "" : msg.trim();
        String low = t.toLowerCase(java.util.Locale.ROOT);
        int accent = ACCENT;
        if (t.startsWith("✔") || t.startsWith("✅")) accent = GOOD;
        else if (t.startsWith("⚠")) accent = WARN;
        else if (t.startsWith("❌") || t.startsWith("✖") || low.startsWith("erro") || low.startsWith("error")) accent = DANGER;

        final LinearLayout bar = new LinearLayout(a);
        bar.setTag(SNACK_TAG);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(shape(a, 0xFF1A2234, 12, 1, accent));
        bar.setElevation(dp(a, 12));
        bar.setClickable(true);
        bar.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        View stripe = new View(a);
        stripe.setBackground(lateralStripe(a, accent, 12));
        bar.addView(stripe, new LinearLayout.LayoutParams(dp(a, 5), ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tv = text(a, t, 13, TEXT, false);
        tv.setMaxLines(6);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        bar.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(dp(a, 14), 0, dp(a, 14), dp(a, 22));
        host.addView(bar, lp);

        bar.setAlpha(0f);
        bar.setTranslationY(dp(a, 24));
        bar.animate().alpha(1f).translationY(0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();

        final Runnable dismiss = () -> {
            if (bar.getParent() == null) return;
            bar.animate().alpha(0f).translationY(dp(a, 14)).setDuration(200)
                    .withEndAction(() -> host.removeView(bar)).start();
        };
        bar.setOnClickListener(v -> dismiss.run());
        bar.postDelayed(dismiss, Math.min(6500, 2600 + t.length() * 35L));
    }

    /** Abre outro app (ex.: o Eden) pelo nome do pacote. */
    static boolean openApp(Context c, String pkg) {
        try {
            android.content.Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) {
                toast(c, Lang.s(R.string.ux_open_failed));
                return false;
            }
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            toast(c, Lang.s(R.string.ux_open_failed));
            return false;
        }
    }

    /** Sucesso ao instalar cheats: fica na tela (o aviso rápido sumia) e oferece abrir o Eden. */
    static void installedDialog(Activity a, String message) {
        if (a.isFinishing()) return;
        final EdenDetector.Found eden = EdenDetector.best(a);
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(a)
                .setTitle(Lang.s(R.string.ux_installed_title))
                .setMessage(message)
                .setNegativeButton(Lang.s(R.string.ux_close), null);
        if (eden != null) {
            b.setPositiveButton(Lang.s(R.string.ux_open_eden), (d, w) -> openApp(a, eden.pkg));
        }
        b.show();
    }

    /** Guia rápido "Como usar" (4 passos). */
    static void helpDialog(Activity a) {
        if (a.isFinishing()) return;
        final EdenDetector.Found eden = EdenDetector.best(a);
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(a)
                .setTitle(Lang.s(R.string.ux_help_title))
                .setMessage(Lang.s(R.string.ux_help_body))
                .setPositiveButton(Lang.s(R.string.ux_help_close), null);
        if (eden != null) {
            b.setNeutralButton(Lang.s(R.string.ux_open_eden), (d, w) -> openApp(a, eden.pkg));
        }
        b.show();
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
                // IMPORTANTE: a linha NÃO pode ser clicável/focável. Numa ListView, uma linha clicável
                // "engole" o toque e o setOnItemClickListener da lista nunca dispara (o jogo não abria).
                // O efeito de toque vem do selector da própria lista (Ui.styleList).
                card.setFocusable(false);
                card.setClickable(false);

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
                sub.setTextSize(11);
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
