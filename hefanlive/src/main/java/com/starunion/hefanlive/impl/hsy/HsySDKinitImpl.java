package com.starunion.hefanlive.impl.hsy;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.arcsoft.MediaPlayer.ModuleManager;
import com.starunion.hefanlive.ihefan.IHefanLive;
import com.starunion.hefanlive.ihefan.IHefanPlayer;
import com.starunion.hefanlive.ihefan.ISDKinit;
import com.starunion.hefanlive.impl.HefanPlayerImpl;
import com.starunion.hefanlive.impl.hsy.player.HsyPlayerImpl;
import com.starunion.hefanlive.impl.hsy.player.IjkPlayerImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by saga_ios on 16/12/6.
 */
public class HsySDKinitImpl implements ISDKinit{

    private static String TAG = "HsySDKinitImpl";

    private static String _strFileDir = null;

    private static String _strLibsDir = null;

    private static boolean _bOuputLog = true;

    @Override
    public void initSDK(Application application) {
        outputLog(TAG, "ArcPlayerApplication");
        CheckCPUinfo();
        getCurAppDir(application);
        copyPlayerIni();
        copyPlginIni(application);
        LoadLibraray();
    }

    @Override
    public IHefanLive createHefanLive(){
        return new HsyLiveImpl();
    }

    @Override
    public IHefanPlayer createHefanPlayer(){
//        return new HsyPlayerImpl();
        return  new IjkPlayerImpl();
    }


    private static void outputLog(String strTAG, String strInfo) {
        if (_bOuputLog)
            Log.i(strTAG, strInfo);
    }

    private void CheckCPUinfo() {
        ProcessBuilder cmd;
        String result = "";

        try {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while (in.read(re) != -1) {
                System.out.println(new String(re));
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Log.i("CPUID", result);
        return;
    }

    /**
     * 文件夹路径
     */
    private String getCurAppDir(Application application) {
        ApplicationInfo applicationInfo = application.getApplicationInfo();
        outputLog(TAG, "cur data dir:" + application.getApplicationInfo().dataDir);
        outputLog(TAG, "cur file dir:"
                + application.getBaseContext().getFilesDir().getAbsolutePath());

        _strLibsDir = applicationInfo.dataDir;
        _strFileDir = application.getBaseContext().getFilesDir().getAbsolutePath();

        if (!_strLibsDir.endsWith("/")) {
            _strLibsDir = _strLibsDir + "/";
        }
        _strLibsDir = _strLibsDir + "lib/";
        outputLog(TAG, "cur libs dir:" + _strLibsDir);

        if (!_strFileDir.endsWith("/"))
            _strFileDir = _strFileDir + "/";

        if (!_strFileDir.endsWith("/"))
            _strFileDir = _strFileDir + "/";

        outputLog(TAG, "cur file dir:" + _strFileDir);
        return application.getApplicationInfo().dataDir;
    }

    private void copyPlayerIni() {

        ArrayList<Integer> codecList = new ArrayList<Integer>();
        codecList.add(ModuleManager.CODEC_SUBTYPE_H264);
        codecList.add(ModuleManager.CODEC_SUBTYPE_MP3);
        codecList.add(ModuleManager.CODEC_SUBTYPE_AAC);
        //WARNING: DO NOT USE CODEC_SUBTYPE_ALL if you expect smaller config file size
        codecList.add(ModuleManager.CODEC_SUBTYPE_ALL);

        ArrayList<Integer> parserList = new ArrayList<Integer>();
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_MP4);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_FLV);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_AVI);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_MKV);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_ASF_STREAMING);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_OGG);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_AAC);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_MP3);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_TS);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_MP2);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_RM);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_FLAC);
//		parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_TS_STREAMING);
        parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_ALL);

        ModuleManager mgr = new ModuleManager(null, codecList, parserList);
        ArrayList<String> modList = mgr.QueryRequiredModules();
        ModuleManager.E_ARCH_TYPE archType = mgr.getCPUArchType();
        outputLog(TAG, "module list(" + modList.size() + ": " + modList);

        File dirFile = new File(_strFileDir);
        if (!dirFile.exists()) {
            if (!dirFile.mkdirs()) {
                return;
            }
        }

        mgr.GenerateConfigFile(_strLibsDir, _strFileDir+"MV3Plugin.ini");
    }

    private void copyPlginIni(Application application) {
        try {
            String strINIFile = _strFileDir;

            File dirFile = new File(strINIFile);
            if (!dirFile.exists()) {
                if (!dirFile.mkdirs()) {
                    return;
                }
            }
            File fileExist = new File(strINIFile + "ArcPlugin.ini");
            if(fileExist.exists()) {
                fileExist.delete();
            }

            InputStream is = application.getBaseContext().getAssets().open("ArcPlugin.ini");
            File file = new File(strINIFile + "ArcPlugin.ini");
            if (!file.exists()) {
                byte[] buf = new byte[1024];
                file.createNewFile();
                FileOutputStream os = new FileOutputStream(file);
                while (true) {
                    int ins = is.read(buf);
                    if (ins > 0) {
                        os.write(buf, 0, ins);
                    } else {
                        is.close();
                        os.flush();
                        os.close();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载so文件
     */
    private static void LoadLibraray() {
        try {
            outputLog(TAG, "LoadLibraray : load libmv3_platform.so");
            System.load(_strLibsDir + "libmv3_platform.so");
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.d(TAG, "load libmv3_platform.so failed," + ex.getMessage());
        }

        try {
            outputLog(TAG, "LoadLibraray : load libmv3_common.so");
            System.load(_strLibsDir + "libmv3_common.so");
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.d(TAG, "load libmv3_common.so failed," + ex.getMessage());
        }

        try {
            outputLog(TAG, "LoadLibraray : load libmv3_mpplat.so");
            System.load(_strLibsDir + "libmv3_mpplat.so");
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.d(TAG, "load libmv3_mpplat.so failed," + ex.getMessage());
        }

        try {
            outputLog(TAG, "LoadLibraray : load libmv3_playerbase.so");
            System.load(_strLibsDir + "libmv3_playerbase.so");
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.d(TAG, "load libmv3_playerbase.so failed," + ex.getMessage());
        }

        int apiVersion = android.os.Build.VERSION.SDK_INT;
        if (apiVersion >= 15) {
            try {
                Log.d(TAG, "LoadLibraray : load libmv3_jni_4.0.so");
                System.load(_strLibsDir + "libmv3_jni_4.0.so");
            } catch (java.lang.UnsatisfiedLinkError ex) {
                Log.d(TAG, "load libmv3_jni_4.0.so failed, " + ex.getMessage());
            }
        }else {
            try {
                Log.d(TAG, "LoadLibraray : load libmv3_jni.so");
                System.load(_strLibsDir + "libmv3_jni.so");
            } catch (java.lang.UnsatisfiedLinkError ex) {
                Log.d(TAG, "load libmv3_jni.so failed, " + ex.getMessage());
            }
        }

        try {
            Log.d("Loadlib", "load libs begin");
            System.loadLibrary("mv3_platform");
            System.loadLibrary("mv3_common");
            System.loadLibrary("mv3_camera");
            System.loadLibrary("mv3_mediarecorder");
            System.loadLibrary("mv3_mediapublisher");
            System.loadLibrary("mv3_mediarecorderjni");
            System.loadLibrary("mv3_cameravideowriter");

            Log.d("Loadlib", "load libs success");
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.e("Loadlib", ex.getMessage() + "load lib failed");
        }
    }


}
