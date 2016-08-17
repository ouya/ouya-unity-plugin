SET JDK7=c:\NVPACK\jdk1.7.0_71
SET JAR="C:\Program Files\Java\jdk1.8.0_73\bin\jar.exe"
CD ..\
IF EXIST classes.jar DEL classes.jar
"%JDK7%\bin\jar.exe" -xvf OuyaUnityPlugin.aar classes.jar
IF EXIST OuyaUnityPlugin.jar DEL OuyaUnityPlugin.jar
RENAME classes.jar OuyaUnityPlugin.jar
COPY /Y OuyaUnityPlugin.jar Assets\Plugins\Android\libs\

IF EXIST Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so DEL Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so
COPY AAROuyaSdk\java\build\intermediates\ndk\release\lib\armeabi-v7a\lib-ouya-ndk.so Assets\Plugins\Android\libs\armeabi-v7a\lib-ouya-ndk.so

PAUSE
