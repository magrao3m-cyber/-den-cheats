package com.example.edencheats;

import java.util.ArrayList;
import java.util.List;

/** Lê/gera arquivos de cheat no formato Atmosphère/Eden ([Nome] seguido de linhas hexadecimais). */
final class CheatParser {
    private CheatParser() {}

    static final class Block {
        String header = "";
        boolean master;
        final List<String> lines = new ArrayList<>();

        String name() {
            String h = header.trim();
            if (h.length() >= 2 && ((h.startsWith("[") && h.endsWith("]")) || (h.startsWith("{") && h.endsWith("}")))) {
                h = h.substring(1, h.length() - 1);
            }
            return h.trim();
        }
    }

    static List<Block> parse(String text) {
        List<Block> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n');
        Block cur = null;
        for (String raw : t.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            boolean isCheat = line.startsWith("[") && line.contains("]");
            boolean isMaster = line.startsWith("{") && line.contains("}");

            if (isCheat || isMaster) {
                cur = new Block();
                cur.master = isMaster;
                int close = isCheat ? line.indexOf(']') : line.indexOf('}');
                cur.header = line.substring(0, close + 1).trim();
                out.add(cur);

                // Suporte quando os códigos estão na mesma linha após o cabeçalho
                // Ex: [Nome do Cheat Aqui] 58000000 00C88A70 78001000 00000090
                String remainder = line.substring(close + 1).trim();
                if (!remainder.isEmpty()) {
                    addCodeLines(cur, remainder);
                }
            } else if (cur != null) {
                addCodeLines(cur, line);
            }
        }
        return out;
    }

    private static void addCodeLines(Block block, String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) return;

        // Se houver múltiplos pares hexadecimais em uma única linha, separa em pares de 8 hex
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length > 2 && tokens.length % 2 == 0) {
            for (int i = 0; i < tokens.length; i += 2) {
                block.lines.add((tokens[i] + " " + tokens[i + 1]).toUpperCase());
            }
        } else {
            block.lines.add(trimmed.toUpperCase());
        }
    }

    /**
     * O Eden/yuzu junta TODOS os cheats ativos de um jogo em um único programa com no máximo
     * 0x400 (1024) palavras de 32 bits. Acima disso o programa inteiro é descartado e nenhum
     * cheat funciona. Por isso o Master Code é gravado UMA vez, em uma pasta própria.
     */
    static final int MAX_WORDS = 0x400;
    static final String MASTER_FOLDER = "00 - Master Code (nao desmarcar)";
    private static final int MAX_NAME_BYTES = 63; // readable_name tem 0x40 bytes no Eden

    /** Cabeçalho seguro: remove ']' / '}' internos e limita o nome a 63 bytes UTF-8. */
    static String safeHeader(Block b) {
        String n = b.name().replace("]", ")").replace("}", ")").replace("[", "(").replace("{", "(").trim();
        if (n.isEmpty()) n = "Cheat";
        byte[] bytes = n.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > MAX_NAME_BYTES) {
            int cut = MAX_NAME_BYTES;
            while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) cut--; // não corta no meio de um caractere
            n = new String(bytes, 0, cut, java.nio.charset.StandardCharsets.UTF_8).trim();
        }
        return b.master ? "{" + n + "}" : "[" + n + "]";
    }

    /** Conta palavras de 8 dígitos hexadecimais (o que o Eden limita a 0x400 no total). */
    static int countWords(List<Block> blocks) {
        int n = 0;
        if (blocks == null) return 0;
        for (Block b : blocks) {
            for (String l : b.lines) {
                String t = l.trim();
                if (!t.isEmpty()) n += t.split("\\s+").length;
            }
        }
        return n;
    }

    /** Une todos os blocos {Master} em um só (o Eden aceita apenas um '{' por arquivo). */
    static Block mergeMasters(List<Block> masters) {
        if (masters == null || masters.isEmpty()) return null;
        Block m = new Block();
        m.master = true;
        m.header = "{Master Code}";
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Block b : masters) {
            String sig = String.join("\n", b.lines);
            if (!seen.add(sig)) continue; // blocos idênticos entram uma vez só
            m.lines.addAll(b.lines);
        }
        return m;
    }

    /** Texto de UM cheat, sem Master Code (ele vai na pasta MASTER_FOLDER). */
    static String buildSingle(Block b, List<Block> ignoredMasters) {
        StringBuilder sb = new StringBuilder();
        sb.append(b.master ? b.header.trim() : safeHeader(b)).append('\n');
        for (String l : b.lines) {
            sb.append(l.trim()).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }

    /** Arquivo da pasta do Master Code: {Master Code} + uma entrada vazia com o nome da pasta. */
    static String buildMasterFile(Block merged) {
        StringBuilder sb = new StringBuilder();
        sb.append("{Master Code}\n");
        for (String l : merged.lines) {
            sb.append(l.trim()).append('\n');
        }
        sb.append('\n').append('[').append(MASTER_FOLDER).append("]\n");
        return sb.toString();
    }

    /**
     * Monta o mapa "pasta/cheats/BID.txt" -> conteúdo: uma pasta por cheat (sem Master Code)
     * e, se houver Master Code, uma única pasta MASTER_FOLDER.
     */
    static java.util.Map<String, byte[]> buildFolders(List<Block> sel, List<Block> masters, String bid) {
        java.util.Map<String, byte[]> out = new java.util.LinkedHashMap<>();
        Block merged = mergeMasters(masters);
        if (merged != null && !merged.lines.isEmpty()) {
            out.put(MASTER_FOLDER + "/cheats/" + bid + ".txt",
                    buildMasterFile(merged).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        for (Block b : sel) {
            if (b.master) continue;
            String folder = Util.sanitize(b.name());
            if (folder.equals(MASTER_FOLDER)) continue;
            out.put(folder + "/cheats/" + bid + ".txt",
                    buildSingle(b, null).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return out;
    }

    /** Mensagem de aviso se a soma dos cheats (+ Master Code) passar do limite do Eden; senão null. */
    static String budgetWarning(List<Block> sel, List<Block> masters) {
        List<Block> real = new ArrayList<>();
        for (Block b : sel) if (!b.master) real.add(b);
        int total = countWords(real);
        Block merged = mergeMasters(masters);
        if (merged != null) total += merged.lines.isEmpty() ? 0 : countWords(java.util.Collections.singletonList(merged));
        if (total <= MAX_WORDS) return null;
        return "Os mods marcados somam " + total + " palavras de código, acima do limite de "
                + MAX_WORDS + " do Eden. Com tudo ativo, NENHUM funciona. Marque menos mods.";
    }

    /** Texto de uma lista de blocos, sem alterar os cabeçalhos (usado pela base de dados local). */
    static String build(List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            sb.append(b.header.trim()).append('\n');
            for (String l : b.lines) {
                sb.append(l.trim()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Igual a build(), mas com nomes seguros para o Eden (limite de 63 bytes, sem ']' internos). */
    static String buildForEden(List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            sb.append(b.master ? b.header.trim() : safeHeader(b)).append('\n');
            for (String l : b.lines) {
                sb.append(l.trim()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
