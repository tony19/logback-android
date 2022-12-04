# Issue #229
-keepclassmembers class ch.qos.logback.classic.pattern.* { <init>(); }

# The following rules should only be used if you plan to keep
# the logging calls in your released app.
-keepclassmembers ch.qos.logback.** { *; }
-keepclassmembers org.slf4j.impl.** { *; }
-keepattributes *Annotation*
