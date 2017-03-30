SET JDK7=c:\Program Files (x86)\Java\jdk1.8.0_112
SET JAR=%JDK7%\bin\jar.exe
COPY /Y java\build\outputs\aar\java-release.aar ..\OuyaUnityPlugin.aar
CD ..\
IF EXIST classes.jar DEL classes.jar
"%JAR%" -xvf OuyaUnityPlugin.aar classes.jar
IF EXIST OuyaUnityPlugin.jar DEL OuyaUnityPlugin.jar
RENAME classes.jar OuyaUnityPlugin.jar
COPY /Y OuyaUnityPlugin.jar Assets\Plugins\Android\libs\

IF EXIST Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so DEL Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so
COPY AAROuyaSdk\java\build\intermediates\ndk\release\lib\armeabi-v7a\lib-ouya-ndk.so Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so

PAUSE
