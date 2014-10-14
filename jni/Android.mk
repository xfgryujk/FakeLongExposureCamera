LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES:=com_xfgryujk_longexposurecamera_CameraPreview.c
LOCAL_MODULE := ImageJni
LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)
LOCAL_SHARED_LIBRARIES := libutils
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS :=optional
include $(BUILD_SHARED_LIBRARY)
