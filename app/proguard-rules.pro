# §11.2's "R8 full mode on." Room, Hilt and AndroidX Credentials all ship their own
# consumer-proguard-rules inside their AARs (nothing to add for them here) -- the
# rules below are for the two things that don't: kotlinx.serialization's reflection-
# based serializer lookup, and belt-and-suspenders keeps for the two Google
# libraries Credential Manager sign-in (feature:auth, feature:settings) depends on,
# since a broken serializer or credential type after shrinking fails silently at
# runtime, not at compile time.

# kotlinx.serialization's own documented ProGuard/R8 rules (kotlinx.serialization
# docs, "Android"). Keeps every @Serializable class's generated $$serializer and
# Companion.serializer() lookup -- without this, decoding an EntryDto/AccountDto/
# etc. (core:network) or this app's own LedgerExport (core:data backup) can throw
# a runtime SerializationException that never shows up in a debug build, where
# shrinking is off.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.dumbopanda.ledger.**$$serializer { *; }
-keepclassmembers class com.dumbopanda.ledger.** {
    *** Companion;
}
-keepclasseswithmembers class com.dumbopanda.ledger.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# supabase-kt's own request/response models are also kotlinx.serialization-based;
# unlike Room/Hilt it's not confirmed to bundle its own consumer rules, so this is
# the same protection applied to its namespace rather than an assumption it's
# already covered.
-keep,includedescriptorclasses class io.github.jan.supabase.**$$serializer { *; }
-keepclassmembers class io.github.jan.supabase.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.jan.supabase.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google Identity Services / Credential Manager (feature:auth's sign-in,
# feature:settings' delete-account re-auth) parse a credential response by type
# at runtime -- keep the credential classes themselves rather than relying on
# whatever consumer rules those AARs do or don't already bundle.
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# UserPrefs (core:datastore, protobuf-kotlin-lite) crashed EVERY release-build
# launch on a real device (26 Sep 2026, real-device pass): protobuf's generated
# GeneratedMessageLite subclasses resolve their own fields (e.g. syncMode_) by
# NAME via reflection when first building their message schema, and R8 renamed
# them with no rule telling it not to -- NoSuchFieldException on first read,
# immediate crash. This is protobuf-lite's own documented R8 requirement
# (protobuf-lite doesn't ship a consumer-rules.pro the way Room/Hilt do).
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
