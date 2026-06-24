# Optimizaciones básicas para Minitos

# --- GSON ---
# Mantener los modelos de datos que se parsean desde el API de Minits
-keep class org.juanro.minitos.data.api.MinitsModels** { *; }
-keepclassmembers class org.juanro.minitos.data.api.MinitsModels** { *; }

# --- ROOM ---
# Mantener las entidades de la base de datos para evitar problemas con Room
-keep class org.juanro.minitos.model.entity.** { *; }
-keepclassmembers class org.juanro.minitos.model.entity.** { *; }

# Reglas generales para Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.stream.** { *; }

# --- MAPLIBRE ---
-keep class org.maplibre.gl.** { *; }
-dontwarn org.maplibre.gl.**

# --- OKHTTP / OKIO ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
