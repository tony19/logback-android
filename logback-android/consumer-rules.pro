# R8/ProGuard rules shipped inside the AAR and applied automatically to
# consuming apps (issues #229, #344, #352, #364, #378, #380).
#
# logback-android is configured almost entirely through reflection: Joran
# instantiates the appenders, encoders, layouts, and rolling policies named
# in logback.xml by class name, sets their properties via reflective setter
# lookup, and PatternLayout resolves %converters through a class-name map.
# R8 cannot trace any of those references, so without these rules it strips
# or renames classes (e.g. DateTokenConverter, the rolling-policy classes)
# and logging fails only in minified release builds.
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.impl.** { *; }
-keepattributes *Annotation*

# Keep the app's custom appenders, which are typically referenced only by
# class name inside logback.xml.
-keep class * implements ch.qos.logback.core.Appender { *; }

# SMTPAppender's javax.mail/javax.activation dependency is optional
# (compileOnly); suppress R8 missing-class errors when the app doesn't
# bundle it.
-dontwarn javax.mail.**
-dontwarn javax.activation.**
