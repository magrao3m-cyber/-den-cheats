# Política de Segurança do SwitchForge

## 🛡️ Diretrizes de Segurança

O projeto **SwitchForge** adota práticas ativas para garantir a integridade dos dados e a segurança do usuário durante a utilização de mods e cheats no Android.

### 1. Política de Divulgação de Vulnerabilidades
Se você encontrar qualquer problema de segurança, comportamento inesperado no gravador de arquivos ou falha na validação de dados:
- Abra uma issue de segurança ou reporte privadamente através do repositório.
- Inclua detalhes da versão do SwitchForge, modelo do aparelho e passos para reprodução.

### 2. Medidas de Segurança Implementadas
- **Proteção contra Path Traversal (Anti-Zip Slip):** Todas as gravações via Storage Access Framework (SAF) validam nomes de diretórios e bloqueiam tentativas de escrita fora do escopo selecionado (`..`, barras invertidas ou barras iniciais são sumariamente descartadas).
- **Comunicação Segura (HTTPS / TLS 1.2+):** O tráfego de rede é estritamente restrito a conexões seguras criptografadas através do `network_security_config.xml`.
- **Proteção de Código:** Compilações de produção utilizam R8/ProGuard para minificação e ofuscação de código e remoção de registros de depuração sensíveis.
- **Isolamento de Dados:** A opção `android:allowBackup="false"` impede que os dados locais do aplicativo sejam exportados sem autorização através de comandos ADB.
- **Privacidade do Usuário:** O SwitchForge não integra SDKs de análise comportamental, rastreadores ou telemetria.
