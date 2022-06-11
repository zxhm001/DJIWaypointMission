package me.zxhm.map;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

public class MeApplication extends Application {

    private static BaseProduct mProduct;
    private static Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MeApplication.this);
    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }

    public static synchronized Context getContext()
    {
        return applicationContext;
    }
}

