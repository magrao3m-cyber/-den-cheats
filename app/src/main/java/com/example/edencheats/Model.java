package com.example.edencheats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Modelo de dados compartilhado entre as fontes e as telas. */
final class Model {
    private Model() {}

    /** Um arquivo a ser instalado dentro da pasta do mod. */
    static final class FileRef {
        final String dest;     // caminho relativo dentro do mod, ex.: cheats/0F1169AA7D0FA452.txt
        final String url;      // se != null, baixa por HTTP
        final String zipPath;  // se url == null, entrada dentro de Item.zip
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
        final String tid;   // pode ser "" quando desconhecido
        String name;
        final List<Item> items = new ArrayList<>();

        Game(String tid, String name) {
            this.tid = tid;
            this.name = name;
        }

        String display() {
            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase(tid)) return name;
            if (tid != null && !tid.isEmpty()) {
                String n = Names.get(tid);
                if (n != null && !n.isEmpty()) {
                    this.name = n;
                    return n;
                }
            }
            return (tid == null || tid.isEmpty()) ? "(sem nome)" : tid;
        }
    }
}
