cd java\build\intermediates\classes\release

CALL javap -s tv.ouya.sdk.DebugInput > ..\..\..\..\..\signature_debuginput.txt

CALL javap -s tv.ouya.sdk.OuyaInputView > ..\..\..\..\..\signature_ouyainputview.txt

CALL javap -s tv.ouya.sdk.MainActivity > ..\..\..\..\..\signature_mainactivity.txt

CALL javap -s tv.ouya.sdk.IOuyaActivity > ..\..\..\..\..\signature_iouyaactivity.txt

CALL javap -s tv.ouya.sdk.OuyaUnityPlugin > ..\..\..\..\..\signature_ouyaunityplugin.txt

CALL javap -s tv.ouya.sdk.UnityOuyaFacade > ..\..\..\..\..\signature_unityouyafacade.txt

CALL javap -s com.razerzone.turretmouse.HidJni > ..\..\..\..\..\signature_hidjni.txt

CALL javap -s com.razerzone.turretmouse.TurretMouseService > ..\..\..\..\..\signature_turretmouseservice.txt
