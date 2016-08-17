CALL gradlew clean build
COPY /Y java\build\outputs\aar\java-debug.aar ..\OuyaUnityPlugin.aar
CALL copy_libs.cmd
