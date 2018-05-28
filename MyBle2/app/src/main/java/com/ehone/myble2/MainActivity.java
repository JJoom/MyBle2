package com.ehone.myble2;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main";
    private Switch stLianjie;
    private BleDevice bleDevice;
    private ProgressDialog progressDialog;
    private String deviceMac = "7C:01:0A:6F:FE:35";
    private Switch stTongZhi;
    private TextView tvweizhi;
    private String  xiaoche_uuid_service = "0000ffe0-0000-1000-8000-00805f9b34fb";

    private String xiaoche_uuid_notify = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic characteristic;
    private VideoView mVideoView;
    private ImageView iv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//       初始化
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setMaxConnectCount(7)
                .setOperateTimeout(5000);

        initView();
    }

    /**
     * 初始化控件的点击事件
     */
    private void initView() {

        //----以下点击时间全是没用的，用来的测试的
        findViewById(R.id.btn01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show("01");
            }
        });
        findViewById(R.id.btn02).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show("02");
            }
        });
        findViewById(R.id.btn03).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show("03");
            }
        });
        findViewById(R.id.btn08).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show("08");
            }
        });
        //-----------
        mVideoView = (VideoView) findViewById(R.id.video);
        iv = (ImageView) findViewById(R.id.iv);


        progressDialog = new ProgressDialog(this);

        stLianjie = (Switch) findViewById(R.id.st_lianjie);
        stLianjie.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b) {

                    connect(false);

                }else{

                    if (BleManager.getInstance().isConnected(bleDevice)) {

                        BleManager.getInstance().disconnect(bleDevice);
                        stTongZhi.setChecked(false);
                        stLianjie.setText("打开连接");
                    }
                }
            }
        });

        stTongZhi = (Switch) findViewById(R.id.st_tongzhi);
        stTongZhi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {

                    tongzhi();



                }else{

                    BleManager.getInstance().stopNotify(
                            bleDevice,
                            xiaoche_uuid_service,
                            xiaoche_uuid_notify);
                    stTongZhi.setText("打开通知");
                    tvweizhi.setText("");

                }
            }
        });

        tvweizhi = (TextView) findViewById(R.id.tv_weizhi);


    }

    /**
     * 打开通知
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void tongzhi() {
        if (!BleManager.getInstance().isConnected(bleDevice)) {
            Toast.makeText(MainActivity.this,"请先连接设备",Toast.LENGTH_SHORT).show();
            stTongZhi.setChecked(false);
        }
        if (bleDevice != null) {
            BluetoothGatt bluetoothGatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
            if (bluetoothGatt != null) {
                for (BluetoothGattService service : bluetoothGatt.getServices()) {

                    if (service.getUuid().toString().equals(xiaoche_uuid_service)) {

                        mService = service;
                    }
                }

                if (mService == null) {
                    Toast.makeText(MainActivity.this,"找不到服务",Toast.LENGTH_SHORT).show();
                    stTongZhi.setChecked(false);
                    return;
                }else{

                    characteristic = mService.getCharacteristic(UUID.fromString(xiaoche_uuid_notify));

                }
                if (characteristic == null) {
                    Toast.makeText(MainActivity.this,"找不到特征",Toast.LENGTH_SHORT).show();
                    stTongZhi.setChecked(false);
                    return;
                }else{

                    BleManager.getInstance().notify(
                            bleDevice,
                            xiaoche_uuid_service,
                            xiaoche_uuid_notify,
                            new BleNotifyCallback() {

                                @Override
                                public void onNotifySuccess() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this,"打开通知成功",Toast.LENGTH_SHORT).show();
                                            stTongZhi.setText("断开通知");
                                        }
                                    });
                                }

                                @Override
                                public void onNotifyFailure(final BleException exception) {
//
//                                    });
                                }

                                @Override
                                public void onCharacteristicChanged( byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String s = HexUtil.formatHexString(characteristic.getValue(), true);
                                            tvweizhi.setText("收到信息" + s);
                                            //播放视屏或者显示图片
                                            show(s);
                                        }
                                    });
                                }
                            });
                }
            }else{
                Toast.makeText(MainActivity.this,"获取协议失败",Toast.LENGTH_SHORT).show();
                stTongZhi.setChecked(false);
            }

        }else{

            Toast.makeText(MainActivity.this,"未找到设备",Toast.LENGTH_SHORT).show();
            stTongZhi.setChecked(false);
            return;
        }


    }

    private void show(String s) {

        if (mVideoView.isPlaying()) {

            mVideoView.pause();
        }
        String uri = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" ;
        switch (s) {

            case "01":
            case "02":
            case "03":

                iv.setVisibility(View.VISIBLE);
                mVideoView.setVisibility(View.GONE);
                break;
            case "04":
            case "05":
            case "06":
            case "07":
            case "08":
            case "09":
            case "10":
                iv.setVisibility(View.GONE);
                mVideoView.setVisibility(View.VISIBLE);
                break;
        }
        switch (s) {

            case "01":
                Toast.makeText(MainActivity.this,"收到指令，显示图片：" + "支付宝到账",Toast.LENGTH_SHORT).show();
                iv.setImageResource(R.drawable.one);
                break;
            case "02":
                Toast.makeText(MainActivity.this,"收到指令，显示图片：" + "微信好友添加成功",Toast.LENGTH_SHORT).show();
                iv.setImageResource(R.drawable.tow);
                break;
            case "03":
                Toast.makeText(MainActivity.this,"收到指令，显示图片：" + "淘宝促销",Toast.LENGTH_SHORT).show();
                iv.setImageResource(R.drawable.three);
                break;
            case "04":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "拉菲红酒广告",Toast.LENGTH_SHORT).show();
                uri += R.raw.four;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "05":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "阳光社区",Toast.LENGTH_SHORT).show();
                uri += R.raw.five;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "06":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "医院就诊",Toast.LENGTH_SHORT).show();
                uri += R.raw.six;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "07":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "蒙娜丽莎",Toast.LENGTH_SHORT).show();
                uri += R.raw.seven;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "08":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "隧道施工",Toast.LENGTH_SHORT).show();
                uri += R.raw.eight;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "09":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "地铁站",Toast.LENGTH_SHORT).show();
                uri += R.raw.nine;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
            case "10":
                Toast.makeText(MainActivity.this,"收到指令，播放视屏：" + "观影提示",Toast.LENGTH_SHORT).show();
                uri += R.raw.ten;
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.start();
                break;
        }

    }

    /**
     * 连接设备
     * @param istongzhi 通过这个字段来自动打开通知
     */
    private void connect( Boolean istongzhi) {

        final Boolean b = istongzhi;
        BleManager.getInstance().connect(deviceMac, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                progressDialog.dismiss();
                stLianjie.setChecked(false);
                Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_LONG).show();
            }


            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                MainActivity.this.bleDevice = bleDevice;
                setMtu(bleDevice, 23);
                stLianjie.setText("断开连接");
                stLianjie.setChecked(true);
                if (b) {

                    stTongZhi.setChecked(true);
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                if (isActiveDisConnected) {
                    stLianjie.setChecked(false);
                    Toast.makeText(MainActivity.this, "断开了", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "连接中断 ");
                } else {
                    stLianjie.setChecked(false);
                    Toast.makeText(MainActivity.this, "连接中断", Toast.LENGTH_LONG).show();
//                    ObserverManager.getInstance().notifyObserver(bleDevice)

                    Log.d(TAG, "连接意外中断 ");
                    if (bleDevice != null) {

                        connect(true);
                    }
                }

            }
        });

    }
    /**
     * 设置数据大小
     * @param bleDevice
     * @param mtu
     */
    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }
}
