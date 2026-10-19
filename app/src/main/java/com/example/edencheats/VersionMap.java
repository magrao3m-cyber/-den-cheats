package com.example.edencheats;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapeador inteligente de Build IDs para versões legíveis (ex.: v1.3.0, v1.0.0 Base).
 * Ajuda o usuário a saber exatamente qual versão do jogo corresponde a qual Build ID.
 */
public final class VersionMap {
    private VersionMap() {}

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        // Super Mario Odyssey (0100000000010000)
        MAP.put("0100000000010000|B424BE150A8E7D78", "v1.3.0 (Mais Recente)");
        MAP.put("0100000000010000|F5DCCDDB37E97724", "v1.2.0 (Atualização)");
        MAP.put("0100000000010000|3CA12DFAAF9C82DA", "v1.0.0 (Jogo Base)");

        // The Legend of Zelda: Tears of the Kingdom (0100F2C0115B6000)
        MAP.put("0100F2C0115B6000|39CB8A9A4A1C8B20", "v1.2.1 (Mais Recente)");
        MAP.put("0100F2C0115B6000|2566F8319F46D080", "v1.2.0");
        MAP.put("0100F2C0115B6000|D9A02F3A8DA64654", "v1.1.2");
        MAP.put("0100F2C0115B6000|335A7AE54B86D4A5", "v1.1.1");
        MAP.put("0100F2C0115B6000|E7E1382D5EB931B0", "v1.1.0");
        MAP.put("0100F2C0115B6000|90A175B4A1374567", "v1.0.0 (Jogo Base)");

        // The Legend of Zelda: Breath of the Wild (01007EF00011E000)
        MAP.put("01007EF00011E000|F3670696DCE6EB87", "v1.6.0 (Mais Recente)");
        MAP.put("01007EF00011E000|EAE5DE389ADFDE9B", "v1.5.0");

        // Super Smash Bros. Ultimate (0100A6301214E000)
        MAP.put("0100A6301214E000|2B4A12B264ECA2D5", "v13.0.2 (Mais Recente)");
        MAP.put("0100A6301214E000|E81084AEBCDB0A44", "v13.0.1");
        MAP.put("0100A6301214E000|C887E0E5D021FE0F", "v13.0.0");
        MAP.put("0100A6301214E000|7DF051A46DC4DF15", "v1.0.0 (Jogo Base)");

        // Mario Kart 8 Deluxe (0100152000022000)
        MAP.put("0100152000022000|593E919D76023F27", "v3.0.3 (Mais Recente)");
        MAP.put("0100152000022000|3541BDCFBC8F1D2F", "v3.0.1");
        MAP.put("0100152000022000|AC5FEAE4E62FF1CF", "v3.0.0");

        // Pokémon Scarlet (0100A3D008F5C000)
        MAP.put("0100A3D008F5C000|96DF05A12FE10000", "v3.0.1 (Mais Recente)");
        MAP.put("0100A3D008F5C000|F50B94A0EBE24D04", "v3.0.0");

        // Pokémon Violet (01008CF00BAFB000)
        MAP.put("01008CF00BAFB000|96DF05A12FE10000", "v3.0.1 (Mais Recente)");
        MAP.put("01008CF00BAFB000|F50B94A0EBE24D04", "v3.0.0");

        // Metroid Dread (010093801237C000)
        MAP.put("010093801237C000|8F29BC060E9B6047", "v2.1.0 (Mais Recente)");

        // Animal Crossing: New Horizons (01006F8002326000)
        MAP.put("01006F8002326000|59AE018FA6E7FF8B", "v2.0.6 (Mais Recente)");

        // Super Mario 3D World + Bowser's Fury (010028600EBDA000)
        MAP.put("010028600EBDA000|8D74AEBF2944DF92", "v1.1.0 (Mais Recente)");
        MAP.put("010028600EBDA000|E14D4C5110905D41", "v1.0.0 (Jogo Base)");
    }

    private static final Pattern VER_PAT = Pattern.compile("(?i)(?:/|_|v)([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)");

    /**
     * Retorna o nome amigável da versão para um determinado Title ID e Build ID.
     * Exemplo: "v1.3.0 (Mais Recente)" para Super Mario Odyssey B424BE150A8E7D78.
     */
    public static String getVersionLabel(String tid, String bid, String pathHint, List<String> allBids) {
        if (tid == null) tid = "";
        if (bid == null) bid = "";
        String t = tid.toUpperCase(Locale.US);
        String b = bid.toUpperCase(Locale.US);

        // 1. Dicionário de versões conhecidas
        String key = t + "|" + b;
        if (MAP.containsKey(key)) {
            return MAP.get(key);
        }

        // 2. Extração via dica de caminho de arquivo (ex.: repo ademola /1.3.0/)
        if (pathHint != null) {
            Matcher m = VER_PAT.matcher(pathHint);
            if (m.find()) {
                return "v" + m.group(1);
            }
        }

        // 3. Fallback inteligente quando há múltiplos Build IDs conhecidos
        if (allBids != null && allBids.size() > 1) {
            int idx = allBids.indexOf(b);
            if (idx == allBids.size() - 1) {
                return "v" + (idx + 1) + ".0 (Mais Recente)";
            } else if (idx == 0) {
                return "v1.0.0 (Jogo Base)";
            } else if (idx > 0) {
                return "v1." + idx + ".0";
            }
        }

        return "Build " + (b.length() >= 8 ? b.substring(0, 8) + "…" : b);
    }
}
