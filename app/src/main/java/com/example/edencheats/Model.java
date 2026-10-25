package com.example.edencheats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Modelo de dados compartilhado entre as fontes e as telas. */
final class Model {
    private Model() {}

    /** Um arquivo a ser instalado dentro da pasta do mod. */
    static final class FileRef {
        final String dest;
        final String url;
        final String zipPath;
        final long size;

        FileRef(String dest, String url, String zipPath, long size) {
            this.dest = dest;
            this.url = url;
            this.zipPath = zipPath;
            this.size = size;
        }
    }

    /** Um item instalável: um arquivo de cheats (por Build ID) ou um mod completo. */
    static final class Item {
        String title = "";
        boolean cheat;
        String bid = "";
        String modName = "Cheats";
        File zip;
        final List<FileRef> files = new ArrayList<>();

        long totalSize() {
            long t = 0;
            for (FileRef f : files) {
                if (f.size > 0) t += f.size;
            }
            return t;
        }
    }

    static final class Game {
        final String tid;
        String name;
        final List<Item> items = new ArrayList<>();

        Game(String tid, String name) {
            this.tid = tid;
            this.name = name;
        }

        String display() {
            if (isUsefulName(name, tid)) return name.trim();

            if (tid != null && !tid.isEmpty()) {
                String n = Names.get(tid);
                if (isUsefulName(n, tid)) {
                    this.name = n.trim();
                    return this.name;
                }
            }

            // Algumas fontes incluem o título no pacote mesmo quando o nome do jogo
            // não foi resolvido pelo Title ID. Use-o como fallback antes de exibir o ID.
            for (Item item : items) {
                if (item == null) continue;
                String candidate = item.title;
                if (isUsefulName(candidate, tid) && !isGenericPackageName(candidate)) {
                    this.name = candidate.trim();
                    return this.name;
                }
                candidate = item.modName;
                if (isUsefulName(candidate, tid) && !isGenericPackageName(candidate)) {
                    this.name = candidate.trim();
                    return this.name;
                }
            }

            return (tid == null || tid.isEmpty()) ? Lang.s(R.string.data_unidentified_name) : tid;
        }

        private static boolean isUsefulName(String candidate, String tid) {
            if (candidate == null) return false;
            String value = candidate.trim();
            return !value.isEmpty()
                    && !value.equalsIgnoreCase(tid == null ? "" : tid.trim())
                    && !value.equalsIgnoreCase("(sem nome)")
                    && !value.equalsIgnoreCase("(nome não identificado)")
                    && !value.equalsIgnoreCase(Lang.s(R.string.data_unidentified_name));
        }

        private static boolean isGenericPackageName(String candidate) {
            String value = candidate.trim().toLowerCase(java.util.Locale.ROOT);
            return value.equals("cheats") || value.equals("mods")
                    || value.equals("mod") || value.equals("unknown")
                    || value.equals("unknown game") || value.equals("sem nome")
                    || value.equals("game") || value.equals("update");
        }
    }
}
