package com.starunion.hefanlive.impl.hsy;

import android.util.Log;

import com.arcvideo.camerarecorder.CameraTypes;

/**
 * Created by Administrator on 2016/12/7.
 */
public class HsyLiveShowError {
    /**
     * 错误
     *
     * @param res 错误标示
     * */
    public static  String showError(int res) {
        String errorInfo = "";
        Log.e("TAG", "ShowError res = " + res);
        switch (res) {
            case CameraTypes.MEDIA_ERR_AUDIOCODEC_UNSUPPORT:
                errorInfo = "音频不支持！";
                break;
            case CameraTypes.MEDIA_ERR_AUDIO_INPUT_OPEN:
                errorInfo = "音频打开失败！请确认麦克风已授权访问...";
                break;
            case CameraTypes.MEDIA_ERR_AUDIO_INPUT_CLOSE:
                errorInfo = "音频关闭失败！";
                break;
            case CameraTypes.MEDIA_ERR_AUDIO_INPUT_RECORDING:
                errorInfo = "音频录制失败！";
                break;
            case CameraTypes.MEDIA_ERR_AUDIO_INPUT_PAUSE:
                errorInfo = "音频暂停失败！";
                break;
            case CameraTypes.MEDIA_ERR_AUDIO_INPUT_STOP:
                errorInfo = "音频停止失败！";
                break;
            case CameraTypes.MEDIA_ERR_VIDEOCODEC_UNSUPPORT:
                errorInfo = "视频不支持！";
                break;
            case CameraTypes.MEDIA_ERR_INVALID_PARAM:
                errorInfo = "设置参数错误！";
                break;
            case CameraTypes.MEDIA_ERR_OPERATION_NOT_SUPPORT:
                errorInfo = "Publisher操作不支持！";
                break;
            case CameraTypes.MEDIA_ERR_NOT_INIT:
                errorInfo = "Publisher初始化失败！";
                break;
            case CameraTypes.MEDIA_ERR_GET_PLUGIN_INPUTSTREAM:
                errorInfo = "Publisher创建失败！";
                break;
            case CameraTypes.MEDIA_ERR_MEM_NOT_ENOUGH:
                errorInfo = "Publisher内存不足！";
                break;
            case CameraTypes.MEDIA_ERR_NOT_READY:
                errorInfo = "Publisher未准备好！";
                break;
            default:
                errorInfo = "未知错误!";
                break;
        }
        errorInfo += " res = " + res;
        if (CameraTypes.MEDIA_ERR_NONE != res) {
            // Toast.makeText(getMyActivity(), errorInfo, Toast.LENGTH_SHORT).show();
        }
        Log.e("错误","errorInfo=="+errorInfo);
        return errorInfo;
    }
}
