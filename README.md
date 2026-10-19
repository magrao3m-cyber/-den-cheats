# ⚡ SwitchForge (Cyber Edition)

A forja definitiva de cheats, mods, otimizações 60 FPS e gerenciamento de saves para Nintendo Switch no Android.

Projetado com interface dark OLED tecnológica, integração com servidores remotos anônimos e uma tarefa autônoma de atualização com desduplicação de códigos em tempo real.

---

## 🚀 Funcionalidades Principais

### ⚡ Tarefa Autônoma de Atualização
- **Sincronização Direta:** O próprio aplicativo se conecta aos servidores remotos protegidos para buscar os cheats mais recentes lançados para cada jogo.
- **Desduplicação Inteligente em Tempo Real:** Varre os códigos hexadecimais, identifica cheats duplicados ou já existentes e incorpora apenas novos truques exclusivos.
- **Atualização Sem Reinstalação:** Atualiza o banco de dados interno diretamente no armazenamento do celular sem que o usuário precise baixar outro APK.
- **Opção de Restauração de Fábrica:** Permite restaurar a base original a qualquer momento através do menu de Ferramentas.

### 🌐 Arquitetura de Servidores Padronizada (Server 1 a Server 7)
Os servidores fornecem redundância, anonimização e máxima segurança:

| Servidor | Descrição |
|---|---|
| **Server 1** | **Offline · Base Principal:** Banco embutido de alta capacidade com mais de 66.000 cheats desduplicados e 29.999 títulos reais mapeados. Funciona 100% sem internet. |
| **Server 2** | **Nuvem · Alta Velocidade:** Espelho de alta velocidade com atualizações de cheats completos para títulos populares e recentes. |
| **Server 3** | **Nuvem · Mirror Global:** Servidor secundário de contingência para garantir disponibilidade global ininterrupta. |
| **Server 4** | **Nuvem · Repositório Alternativo:** Base estendida com suporte a versões alternativas de títulos do catálogo. |
| **Server 5** | **Nuvem · 60FPS & Resolução:** Patches gráficos, desbloqueio de taxa de quadros (30 FPS → 60 FPS) e modificações de resolução. |
| **Server 6** | **Nuvem · Mods & Otimizações:** Mods estruturados com pastas `romfs`, `exefs` e `exefs_patches`. |
| **Server 7** | **Nuvem · Curadoria Especial:** Seleção refinada de mods e ajustes avançados. |

### 🔍 Varredura Profunda no Emulador
- **Detecção Automática:** Localiza os jogos instalados, versões reais e Build IDs a partir dos registros, arquivos de configuração e saves.
- **Auto-Match Neon:** Destaca em verde neon (`[MATCH]`) o arquivo de cheats exatamente compatível com a sua versão instalada.
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

1. Baixe o arquivo **SwitchForge.apk** na aba [Releases](https://github.com/magrao3m-cyber/-den-cheats/releases/latest).
2. No seu dispositivo Android, toque no arquivo baixado e selecione **Instalar** (ative "Instalar de fontes desconhecidas" nas configurações se solicitado).
3. Abra o app, toque em **📁 Pasta Eden** e selecione o diretório de dados do emulador.
4. Toque em **🔍 Varredura Eden** ou explore os jogos diretamente no catálogo!

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
