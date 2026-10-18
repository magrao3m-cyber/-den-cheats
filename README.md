# Eden Cheats Manager (Android)

App Android para baixar e instalar **cheats e mods de Switch** no **Eden** (emulador para Android),
no estilo do *Eden Cheats Manager* e do *ns-emu-cheats-downloader*.

## Fontes de dados

| Fonte | Como funciona |
|---|---|
| **Offline · seu .zip (ibnux)** | `switch-cheat-master.zip` já embutido no app (`app/src/main/assets/ibnux_db.zip`, ~2.100 jogos). Funciona sem internet. |
| **Online · switch-cheats-db (HamletDuFromage)** | Baixa `contents.zip` da última release do GitHub (e guarda em cache). |
| **Online · switch-cheats-db · 60FPS/Res/Gráficos** | Baixa `contents_60fps-res-gfx.zip` da mesma release. |
| **Online · Mod Database (ADEMOLA200)** | Lista os arquivos do repositório pela API do GitHub e baixa só o que você escolher (cheats, patches `.pchtxt`, romfs). |

## Recursos

- Busca por **nome, Title ID ou Build ID**.
- Na tela do jogo, informe o **seu Build ID**: os arquivos que combinam ficam em verde, no topo.
- Escolha **cheat por cheat** (marcar/desmarcar) antes de instalar.
- **Instala direto** em `load/<TitleID>/<Mod>/cheats/<BuildID>.txt` (o Build ID é gravado em MAIÚSCULAS, padrão do yuzu/Eden, já que o Android diferencia maiúsculas de minúsculas).
- Mods com `romfs` / `exefs` (patches de 60 FPS etc.) mantêm a estrutura correta.
- **Colar código**: cole um cheat copiado de um fórum e informe Title ID + Build ID.
- **Instalados**: lista e apaga o que foi instalado.
- **Exportar ZIP** com a estrutura `<TitleID>/<Mod>/...` quando não for possível gravar direto.

## Compilar o APK

### Opção A — GitHub Actions (sem instalar nada)
1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (inclui `.github/workflows/build-apk.yml`).
2. Abra a aba **Actions** → *Build APK* → **Run workflow** (ou faça um push).
3. Ao terminar (~5 min), baixe o artefato **EdenCheatsManager-apk** e instale o `.apk` no celular
   (permita "instalar apps desconhecidos").

### Opção B — Android Studio
Abra esta pasta (Android Studio Koala+ / JDK 17) e use **Build → Build APK(s)**.

### Opção C — linha de comando
Com o Android SDK (plataforma 34) instalado e `ANDROID_HOME` definido:

```
./gradlew assembleRelease
```
O APK sai em `app/build/outputs/apk/release/`. (A build "release" é assinada com a chave de debug
para poder ser instalada direto; para publicar em loja, configure sua própria assinatura.)

## Configurando a pasta de destino no Eden

Toque em **Pasta** e escolha onde gravar. O app cria `load/<TitleID>/<Mod>/` dentro dela.

- **Android 10 ou inferior / 11–12:** navegue até `Android > data > dev.eden.eden_emulator > files`
  e toque em *Usar esta pasta*. (O pacote do Eden pode ser alterado no diálogo, caso o seu seja outro.)
- **Android 13+:** o sistema bloqueia `Android/data` para apps. Escolha uma pasta comum
  (ex.: *Documentos/EdenMods*), depois copie a pasta `load` para dentro da pasta de dados do Eden
  com um gerenciador de arquivos que tenha acesso, ou instale o mod pelo Eden
  (Propriedades do jogo → Add-ons).

Depois de instalar, abra o jogo no Eden → **Propriedades → Add-ons** e ative os cheats.

## Observações

- O Build ID do **seu** jogo precisa ser igual ao do arquivo de cheats; atualizações do jogo mudam o Build ID.
- A API do GitHub sem login permite ~60 consultas/hora; a lista do repositório de mods fica em cache
  (use *Atualizar* para renovar).
- Os bancos de cheats pertencem aos seus autores (ibnux, HamletDuFromage, ADEMOLA200 e as fontes
  que eles compilam); este app só os baixa e organiza.
