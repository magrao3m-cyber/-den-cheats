# ⚡ SwitchForge (Edição Forja Cibernética · v1.7.1)

A forja definitiva de cheats, mods, otimizações 60 FPS e gerenciamento de saves para Nintendo Switch no Android.

Projetado com a interface futurista **A Forja Cibernética** (Dark OLED, zero emojis infantis, badges minimalistas, cards gamer com indicador lateral de status e console de controle unificado), integração com servidores remotos seguros e uma base offline robusta com mais de **68.000 cheats desduplicados**.

---

## 🎨 Redesign "A Forja Cibernética" (v1.7.0)

O SwitchForge foi reformulado com base em uma especificação visual e ergonômica de alto nível gamer:

1. **Grid de Cards Tecnológicos Unificados:**
   - Fundo escuro azulado (`#121620`), bordas precisas de 1dp (`#1F2637`) e cantos suavizados (8dp).
   - Eliminação de emojis comuns do sistema por badges técnicos lineares: `[SCAN]`, `[PATH]`, `[MODS]`, `[SAVE]`, `[RAW]`, `[TOOL]`.
   - O botão de Varredura destaca-se com sutileza através de badge neon ciano (`[SCAN]`), mantendo perfeita harmonia de peso visual com os demais cards.

2. **Console Unificado de Operações:**
   - Agrupamento do cabeçalho, seletor de servidores remotos, abas de filtro (`◈ Meus Jogos` / `◈ Todos os Jogos`) e barra de busca monoespaçada em um único bloco de comando integrado.
   - **Trio de Ações com Base Neutra & Acentos Semânticos:**
     - `⚡ 60FPS`: Base escura neutra com indicador em Verde Menta neon (`#00F5A0`).
     - `↻ SYNC`: Base escura neutra com indicador de sincronização em Ciano Elétrico (`#00D4FF`).
     - `▲ UPDATE`: Base escura neutra com indicador de atualização em Âmbar Cyber (`#FFB700`).

3. **Lista de Jogos Gamer:**
   - **Elevação Gamer:** Fundo em `#161B26` com bordas sutis de 1dp.
   - **Indicador Lateral de 3dp:** Barra vertical no canto esquerdo de cada card. Brilha em **Verde Menta (`#00F5A0`)** quando o jogo está instalado e detectado no Eden, ou fica em cinza apagado quando não detectado.
   - **Tipografia Tecnológica:** Nomes dos jogos em tipografia encorpada de alto contraste (`#FFFFFF`) e códigos de Title ID / Build ID em fonte monoespaçada estilo terminal.

---

## 🚀 Funcionalidades Principais

### ⚡ Banco Offline Consolidado & Desduplicação Global
- **68.016 Cheats Únicos Incorporados:** Varredura e consolidação de múltiplas fontes globais (HamletDuFromage, tomvita NXCheatCode, ChanseyIsTheBest 60FPS/GFX e base offline).
- **115.869 Duplicatas Eliminadas:** Algoritmo rigoroso de assinatura de código hexadecimal que remove duplicações e consolida blocos idênticos.
- **30.001 Títulos Mapeados:** Catálogo exaustivo com nomes reais de todos os jogos e DLCs em `names.tsv`.

### ⚡ Tarefa Autônoma de Atualização
- **Sincronização Direta:** O próprio aplicativo se conecta aos servidores remotos protegidos para buscar novos cheats lançados na comunidade.
- **Fusão Inteligente em Tempo Real:** Varre os códigos hexadecimais, identifica cheats existentes e incorpora apenas novos truques exclusivos sem apagar os códigos já salvos.
- **Opção de Restauração de Fábrica:** Permite restaurar a base original do APK a qualquer momento através do menu de Ferramentas.

### 🌐 Arquitetura de Servidores Padronizada (Server 1 a Server 7)

| Servidor | Descrição |
|---|---|
| **Server 1** | **Offline · Base Principal (v6):** Banco embutido de alta capacidade com 68.016 cheats consolidados e 30.001 títulos mapeados. Funciona 100% sem internet. |
| **Server 2** | **Nuvem · Alta Velocidade:** Espelho de alta velocidade com atualizações de cheats completos para títulos populares e recentes. |
| **Server 3** | **Nuvem · Mirror Global:** Servidor secundário de contingência para garantir disponibilidade global ininterrupta. |
| **Server 4** | **Nuvem · Repositório Alternativo:** Base estendida com suporte a versões alternativas de títulos do catálogo. |
| **Server 5** | **Nuvem · 60FPS & Resolução:** Patches gráficos, desbloqueio de taxa de quadros (30 FPS → 60 FPS) e modificações de resolução. |
| **Server 6** | **Nuvem · Mods & Otimizações:** Mods estruturados com pastas `romfs`, `exefs` e `exefs_patches`. |
| **Server 7** | **Nuvem · Curadoria Especial:** Seleção refinada de mods e ajustes avançados. |

### 🧩 Instalação de cheats no Eden (limite de 1024 palavras)

O Eden/yuzu junta **todos os cheats ativos** de um jogo em um único programa com no máximo
**0x400 (1024) palavras** de 8 dígitos hexadecimais. Se a soma passar disso, o programa inteiro
é descartado e **nenhum cheat funciona** (cada um funcionava sozinho, mas não vários juntos).

Para evitar isso, o app agora:
- grava o **Master Code uma única vez**, na pasta `00 - Master Code (nao desmarcar)`, em vez de repeti-lo em cada pasta;
- une vários blocos `{...}` em um só (o Eden aceita apenas um por arquivo);
- não grava mais o cabeçalho informativo (`[FINAL FANTASY ... TID=... BID=...]`) dentro de cada cheat;
- limita nomes de cheat a 63 bytes (limite do Eden) e troca `[`/`]` internos;
- avisa, antes de instalar, quando os mods marcados somam mais de 1024 palavras.

Mantenha a pasta do Master Code marcada em *Propriedades > Add-Ons*.

### 🔍 Varredura Profunda no Emulador
- **Detecção Automática:** Localiza os jogos instalados, versões reais e Build IDs a partir dos registros, arquivos de configuração e saves.
- **Auto-Match Neon:** Destaca em verde menta (`[INSTALADO]`) com faixa vertical brilhante o jogo e arquivo de cheats compatível com a sua versão instalada.
- **Seleção Individual:** Escolha cheat por cheat (marcar/desmarcar) antes de instalar, mantendo controle total sobre as modificações.

### 💾 Gerenciador de Saves Integrado
- **Backup em 1 Toque:** Cria arquivos `.zip` individuais ou gerais de todo o progresso dos seus jogos.
- **Restauração Rápida:** Importa saves de backups anteriores para que você possa testar cheats com segurança total.

### 🛠️ Diagnóstico & Limpeza do Emulador
- **Verificador de Chaves (`prod.keys` / `title.keys`):** Alerta imediatamente se as chaves de descriptografia estão presentes e válidas.
- **Limpador de Logs:** Esvazia arquivos de log gigantes (`yuzu_log.txt` / logs do emulador) para liberar armazenamento e acelerar a emulação.
- **Limpador de Cache:** Remove arquivos temporários antigos com 1 toque.

---

## 🔒 Segurança & Privacidade

O SwitchForge foi construído com os mais elevados padrões de segurança para aplicativos Android:

1. **Proteção Anti-Zip Slip (Path Traversal Guard):** O gravador SAF valida rigorosamente cada arquivo, rejeitando qualquer tentativa de injeção de caminhos relativos (`..`) ou barras malformadas.
2. **Network Security Config Estrito:** Tráfego em texto claro (HTTP não criptografado) é estritamente proibido (`cleartextTrafficPermitted=false`). Toda comunicação utiliza TLS 1.2+ criptografado.
3. **Ofuscação & Minificação R8/ProGuard:** O código de release passa por otimização, minificação de classes e remoção de registros de debug para proteger o aplicativo contra engenharia reversa.
4. **Proteção de Backup:** `android:allowBackup="false"` ativado para impedir extração não autorizada de dados via ADB.
5. **Zero Rastreamento / Telemetria:** O aplicativo não coleta, não monitora e não envia quaisquer dados pessoais, identificadores de aparelho ou atividades do usuário para terceiros.

---

## 📲 Como Instalar

1. Baixe o arquivo **SwitchForge-v1.7.0.apk** na aba [Releases](https://github.com/magrao3m-cyber/-den-cheats/releases/latest).
2. No seu dispositivo Android, toque no arquivo baixado e selecione **Instalar** (ative "Instalar de fontes desconhecidas" nas configurações se solicitado).
3. Abra o app, toque em **Pasta Eden** e selecione o diretório de dados do emulador.
4. Toque em **Varredura Eden** ou explore os jogos diretamente no catálogo!

---

## ⚙️ Como Compilar

### Opção A — GitHub Actions (Automático)
1. Faça push para a branch `main` do repositório.
2. Acesse a aba **Actions** → **Build APK** → **Run workflow**.
3. Baixe o artefato compilado `SwitchForge-apk` com o APK pronto para instalação.

### Opção B — Linha de Comando (Gradle)
Com o Android SDK (plataforma 34) e JDK 17 configurados:
```bash
./gradlew assembleRelease
```
O APK final otimizado e assinado será gerado em:
`app/build/outputs/apk/release/app-release.apk`
