package com.example.edencheats;

import java.util.ArrayList;
import java.util.List;

/** Lê/gera arquivos de cheat no formato Atmosphère ([Nome] seguido de linhas hex). */
final class CheatParser {
    private CheatParser() {}

    static final class Block {
        String header = "";
        boolean master;
        final List<String> lines = new ArrayList<>();

        String name() {
            String h = header.trim();
            if (h.length() >= 2) h = h.substring(1, h.length() - 1);
            return h.trim();
        }
    }

    static List<Block> parse(String text) {
        List<Block> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.replace("﻿", "").replace("\r\n", "\n").replace('\r', '\n');
        Block cur = null;
        for (String raw : t.split("\n")) {
            String line = raw.trim();
            boolean isCheat = line.startsWith("[") && line.contains("]");
            boolean isMaster = line.startsWith("{") && line.contains("}");
            if (isCheat || isMaster) {
                cur = new Block();
                cur.header = line;
                cur.master = isMaster;
                out.add(cur);
            } else if (cur != null && !line.isEmpty()) {
                cur.lines.add(line);
            }
        }
        return out;
    }

    static String build(List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            sb.append(b.header).append('\n');
            for (String l : b.lines) {
                sb.append(l).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
