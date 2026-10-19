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

    /** Gera o texto exato de um cheat individual no formato oficial do Eden/Atmosphère. */
    static String buildSingle(Block b, List<Block> masterBlocks) {
        StringBuilder sb = new StringBuilder();
        if (masterBlocks != null) {
            for (Block m : masterBlocks) {
                sb.append(m.header.trim()).append('\n');
                for (String l : m.lines) {
                    sb.append(l.trim()).append('\n');
                }
                sb.append('\n');
            }
        }
        sb.append(b.header.trim()).append('\n');
        for (String l : b.lines) {
            sb.append(l.trim()).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }

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
}
