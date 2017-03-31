using UnityEngine;
#if UNITY_ANDROID && !UNITY_EDITOR
using System.Collections;
using tv.ouya.console.api;
#endif

class OuyaEnableGenericAsTurretMouse : MonoBehaviour
{
#if UNITY_ANDROID && !UNITY_EDITOR
    public IEnumerator Start()
    {
        // wait for IAP to initialize
        while (!OuyaSDK.isIAPInitComplete())
        {
            yield return null;
        }

        OuyaSDK.enableGenericAsTurretMouse();
    }
#endif
}
