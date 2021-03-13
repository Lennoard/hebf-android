# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\Android\AndroidSDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class android.support.v7.widget.SearchView { *; }

#-keepclassmembers class com.androidvip.hebf.R$* {public static <fields>;}
-keep class com.androidvip.hebf.R$raw*
-keepclassmembers class com.androidvip.hebf.models.** { *; }
-keep class com.androidvip.hebf.models.** { *; }
-keep class com.google.android.material.tabs.** { *; }
-keep class com.google.android.material.slider.** { *; }

-keepattributes SourceFile,LineNumberTable
