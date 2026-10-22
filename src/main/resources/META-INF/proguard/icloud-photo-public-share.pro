# R8 and ProGuard pick this file up from the jar, so an app that shrinks its code needs no rules of its own.
# Gson fills the internal *Json classes (ICloudJson.kt) by reflection: their field names must stay those of iCloud's
# JSON, and Gson creates them through their no-arg constructor.
-keep class com.github.mvysny.icloudphotopublicshare.*Json {
    <fields>;
    <init>();
}
