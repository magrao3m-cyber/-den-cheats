package com.example.edencheats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

final class Util {
    private Util() {}

    static final ExecutorService POOL = Executors.newCachedThreadPool();
    static final Pattern HEX16 = Pattern.compile("^[0-9A-Fa-f]{16}$");

    /** Link de doação do PayPal (botão "Pague-me um café"). */
    static final String DONATE_URL = "https://www.paypal.com/donate/?hosted_button_id=C2UB4FRBJ2QQN";

    /** Abre a página de doação no navegador externo (o PayPal não funciona bem em WebView). */
    static void openDonation(android.content.Context c) {
        try {
            android.content.Intent i = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(DONATE_URL));
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            android.widget.Toast.makeText(c,
                    Lang.s(R.string.main_donate_open_failed), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    interface Progress {
        void onProgress(String msg);
    }

    static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "EdenCheatsManager/1.0 (Android)");
        return c;
    }

    static byte[] httpBytes(String url, String accept) throws IOException {
        HttpURLConnection c = open(url);
        if (accept != null) c.setRequestProperty("Accept", accept);
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                String extra = code == 403 ? " " + Lang.s(R.string.main_http_github_limit) : "";
                throw new IOException(Lang.s(R.string.main_http_error, code, extra, url));
            }
            try (InputStream in = c.getInputStream()) {
                return readAll(in);
            }
        } finally {
            c.disconnect();
        }
    }

    static void download(String url, File dest, Progress p) throws IOException {
        File tmp = new File(dest.getPath() + ".part");
        HttpURLConnection c = open(url);
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new IOException(Lang.s(R.string.main_http_error_plain, code, url));
            }
            long total = c.getContentLengthLong();
            long done = 0;
            long last = 0;
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    done += n;
                    long now = System.currentTimeMillis();
                    if (p != null && now - last > 300) {
                        last = now;
                        p.onProgress(Lang.s(R.string.main_downloading, mb(done)) + (total > 0 ? " / " + mb(total) : ""));
                    }
                }
            }
        } finally {
            c.disconnect();
        }
        if (dest.exists() && !dest.delete()) {
            throw new IOException(Lang.s(R.string.main_replace_failed, dest.getName()));
        }
        if (!tmp.renameTo(dest)) {
            throw new IOException(Lang.s(R.string.main_save_failed, dest.getName()));
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) != -1) {
            o.write(buf, 0, n);
        }
        return o.toByteArray();
    }

    static byte[] readFile(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return readAll(in);
        }
    }

    static void writeFile(File f, byte[] data) throws IOException {
        try (OutputStream o = new FileOutputStream(f)) {
            o.write(data);
        }
    }

    static String mb(long bytes) {
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    static String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; i++) {
            if (sb.length() > 0) sb.append('/');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    static String encodePath(String path) {
        String[] seg = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seg.length; i++) {
            if (i > 0) sb.append('/');
            try {
                sb.append(URLEncoder.encode(seg[i], "UTF-8").replace("+", "%20"));
            } catch (java.io.UnsupportedEncodingException e) {
                sb.append(seg[i]);
            }
        }
        return sb.toString();
    }

    /** Remove caracteres inválidos para nome de pasta. */
    static String sanitize(String s) {
        String r = s == null ? "" : s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return r.isEmpty() ? "Cheats" : r;
    }

    static boolean isHex16(String s) {
        return s != null && HEX16.matcher(s).matches();
    }
}
