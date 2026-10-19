package com.example.edencheats;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * SwitchForge - Tarefa Autônoma de Atualização
 * Sincroniza, busca e adiciona novos cheats a partir de servidores remotos protegidos,
 * realizando desduplicação de códigos hexadecimais em tempo real e incorporando à base local.
 */
public final class AutoUpdater {
    private AutoUpdater() {}

    private static final String PREFS_NAME = "SwitchForgeUpdater";
    private static final String KEY_LAST_TIME = "last_update_time";
    private static final String KEY_NEW_CHEATS = "last_new_cheats";
    private static final String KEY_DUPS_FILTERED = "last_dups_filtered";
    private static final String KEY_NEW_FILES = "last_new_files";

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "(?:^|/)\\[?([0-9A-Fa-f]{16})\\]?/(?:cheats/)?([0-9A-Fa-f]{16})\\.txt$");

    private static String decode(String b64) {
        return new String(android.util.Base64.decode(b64, android.util.Base64.NO_WRAP), StandardCharsets.UTF_8);
    }

    // Server 2 Mirror Seguro (Base Remota Principal)
    private static final String UPDATE_URL = decode(
            "aHR0cHM6Ly9naXRodWIuY29tL0hhbWxldER1RnJvbWFnZS9zd2l0Y2gtY2hlYXRzLWRiL3JlbGVhc2VzL2xhdGVzdC9kb3dubG9hZC9jb250ZW50c19jb21wbGV0ZS56aXA=");

    public interface UpdateListener {
        void onProgress(String message, int percent);
        void onSuccess(UpdateResult result);
        void onError(String error);
    }

    public static final class UpdateResult {
        public final int newCheats;
        public final int duplicatesFiltered;
        public final int newFiles;
        public final long durationMs;

        public UpdateResult(int newCheats, int duplicatesFiltered, int newFiles, long durationMs) {
            this.newCheats = newCheats;
            this.duplicatesFiltered = duplicatesFiltered;
            this.newFiles = newFiles;
            this.durationMs = durationMs;
        }

        public String summary() {
            return "⚡ Atualização concluída com sucesso!\n\n"
                    + "• Novos cheats adicionados: +" + newCheats + "\n"
                    + "• Duplicatas eliminadas: " + duplicatesFiltered + "\n"
                    + "• Novos arquivos de versão: +" + newFiles + "\n"
                    + "• Tempo total: " + (durationMs / 1000) + "s";
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static String getLastUpdateInfo(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long time = sp.getLong(KEY_LAST_TIME, 0);
        if (time == 0) {
            return "Nenhuma atualização remota executada ainda.\nBase de dados padrão ativa (100% offline).";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault());
        int cheats = sp.getInt(KEY_NEW_CHEATS, 0);
        int dups = sp.getInt(KEY_DUPS_FILTERED, 0);
        int files = sp.getInt(KEY_NEW_FILES, 0);
        return "Última atualização: " + sdf.format(new Date(time)) + "\n"
                + "• +" + cheats + " cheats únicos incorporados\n"
                + "• " + dups + " duplicatas filtradas (" + files + " novos arquivos)";
    }

    public static boolean restoreFactoryDatabase(Context c) {
        if (RUNNING.get()) return false;
        File f = new File(c.getFilesDir(), Sources.ASSET_CACHE);
        if (f.exists()) {
            f.delete();
        }
        try {
            Sources.assetZip(c);
            SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            sp.edit().clear().apply();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void runUpdate(final Context context, final UpdateListener listener) {
        if (!RUNNING.compareAndSet(false, true)) {
            listener.onError("A tarefa de atualização já está em execução.");
            return;
        }

        Util.POOL.execute(() -> {
            long startTime = System.currentTimeMillis();
            File tempRemote = new File(context.getCacheDir(), "switchforge_update_" + System.currentTimeMillis() + ".zip");
            File tempMerged = new File(context.getFilesDir(), "switchforge_merged_" + System.currentTimeMillis() + ".tmp");
            File localDb;

            try {
                // 1. Garantir existência da base local
                listener.onProgress("1/4 Conectando ao servidor seguro...", 10);
                localDb = Sources.assetZip(context);

                // 2. Download do pacote de atualização com acompanhamento
                listener.onProgress("2/4 Baixando novo pacote de cheats...", 20);
                Util.download(UPDATE_URL, tempRemote, msg -> {
                    // Repassa mensagens de progresso
                    listener.onProgress("2/4 " + msg, 40);
                });

                if (!tempRemote.exists() || tempRemote.length() == 0) {
                    throw new IOException("Falha no download do pacote de atualização.");
                }

                listener.onProgress("3/4 Analisando índices e desduplicando cheats...", 60);

                int newCheatsCount = 0;
                int duplicatesCount = 0;
                int newFilesCount = 0;

                try (ZipFile localZip = new ZipFile(localDb);
                     ZipFile remoteZip = new ZipFile(tempRemote);
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempMerged)))) {

                    // Mapeia entradas do zip remoto
                    Map<String, ZipEntry> remoteMap = new HashMap<>();
                    Enumeration<? extends ZipEntry> remEntries = remoteZip.entries();
                    while (remEntries.hasMoreElements()) {
                        ZipEntry re = remEntries.nextElement();
                        if (re.isDirectory()) continue;
                        Matcher m = ENTRY_PATTERN.matcher(re.getName());
                        if (m.find()) {
                            String tid = m.group(1).toUpperCase(Locale.US);
                            String bid = m.group(2).toUpperCase(Locale.US);
                            remoteMap.put(tid + "_" + bid, re);
                        }
                    }

                    Set<String> processedKeys = new HashSet<>();

                    // Processa entradas do localZip
                    Enumeration<? extends ZipEntry> locEntries = localZip.entries();
                    while (locEntries.hasMoreElements()) {
                        ZipEntry le = locEntries.nextElement();
                        if (le.isDirectory()) continue;

                        Matcher m = ENTRY_PATTERN.matcher(le.getName());
                        if (!m.find()) {
                            // Arquivos como names.tsv ou outros mantêm-se intactos
                            copyZipEntry(localZip, le, zos, le.getName());
                            continue;
                        }

                        String tid = m.group(1).toUpperCase(Locale.US);
                        String bid = m.group(2).toUpperCase(Locale.US);
                        String key = tid + "_" + bid;
                        processedKeys.add(key);

                        ZipEntry re = remoteMap.get(key);
                        if (re != null) {
                            // Arquivo existe em ambos: realiza fusão e desduplicação
                            String localText = readZipEntryText(localZip, le);
                            String remoteText = readZipEntryText(remoteZip, re);

                            List<CheatParser.Block> localBlocks = CheatParser.parse(localText);
                            List<CheatParser.Block> remoteBlocks = CheatParser.parse(remoteText);

                            Set<String> sigs = new HashSet<>();
                            for (CheatParser.Block b : localBlocks) {
                                sigs.add(codeSignature(b));
                            }

                            int addedThis = 0;
                            for (CheatParser.Block rb : remoteBlocks) {
                                String s = codeSignature(rb);
                                if (sigs.contains(s)) {
                                    duplicatesCount++;
                                } else {
                                    sigs.add(s);
                                    localBlocks.add(rb);
                                    newCheatsCount++;
                                    addedThis++;
                                }
                            }

                            byte[] outData = (addedThis > 0)
                                    ? CheatParser.build(localBlocks).getBytes(StandardCharsets.UTF_8)
                                    : localText.getBytes(StandardCharsets.UTF_8);

                            writeZipEntry(zos, le.getName(), outData);
                        } else {
                            // Existe só no local: preserva
                            copyZipEntry(localZip, le, zos, le.getName());
                        }
                    }

                    // Processa novas entradas do remoteZip que não existiam no local
                    listener.onProgress("4/4 Incorporando novos arquivos à base local...", 85);
                    for (Map.Entry<String, ZipEntry> entry : remoteMap.entrySet()) {
                        String key = entry.getKey();
                        if (processedKeys.contains(key)) continue;

                        ZipEntry re = entry.getValue();
                        String remoteText = readZipEntryText(remoteZip, re);
                        List<CheatParser.Block> remoteBlocks = CheatParser.parse(remoteText);

                        List<CheatParser.Block> cleanBlocks = new ArrayList<>();
                        Set<String> sigs = new HashSet<>();
                        for (CheatParser.Block rb : remoteBlocks) {
                            String s = codeSignature(rb);
                            if (!sigs.contains(s)) {
                                sigs.add(s);
                                cleanBlocks.add(rb);
                            } else {
                                duplicatesCount++;
                            }
                        }

                        if (!cleanBlocks.isEmpty()) {
                            newFilesCount++;
                            newCheatsCount += cleanBlocks.size();
                            String[] parts = key.split("_");
                            String destPath = "[" + parts[0] + "]/cheats/" + parts[1] + ".txt";
                            byte[] outData = CheatParser.build(cleanBlocks).getBytes(StandardCharsets.UTF_8);
                            writeZipEntry(zos, destPath, outData);
                        }
                    }
                }

                // Substituição atômica da base local
                File backup = new File(context.getFilesDir(), "offline_db_backup.tmp");
                if (backup.exists()) backup.delete();
                if (localDb.exists()) localDb.renameTo(backup);

                if (!tempMerged.renameTo(localDb)) {
                    // Se falhar o renomeio, restaura backup
                    if (backup.exists()) backup.renameTo(localDb);
                    throw new IOException("Não foi possível substituir a base local pelo arquivo atualizado.");
                } else {
                    if (backup.exists()) backup.delete();
                }

                // Salva registro de atualização
                long elapsed = System.currentTimeMillis() - startTime;
                SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                sp.edit()
                        .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                        .putInt(KEY_NEW_CHEATS, newCheatsCount)
                        .putInt(KEY_DUPS_FILTERED, duplicatesCount)
                        .putInt(KEY_NEW_FILES, newFilesCount)
                        .apply();

                UpdateResult result = new UpdateResult(newCheatsCount, duplicatesCount, newFilesCount, elapsed);
                listener.onProgress("Atualização concluída com sucesso!", 100);
                listener.onSuccess(result);

            } catch (final Exception e) {
                if (tempMerged.exists()) tempMerged.delete();
                listener.onError("Erro na atualização: " + e.getMessage());
            } finally {
                if (tempRemote.exists()) tempRemote.delete();
                RUNNING.set(false);
            }
        });
    }

    private static String codeSignature(CheatParser.Block b) {
        if (b.lines == null || b.lines.isEmpty()) {
            return "EMPTY_" + b.name().toLowerCase(Locale.US);
        }
        StringBuilder sb = new StringBuilder();
        for (String l : b.lines) {
            String clean = l.replaceAll("\\s+", "").toUpperCase(Locale.US);
            if (!clean.isEmpty()) {
                sb.append(clean).append(';');
            }
        }
        return sb.toString();
    }

    private static String readZipEntryText(ZipFile zf, ZipEntry ze) throws IOException {
        try (InputStream in = zf.getInputStream(ze)) {
            return new String(Util.readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static void copyZipEntry(ZipFile zf, ZipEntry ze, ZipOutputStream zos, String targetName) throws IOException {
        ZipEntry outZe = new ZipEntry(targetName);
        zos.putNextEntry(outZe);
        try (InputStream in = zf.getInputStream(ze)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) != -1) {
                zos.write(buf, 0, n);
            }
        }
        zos.closeEntry();
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        zos.putNextEntry(ze);
        zos.write(data);
        zos.closeEntry();
    }
}
