LOCAL_PATH               := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES          := com_xfgryujk_longexposurecamera_CameraPreview.c
LOCAL_MODULE             := ImageJni
LOCAL_LDLIBS             := -llog -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
