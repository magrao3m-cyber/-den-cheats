# ==============================================================================
# SwitchForge - Regras de Otimização, Minificação e Ofuscação ProGuard / R8
# ==============================================================================

# Preserva componentes fundamentais do Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Preserva construtores de View caso instanciados via reflexão/XML
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Preserva modelos de dados, estruturas internas e tarefas assíncronas
-keep class com.example.edencheats.Model { *; }
-keep class com.example.edencheats.Model$* { *; }
-keep class com.example.edencheats.EdenScanner { *; }
-keep class com.example.edencheats.EdenScanner$* { *; }
-keep class com.example.edencheats.CheatParser { *; }
-keep class com.example.edencheats.CheatParser$* { *; }
-keep class com.example.edencheats.AutoUpdater { *; }
-keep class com.example.edencheats.AutoUpdater$* { *; }
-keep class com.example.edencheats.VersionMap { *; }
-keep class com.example.edencheats.Names { *; }

# Remove logs de debug em release para maior segurança e desempenho
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

-dontusemixedcaseclassnames
