# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep WebAppInterface and any Javascript interface methods intact for WebView
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.WebAppInterface {
    public *;
}

# Preserve LineNumberTable and SourceFile for debugging
-keepattributes SourceFile,LineNumberTable

