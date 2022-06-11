package me.zxhm.map.fly;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import me.zxhm.map.MeApplication;
import me.zxhm.map.activity.ControlActivity;

public class CustomWaypointMission {
    private String TAG = "MISSION";

    private List<Waypoint> mWaypointList = new ArrayList<Waypoint>();
    private float mSpeed = 3.0f;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private double aircraftLng,aircraftLat,aircraftAlt,aircraftPich,aircraftRoll,aircraftYaw;
    //判定是否到点的误差
    private double delta = 1;
    private double heightDelta = 5;

    private FlightController mFlightController;
    //任务是否开始执行
    private boolean isRunning = false;
    //航点任务是否开始执行，到达第一个点后才开始
    private boolean isMissionRunning = false;
    private int currentIndex = 0;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    private Camera mCamera;


    public CustomWaypointMission(List<Waypoint> waypointList,float speed, WaypointMissionFinishedAction finishedAction)
    {
        mWaypointList = waypointList;
        mSpeed = speed;
        mFinishedAction = finishedAction;

        BaseProduct product = MeApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null)
        {
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGLE);
            mFlightController.setVerticalControlMode(VerticalControlMode.POSITION);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        }
        initCamera();
    }

    /***
     * 开始任务
     */
    public void start()
    {
        currentIndex = 0;
        isRunning = true;
        mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.e(TAG, "setVirtualStickModeEnabled: " + djiError.getDescription());
                } else {
                    Log.d(TAG, "setVirtualStickModeEnabled: " + "Set Virtual Enable Success");
                    mFlightController.startTakeoff(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        Log.e(TAG, "startTakeoff: " + djiError.getDescription());
                                    } else {
                                        Log.d(TAG, "startTakeoff: " + "Take off Success");
                                        flyToPoint(mWaypointList.get(0));
                                    }
                                }
                            }
                    );
                }
            }
        });

    }

    /***
     * 结束任务
     */
    public void stop()
    {
        isMissionRunning = false;

        if (mFinishedAction == WaypointMissionFinishedAction.GO_HOME)
        {
            isRunning = false;
            mFlightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.e(TAG, "startGoHome: " + djiError.getDescription());
                    } else {
                        Log.d(TAG, "startGoHome: " + "Start Go Home");
                    }
                }
            });
            disableVirtualStickMode();
        }
        else if (mFinishedAction == WaypointMissionFinishedAction.AUTO_LAND)
        {
            isRunning = false;
            land();
            disableVirtualStickMode();
        }
        else if (mFinishedAction == WaypointMissionFinishedAction.GO_FIRST_WAYPOINT)
        {
            flyToPoint(mWaypointList.get(0));
        }
        else if (mFinishedAction == WaypointMissionFinishedAction.NO_ACTION)
        {
            disableVirtualStickMode();
        }

    }

    /**
     * 禁止虚拟摇杆
     */
    private void disableVirtualStickMode()
    {
        if (mSendVirtualStickDataTimer != null)
        {
            mSendVirtualStickDataTimer.cancel();
        }
        mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.e(TAG, "setVirtualStickModeEnabled: " + djiError.getDescription());
                } else {
                    Log.d(TAG, "setVirtualStickModeEnabled: " + "Set Virtual disable Success");
                }
            }
        });
    }

    /**
     * 降落
     */
    private void land()
    {
        mFlightController.startLanding(
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.e(TAG, "startLanding: " + djiError.getDescription());
                        } else {
                            Log.d(TAG, "startLanding: " + "Start Landing");
                        }
                    }
                }
        );
    }

    /**
     * 飞到指定点
     * @param point
     */
    private void flyToPoint(Waypoint point)
    {
        //还没启动就不管
        if (!isRunning)
        {
            return;
        }

        //这两个固定，全程使用前进，全程保持高度
        mPitch = 0;
        mThrottle = point.altitude;

        //还没获取到飞机当前状态时不管
        if (Double.isNaN(aircraftLng) || Double.isNaN((aircraftLat)))
        {
            return;
        }
        //先飞到指定高度再继续，比较安全
        if (Math.abs(aircraftAlt - point.altitude) > heightDelta)
        {
            executeVirtualStickDataTask();
            return;
        }

        double[] targetXY = GaussKruegerConverter.LngLat2XY(point.coordinate.getLongitude(),point.coordinate.getLatitude());
        double[] nowXY = GaussKruegerConverter.LngLat2XY(aircraftLng,aircraftLat);
        double dx = targetXY[0]-nowXY[0];
        double dy = targetXY[1]-nowXY[1];
        double d = Math.sqrt(dx * dx + dy * dy);

        //到点
        if (d < delta)
        {
            goNextPoint();
            return;
        }

        mYaw = (float)calcDeg(nowXY,targetXY);
        mRoll = (float)calcSpeed(nowXY,targetXY);
        executeVirtualStickDataTask();
    }

    /**
     * 飞行到下一个点
     * 如果
     */
    public  void goNextPoint()
    {
        //任务执行时，或者起飞到第一个点时
        if (isMissionRunning || currentIndex == 0)
        {
            //如果是第一个点，就任务开始
            if (currentIndex == 0)
            {
                isMissionRunning = true;
            }
            //如果是正在执行任务并且最后一个点，就任务结束，进入停止流程
            if (isMissionRunning && currentIndex == mWaypointList.size() -1)
            {
                isMissionRunning = false;
                currentIndex = -1;
                stop();
                return;
            }
            //不然就index+1继续飞
            if (currentIndex < mWaypointList.size() -1)
            {
                currentIndex ++;
                if (mWaypointList.get(currentIndex).shootPhotoTimeInterval > 0)
                {
                    executeCaptureTask();
                }
            }
            flyToPoint(mWaypointList.get(currentIndex));
        }
        else if(!isMissionRunning && mFinishedAction == WaypointMissionFinishedAction.GO_FIRST_WAYPOINT)
        {
            isRunning= false;
            land();
            disableVirtualStickMode();
        }
    }

    /**
     * 计算飞行速度
     * @param nowXY
     * @param targetXY
     * @return
     */
    private double calcSpeed(double[] nowXY,double[] targetXY)
    {
        double speed = mSpeed;
        double dx = targetXY[0]-nowXY[0];
        double dy = targetXY[1]-nowXY[1];
        double d = Math.sqrt(dx * dx + dy * dy);

        //根据距离目标的位置计算速度，快到点了速度降下来
        if (d < mSpeed * 2)
        {
            speed = (float) d/2;
        }
        else
        {
            if (currentIndex > 0)
            {
                Waypoint lastPoint = mWaypointList.get(currentIndex -1);
                double[] lastXY = GaussKruegerConverter.LngLat2XY(lastPoint.coordinate.getLongitude(),lastPoint.coordinate.getLatitude());
                double lastDx = lastXY[0]-nowXY[0];
                double lastDy = lastXY[1]-nowXY[1];
                double lastD = Math.sqrt(lastDx * lastDx + lastDy * lastDy);
                if (lastD < 10 && Math.abs(mYaw - aircraftYaw) > 5)
                {
                    speed = 0;
                }
            }
        }

        return  speed;
    }

    /**
     * 计算飞行角度
     * @param nowXY 当前飞机的位置
     * @param targetXY 目标位置
     * @return
     */
    private double calcDeg(double[] nowXY,double[] targetXY)
    {
        double deg = 0;
        double dx = targetXY[0]-nowXY[0];
        double dy = targetXY[1]-nowXY[1];

        //根据目标位置和飞机位置计算角度
        if (dy > 0)
        {
            deg = Math.toDegrees(Math.atan(dx/dy));
        }
        else if (dy == 0)
        {
            if (dx > 0)
            {
                deg = 90;
            }
            else if (dx == 0)
            {
                deg = 0;
            }
            else
            {
                deg = -90;
            }
        }
        else
        {
            if (dx > 0)
            {
                deg = 180 +  Math.toDegrees(Math.atan((targetXY[0]-nowXY[0])/(targetXY[1]-nowXY[1])));
            }
            else  if (dx == 0)
            {
                deg = -180;
            }
            else
            {
                deg = Math.toDegrees(Math.atan((targetXY[0]-nowXY[0])/(targetXY[1]-nowXY[1]))) -180;
            }
        }
        return deg;
    }


    /**
     *执行虚拟摇杆发送任务
     */
    private void executeVirtualStickDataTask()
    {
        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
        }
    }

    /**
     * 初始化相机
     */
    private void initCamera()
    {
         mCamera = MeApplication.getCameraInstance();
         SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
         if (mCamera != null)
         {
             mCamera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                 @Override
                 public void onResult(DJIError djiError) {
                     if (djiError == null) {
                         Log.d(TAG, "startShootPhoto: " + "take photo success" );
                     } else {
                         Log.e(TAG, "startShootPhoto: " + djiError.getDescription());
                     }
                 }
             });
         }
    }

    /**
     * 执行拍照任务
     */
    private void executeCaptureTask()
    {
        if (!isMissionRunning)
        {
            return;
        }

        Waypoint point = mWaypointList.get(currentIndex);

        if (point.shootPhotoTimeInterval > 0)
        {
            //拍照
            if (mCamera != null) {
                mCamera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            Log.d(TAG, "startShootPhoto: " + "take photo success" );
                        } else {
                            Log.e(TAG, "startShootPhoto: " + djiError.getDescription());
                        }
                    }
                });
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    executeCaptureTask();
                }
            }, Math.round(point.shootPhotoTimeInterval) * 1000);
        }

    }

    /**
     * 更新无人机状态
     * @param state
     */
    public void updateAircraftSate(FlightControllerState state)
    {
        aircraftLng = state.getAircraftLocation().getLongitude();
        aircraftLat = state.getAircraftLocation().getLatitude();
        aircraftAlt = state.getAircraftLocation().getAltitude();
        aircraftPich = state.getAttitude().pitch;
        aircraftRoll = state.getAttitude().roll;
        aircraftYaw = state.getAttitude().yaw;
        if (isRunning)
        {
            //在任务中就飞任务点
            if (isMissionRunning || currentIndex == 0)
            {
                Waypoint cwPoint = mWaypointList.get(currentIndex);
                flyToPoint(cwPoint);
            }
            else if (mFinishedAction == WaypointMissionFinishedAction.GO_FIRST_WAYPOINT && currentIndex == -1)
            {
                flyToPoint(mWaypointList.get(0));
            }
        }

        if (state.isLandingConfirmationNeeded())
        {
            mFlightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.e(TAG, "confirmLanding: " + djiError.getDescription());
                    } else {
                        Log.d(TAG, "confirmLanding: " + "Confirm Landing");
                    }
                }
            });
        }
    }

    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {

            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null)
                                {
                                }
                                else
                                {
                                    Log.e(TAG, djiError.getDescription() );
                                }
                            }
                        }
                );
            }
        }
    }
}
