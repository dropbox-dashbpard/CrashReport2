## Add project specific ProGuard rules here.
## By default, the flags in this file are appended to flags specified
## in /Users/xiaocong/opt/sdk/tools/proguard/proguard-android.txt
## You can edit the include path and order by changing the proguardFiles
## directive in build.gradle.
##
## For more details, see
##   http://developer.android.com/guide/developing/tools/proguard.html
#
## Add any project specific keep options here:
#
## Dagger
#-dontwarn dagger.internal.codegen.**
#-keepclassmembers,allowobfuscation class * {
#    @javax.inject.* *;
#    @dagger.* *;
#    <init>();
#}
#-keep class dagger.* { *; }
#-keep class javax.inject.* { *; }
#-keep class * extends dagger.internal.Binding
#-keep class * extends dagger.internal.ModuleAdapter
#-keep class * extends dagger.internal.StaticInjection
#-keep class * extends dagger.internal.BindingsGroup
#
#########--------Retrofit + RxJava--------#########
#-dontwarn rx.**
#
#-dontwarn retrofit.**
#-keep class retrofit.** { *; }
#-dontwarn com.octo.android.robospice.retrofit.RetrofitJackson**
#-dontwarn retrofit.appengine.UrlFetchClient
#-keepattributes *Annotation*
#-keepattributes Signature
#-keepattributes Exceptions
#-keepclasseswithmembers class * {
#    @retrofit.http.* <methods>;
#}
#-dontwarn com.squareup.okhttp.**
#-keep class com.squareup.okhttp.** { *; }
#-keep interface com.squareup.okhttp.** { *; }
#-keep class com.google.gson.** { *; }
#-keep class com.google.inject.** { *; }
#-keep class org.apache.http.** { *; }
#-keep class org.apache.james.mime4j.** { *; }
#-keep class javax.inject.** { *; }
#-dontwarn org.apache.http.**
#-dontwarn android.net.http.AndroidHttpClient
#-dontwarn java.nio.file.*
#
#-dontwarn sun.misc.**
#
#-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
#   long producerIndex;
#   long consumerIndex;
#}
#
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
#   long producerNode;
#   long consumerNode;
#}
#
## DBFlow
#-keep class com.raizlabs.android.dbflow.config.GeneratedDatabaseHolder
#
## ALSO REMEMBER KEEPING YOUR MODEL CLASSES
## Android
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class com.android.vending.licensing.ILicensingService
## Data Model
#-keep class org.tecrash.crashreport2.api.data.** { *; }
#-keep class org.tecrash.crashreport2.api.ApiModule.ApiModule { *; }
#-keep class org.tecrash.crashreport2.db.AppDatabase.AppDatabase { *; }
#-keep class org.tecrash.crashreport2.db.AppDatabase.DbModule { *; }
#-keep class org.tecrash.crashreport2.db.AppDatabase.DropboxModel { *; }
#
## If your project uses WebView with JS, uncomment the following
## and specify the fully qualified class name to the JavaScript interface
## class:
##-keepclassmembers class fqcn.of.javascript.interface.for.webview {
##   public *;
##}
