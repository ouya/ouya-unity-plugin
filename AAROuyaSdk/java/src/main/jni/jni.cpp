#include <jni.h>
#include <android/log.h>

#include <map>
#include <string>
#include <vector>

#define trace(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, "JNI", "trace: %s (%i) " fmt, __FUNCTION__, __LINE__, __VA_ARGS__)

#define PLUGIN_VERSION "2.1.0.5"

#define LOG_TAG "lib-ouya-ndk.cpp"

#define VERBOSE_LOGGING false

#define MAX_CONTROLLERS 4

//axis states
static std::vector< std::map<int, float> > g_axis;

//button states
static std::vector< std::map<int, bool> > g_button;
static std::vector< std::map<int, bool> > g_buttonDown;
static std::vector< std::map<int, bool> > g_buttonUp;
static std::vector< std::map<int, bool> > g_lastButtonDown;
static std::vector< std::map<int, bool> > g_lastButtonUp;
static int g_turretMouseInfo[6] = {0};

void dispatchGenericMotionEventNative(JNIEnv* env, jobject thiz,
									  jint deviceId,
									  jint axis,
									  jfloat val)
{
#if VERBOSE_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Device=%d axis=%d val=%f", deviceId, axis, val);
#endif
	if (deviceId < 0 ||
		deviceId >= MAX_CONTROLLERS)
	{
		deviceId = 0;
	}
	g_axis[deviceId][axis] = val;
}

void dispatchKeyEventNative(JNIEnv* env, jobject thiz,
							jint deviceId,
							jint keyCode,
							jint action)
{
#if VERBOSE_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Device=%d KeyCode=%d Action=%d", deviceId, keyCode, action);
#endif
	if (deviceId < 0 ||
		deviceId >= MAX_CONTROLLERS)
	{
		deviceId = 0;
	}

	bool buttonDown = action == 0;

	if (g_button[deviceId][keyCode] != buttonDown)
	{
		g_button[deviceId][keyCode] = buttonDown;
		if (buttonDown)
		{
			g_buttonDown[deviceId][keyCode] = true;
		}
		else
		{
			g_buttonUp[deviceId][keyCode] = true;
		}
	}
}

static JNINativeMethod method_table[] = {
		{ "dispatchGenericMotionEventNative", "(IIF)V", (void *)dispatchGenericMotionEventNative }
};

static int method_table_size = sizeof(method_table) / sizeof(method_table[0]);

static JNINativeMethod method_table2[] = {
		{ "dispatchKeyEventNative", "(III)V", (void *)dispatchKeyEventNative }
};

static int method_table_size2 = sizeof(method_table2) / sizeof(method_table2[0]);

jint discoverMouseNative(JNIEnv* env, jobject obj);

static JNINativeMethod method_table3[] = {
        { "discoverMouseNative", "()I", (void *)discoverMouseNative }
};

static int method_table_size3 = sizeof(method_table3) / sizeof(method_table3[0]);

jint readReportLoopNative(JNIEnv* env, jobject obj);

static JNINativeMethod method_table4[] = {
        { "readReportLoopNative", "()I", (void *)readReportLoopNative }
};

static int method_table_size4 = sizeof(method_table4) / sizeof(method_table4[0]);

jint stopReadReportLoopNative(JNIEnv* env, jobject obj);

static JNINativeMethod method_table5[] = {
        { "stopReadReportLoopNative", "()I", (void *)stopReadReportLoopNative }
};

static int method_table_size5 = sizeof(method_table5) / sizeof(method_table5[0]);

void setTurretMouseInfoNative(JNIEnv* env, jobject obj, jint index, jint value);

static JNINativeMethod method_table6[] = {
        { "setTurretMouseInfoNative", "(II)V", (void *)setTurretMouseInfoNative }
};

static int method_table_size6 = sizeof(method_table6) / sizeof(method_table6[0]);

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
#if VERBOSE_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "JNI_OnLoad");
#endif

	for (int index = 0; index < MAX_CONTROLLERS; ++index)
	{
		g_axis.push_back(std::map<int, float>());
		g_button.push_back(std::map<int, bool>());
		g_buttonDown.push_back(std::map<int, bool>());
		g_buttonUp.push_back(std::map<int, bool>());
		g_lastButtonDown.push_back(std::map<int, bool>());
		g_lastButtonUp.push_back(std::map<int, bool>());
	}

	JNIEnv* env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
	{
		return JNI_ERR;
	}

    jclass clazz = env->FindClass("com/razerzone/turretmouse/HidJni");
    if (clazz)
    {
        jint ret = env->RegisterNatives(clazz, method_table3, method_table_size3);
        ret = env->RegisterNatives(clazz, method_table4, method_table_size4);
        ret = env->RegisterNatives(clazz, method_table5, method_table_size5);
    }
    else
    {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to find HidJni class");
        return JNI_ERR;
    }

    clazz = env->FindClass("tv/ouya/sdk/MainActivity");
    if (clazz)
    {
        jint ret = env->RegisterNatives(clazz, method_table6, method_table_size6);
    }
    else
    {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to find MainActivity class");
        return JNI_ERR;
    }

	clazz = env->FindClass("tv/ouya/sdk/OuyaInputView");
	if (clazz)
	{
		jint ret = env->RegisterNatives(clazz, method_table, method_table_size);
		ret = env->RegisterNatives(clazz, method_table2, method_table_size2);
		jfieldID fieldNativeInitialized = env->GetStaticFieldID(clazz, "sNativeInitialized", "Z");
		if (fieldNativeInitialized)
		{
			env->SetStaticBooleanField(clazz, fieldNativeInitialized, true);
			env->DeleteLocalRef(clazz);
#if VERBOSE_LOGGING
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native plugin has loaded.");
#endif
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "lib-ouya-ndk: VERSION=%s", PLUGIN_VERSION);
		}
		else
		{
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to find sNativeInitialized field");
			env->DeleteLocalRef(clazz);
			return JNI_ERR;
		}
	}
	else
	{
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to find OuyaInputView class");
		return JNI_ERR;
	}

	return JNI_VERSION_1_6;
}

extern "C"
{
	// Hello world interface
	char* AndroidGetHelloWorld(long* size)
	{
		const char* cString = "Hello World!\0";
		*size = strlen(cString);
		char* result = new char[*cString];
		strcpy(result, cString);
		return result;
	}

	// Release unmanaged memory
	void AndroidReleaseMemory(char* buffer)
	{
		if (NULL == buffer)
		{
			return;
		}

		delete buffer;
	}

	// Example interface
	void AndroidExampleFunction1(unsigned char* a, int b, int* c)
	{
		(*c) = 3;
	}

	// get axis value
	float getAxis(int deviceId, int axis)
	{
		if (deviceId < 0 ||
			deviceId >= MAX_CONTROLLERS)
		{
			return 0.0f;
		}

		std::map<int, float>::const_iterator search = g_axis[deviceId].find(axis);
		if (search != g_axis[deviceId].end())
		{
			return search->second;
		}
		return 0.0f;
	}

	// check if a button is pressed
	bool isPressed(int deviceId, int keyCode)
	{
		if (deviceId < 0 ||
			deviceId >= MAX_CONTROLLERS)
		{
			return false;
		}

		std::map<int, bool>::const_iterator search = g_button[deviceId].find(keyCode);
		if (search != g_button[deviceId].end())
		{
			return search->second;
		}
		return false;
	}

	// check if a button was down
	bool isPressedDown(int deviceId, int keyCode)
	{
		if (deviceId < 0 ||
			deviceId >= MAX_CONTROLLERS)
		{
			return false;
		}

		std::map<int, bool>::const_iterator search = g_lastButtonDown[deviceId].find(keyCode);
		if (search != g_lastButtonDown[deviceId].end())
		{
			return search->second;
		}
		return false;
	}

	// check if a button was up
	bool isPressedUp(int deviceId, int keyCode)
	{
		if (deviceId < 0 ||
			deviceId >= MAX_CONTROLLERS)
		{
			return false;
		}

		std::map<int, bool>::const_iterator search = g_lastButtonUp[deviceId].find(keyCode);
		if (search != g_lastButtonUp[deviceId].end())
		{
			return search->second;
		}
		return false;
	}

	// clear the button state for detecting up and down
	void clearButtonStates()
	{
		if (g_buttonDown.size() == 0) {
			return;
		}
		if (g_buttonUp.size() == 0) {
			return;
		}
		for (int deviceId = 0; deviceId < MAX_CONTROLLERS; ++deviceId)
		{
			g_lastButtonDown[deviceId].clear();
			g_lastButtonUp[deviceId].clear();
			for (std::map<int, bool>::iterator it = g_buttonDown[deviceId].begin(); it != g_buttonDown[deviceId].end(); ++it)
			{
				int keyCode = it->first;
				g_lastButtonDown[deviceId][keyCode] = g_buttonDown[deviceId][keyCode];
			}
			for (std::map<int, bool>::iterator it = g_buttonUp[deviceId].begin(); it != g_buttonUp[deviceId].end(); ++it)
			{
				int keyCode = it->first;
				g_lastButtonUp[deviceId][keyCode] = g_buttonUp[deviceId][keyCode];
			}
			g_buttonDown[deviceId].clear();
			g_buttonUp[deviceId].clear();
		}
	}

	// clear the axis values
	void clearAxes()
	{
		if (g_axis.size() == 0) {
			return;
		}
		for (int deviceId = 0; deviceId < MAX_CONTROLLERS; ++deviceId) {
			g_axis[deviceId].clear();
		}
	}

	// clear the button values
	void clearButtons()
	{
		if (g_button.size() == 0) {
			return;
		}
		if (g_buttonDown.size() == 0) {
			return;
		}
		if (g_buttonUp.size() == 0) {
			return;
		}
		for (int deviceId = 0; deviceId < MAX_CONTROLLERS; ++deviceId) {
			g_button[deviceId].clear();
			g_buttonDown[deviceId].clear();
			g_buttonUp[deviceId].clear();
		}
	}
}


/* Linux */
#include <linux/types.h>
#include <linux/input.h>
#include <linux/hidraw.h>

/*
 * Ugly hack to work around failing compilation on systems that don't
 * yet populate new version of hidraw.h to userspace.
 */
/*#ifndef HIDIOCSFEATURE
#warning Please have your distro update the userspace kernel headers
#define HIDIOCSFEATURE(len)    _IOC(_IOC_WRITE|_IOC_READ, 'H', 0x06, len)
#define HIDIOCGFEATURE(len)    _IOC(_IOC_WRITE|_IOC_READ, 'H', 0x07, len)
#endif*/

/* Unix */
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

/* C */
#include <stdio.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const char *bus_str(int bus);
char mouseLoc[50];
jboolean allowRead = 0;

//DO NOT RUN THIS FUNCTION ON THE UI THREAD!  UI WILL HANG.
jint discoverMouseNative(JNIEnv* env, jobject obj)
{
	int fd;
	int res, desc_size = 0;
	jboolean mouseDiscovered = 0;
	char buf[256];
	struct hidraw_report_descriptor rpt_desc;
	struct hidraw_devinfo info;
	char *rpt_desc_start;
	char *device = "/dev/input/hidraw";

	jclass cls = env->GetObjectClass(obj);
	jmethodID mouseDiscoveredMid = env->GetMethodID(cls, "mouseDiscovered", "()V");
	jmethodID mouseDisconnectedMid = env->GetMethodID(cls, "mouseDisconnected", "()V");
	jmethodID reportReceivedMid = env->GetMethodID(cls, "reportReceived", "([B)V");
	if (mouseDiscoveredMid == 0 || mouseDisconnectedMid == 0 || reportReceivedMid == 0) {
		LOGE("discoverMouseJNI NoSuchMethodError");
		return 0;
	}

	int devIndex = 0;
	char devIndexStr[16];
	char devLoc[50];
	sprintf(devIndexStr, "%d", devIndex);
	strcpy(devLoc, device);
	strcat(devLoc, devIndexStr);
#if VERBOSE_LOGGING
	LOGD("%s",devLoc);
#endif
	while(devIndex < 20) {

		//if (argc > 1)
		// device = argv[1];

		/* Open the Device with blocking reads. In real life,
           don't use a hard coded path; use libudev instead. */

		if (access(devLoc, R_OK) != -1) {
#if VERBOSE_LOGGING
			LOGD("Attempt to open file");
#endif
			fd = open(devLoc, O_RDONLY/*|O_NONBLOCK*/);

			if (fd < 0) {
				char tempStr[16];
				char tempFd[16];
				sprintf(tempFd, "%d", fd);
				//strcpy(tempStr, "Unable to open device. fd=");
				//strcat(tempStr,tempFd);
				LOGE("%s",tempFd);
				//return 0;
			}
			else {
				memset(&rpt_desc, 0x0, sizeof(rpt_desc));
				memset(&rpt_desc_start, 0x0, sizeof(char[4]));
				memset(&info, 0x0, sizeof(info));
				//memset(buf, 0x0, sizeof(buf));

				/* Get Report Descriptor Size */
				res = ioctl(fd, HIDIOCGRDESCSIZE, &desc_size);
				if (res < 0) {
                    LOGE("HIDIOCGRDESCSIZE");
                }
				else {
#if VERBOSE_LOGGING
                    LOGD("Report Descriptor Size: %d\n", desc_size);
#endif
                }


				/* Get Report Descriptor */
				rpt_desc.size = desc_size;
				rpt_desc_start = "";
				res = ioctl(fd, HIDIOCGRDESC, &rpt_desc);
				if (res < 0) {
					LOGE("HIDIOCGRDESC");
				} else {
#if VERBOSE_LOGGING
					LOGD("Report Descriptor:\n");
#endif
					//char tempString[16];
					int n;
					int desc_start = 0;
					int desc_start_size = 4;
					for (n = 0; n < desc_start_size; n++)
						desc_start =
								desc_start + pow(10.f, (desc_start_size - 1 - n)) * rpt_desc.value[n];
					if (desc_start == 5192) {
#if VERBOSE_LOGGING
						LOGD("THIS IS A BOOT MOUSE!!!\n");
#endif
						mouseDiscovered = 1;
						//sprintf(mouseIndexStr, "%d", devIndex);
					}
					LOGD("\n\n");
				}

				/* Get Raw Name */
				res = ioctl(fd, HIDIOCGRAWNAME(256), buf);
				if (res < 0) {
                    LOGE("HIDIOCGRAWNAME");
                }
				else {
#if VERBOSE_LOGGING
                    LOGD("Raw Name: %s\n", buf);
#endif
                }

				/* Get Physical Location */
				/*res = ioctl(fd, HIDIOCGRAWPHYS(256), buf);
                if (res < 0)
                    LOGE("HIDIOCGRAWPHYS");
                else
                    LOGD("Raw Phys: %s\n", buf);*/

				/* Get Raw Info */
				__s16 razerVendorID = 0x1532;
				res = ioctl(fd, HIDIOCGRAWINFO, &info);
				if (res < 0) {
					LOGE("HIDIOCGRAWINFO");
				} else {
#if VERBOSE_LOGGING
					LOGD("Raw Info:\n");
#endif
					//LOGD("\tbustype: %d (%s)\n",
					//     info.bustype, bus_str(info.bustype));
#if VERBOSE_LOGGING
					LOGD("\tvendor: 0x%04hx\n", info.vendor);
					LOGD("\tproduct: 0x%04hx\n", info.product);
#endif
					if (info.vendor == razerVendorID && mouseDiscovered) {
#if VERBOSE_LOGGING
						LOGD("FOUND RAZER MOUSE!!!");
#endif
						strcpy(mouseLoc, device);
						strcat(mouseLoc, devIndexStr);
#if VERBOSE_LOGGING
						LOGD("%s", mouseLoc);
#endif
						//LOGD("%s",devLoc);
					}
				}

				/* Set Feature */
				//buf[0] = 0x9; /* Report Number */
				/*buf[1] = 0xff;
                buf[2] = 0xff;
                buf[3] = 0xff;
                res = ioctl(fd, HIDIOCSFEATURE(4), buf);
                if (res < 0)
                    LOGE("HIDIOCSFEATURE");
                else
                    LOGD("ioctl HIDIOCGFEATURE returned: %d\n", res);*/

				/* Get Feature */
				//buf[0] = 0x9; /* Report Number */
				/*res = ioctl(fd, HIDIOCGFEATURE(256), buf);
                if (res < 0) {
                    LOGE("HIDIOCGFEATURE");
                } else {
                    LOGD("ioctl HIDIOCGFEATURE returned: %d\n", res);
                    LOGD("Report data (not containing the report number):\n\t");
                    for (i = 0; i < res; i++)
                        LOGD("%hhx ", buf[i]);
                    LOGD("\n\n");
                }*/

				/* Send a Report to the Device */
				//buf[0] = 0x1; /* Report Number */
				/*buf[1] = 0x77;
                res = write(fd, buf, 2);
                if (res < 0) {
                    LOGD("Error: %d\n", errno);
                    LOGE("write");
                } else {
                    LOGD("write() wrote %d bytes\n", res);
                }*/
			}
			close(fd);
			if (mouseDiscovered) {
				allowRead = 1;
				env->CallVoidMethod(obj, mouseDiscoveredMid);
				return 1;
			}
		}
		devIndex++;

		sprintf(devIndexStr, "%d", devIndex);
		strcpy(devLoc, device);
		strcat(devLoc, devIndexStr);
#if VERBOSE_LOGGING
		LOGD("%s", devLoc);
#endif
	}
	return 0;
}

//DO NOT RUN THIS FUNCTION ON THE UI THREAD!  UI WILL HANG.
jint readReportLoopNative(JNIEnv* env, jobject obj)
{

	jclass cls = env->GetObjectClass(obj);
	jmethodID mouseDiscoveredMid = env->GetMethodID(cls, "mouseDiscovered", "()V");
	jmethodID mouseDisconnectedMid = env->GetMethodID(cls, "mouseDisconnected", "()V");
	jmethodID reportReceivedMid = env->GetMethodID(cls, "reportReceived", "([B)V");
	if (mouseDiscoveredMid == 0 || mouseDisconnectedMid == 0 || reportReceivedMid == 0) {
		LOGE("readReportLoopJNI NoSuchMethodError");
		return 0;
	}

	if(access(mouseLoc, R_OK) != -1) {
		int fd = open(mouseLoc, O_RDONLY/*|O_NONBLOCK*/);

		while(access(mouseLoc, R_OK) != -1 && allowRead) {
			if (fd < 0) {
				LOGE("Mouse disconnected");
				allowRead = 0;
				env->CallVoidMethod(obj, mouseDisconnectedMid);
				return 0;
			}
			else {
				/* Get a report from the device */
				int data_size = 8;
				jbyte buf[data_size];
				memset(buf, 0x0, sizeof(buf));
				int res = read(fd, buf, data_size);
				if (res < 0) {
					LOGE("Mouse read blocked");
					allowRead = 0;
					env->CallVoidMethod(obj, mouseDisconnectedMid);
					return 0;
				} else {
#if VERBOSE_LOGGING
					LOGD("read() read %d bytes:\n\t", res);
					for (int i = 0; i < res; i++)
						LOGD("%hhx ", buf[i]);
					LOGD("\n\n");
#endif

					jbyteArray retArray = env->NewByteArray(data_size);

					void *temp = env->GetPrimitiveArrayCritical(retArray, 0);
					memcpy(temp, buf, (size_t) data_size);
					env->ReleasePrimitiveArrayCritical(retArray, temp, 0);

					env->CallVoidMethod(obj, reportReceivedMid, retArray);
					env->DeleteLocalRef(retArray);
				}
			}
		}
		close(fd);
	}
}

jint stopReadReportLoopNative(JNIEnv* env, jobject obj)
{
	allowRead = 0;
	return 1;
}

const char *
bus_str(int bus)
{
	switch (bus) {
		case BUS_USB:
			return "USB";
			break;
		case BUS_HIL:
			return "HIL";
			break;
		case BUS_BLUETOOTH:
			return "Bluetooth";
			break;
		case BUS_VIRTUAL:
			return "Virtual";
			break;
		default:
			return "Other";
			break;
	}
}

void setTurretMouseInfoNative(JNIEnv* env, jobject obj, jint index, jint value) {
    g_turretMouseInfo[index] = value;
}

extern "C" int getTurretMouseInfo(int index) {
    if (index < 0 ||
        index > 6) {
        return 0;
    }
    return g_turretMouseInfo[index];
}
