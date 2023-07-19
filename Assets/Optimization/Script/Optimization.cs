using System.Collections;
using System.Collections.Generic;
using UnityEngine;

//https://ouya.world/t/small-optimizations-for-games-made-in-unity/581


public class Optimization : MonoBehaviour
{
    public bool disableVSync = true;

    public enum Resolution {r1024x576, r1280x720, r1366x768};
    public Resolution forceResolution16_9 = Resolution.r1280x720;

    private void Awake()
    {
        if (disableVSync)
        {
            Application.targetFrameRate = -1;
            QualitySettings.vSyncCount = 0;
        }

        switch (forceResolution16_9)
        {
            case Resolution.r1024x576:
                Screen.SetResolution(1024, 576, true);
                break;
            case Resolution.r1280x720:
                Screen.SetResolution(1280, 720, true);
                break;
            case Resolution.r1366x768:
                Screen.SetResolution(1366, 768, true);
                break;
        }
    }

    // Start is called before the first frame update
    void Start()
    {
        
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
