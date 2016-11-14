package com.suntrans.wanhua;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import Utils.DbHelper;
import Utils.UiUtils;
import fragment.EnvironmentFragment;
import fragment.PowerControlFragment;
import services.MainService;
import views.BottomMentTab;

import static android.R.attr.color;
import static android.R.attr.data;
import static com.suntrans.wanhua.R.id.img_env;
import static com.suntrans.wanhua.R.id.layout_title;

/**
 * Created by Looney on 2016/10/20.
 */

public class MainActivity extends AppCompatActivity {
    int isWarning = 0;

    private FrameLayout content ;
    private TabLayout tabLayout;
    private PowerControlFragment controlFragment;
    private EnvironmentFragment envFragment;
    public MainService.ibinder binder;  //用于Activity与Service通信
    private ImageView img_control,img_parameter;    //标签栏对应的三个图标
    private TextView tx_con,tx_env;
    private ServiceConnection con = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder=(MainService.ibinder)service;   //activity与service通讯的类，调用对象中的方法可以实现通讯
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplication(), "网络错误！", Toast.LENGTH_SHORT).show();

        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        setLinstener();
    }

    @Override
    protected void onDestroy() {
        try {
            unbindService(con);   //解除Service的绑定
            unregisterReceiver(receiver);  //注销广播接收者
        }catch (Exception e){

        }
        super.onDestroy();
    }

    private void initViews() {
        int isWarning = 1;
        //绑定MainService
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        bindService(intent, con, Context.BIND_AUTO_CREATE);
        // 注册自定义动态广播消息。根据Action识别广播
        IntentFilter filter_dynamic = new IntentFilter();
        filter_dynamic.addAction("com.suntrans.beijing.RECEIVE");
        registerReceiver(receiver, filter_dynamic);

        setContentView(R.layout.main_activity1);
        img_control = (ImageView) findViewById(R.id.img_con);
        img_parameter = (ImageView) findViewById(R.id.img_env);
        content = (FrameLayout) findViewById(R.id.content);
//        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tx_con = (TextView) findViewById(R.id.tx_con);
        tx_env = (TextView) findViewById(R.id.tx_envi);
        controlFragment= new PowerControlFragment();
        img_control.clearColorFilter();    //先清除之前的滤镜效果
        img_control.setColorFilter(0x43B253);
        img_parameter.clearColorFilter();    //先清除之前的滤镜效果
        img_parameter.setColorFilter(Color.GRAY);
        getSupportFragmentManager().beginTransaction().replace(R.id.content,controlFragment).commit();

    }


    private void setLinstener() {
        img_control.setOnClickListener(new MyOnClickListener(0));
        img_parameter.setOnClickListener(new MyOnClickListener(1));
    }

    //广播接收者
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int count = intent.getIntExtra("ContentNum", 0);   //byte数组的长度
            byte[] data = intent.getByteArrayExtra("Content");  //内容数组
            if(count>13)   //通过handler将数据传过去
            {
                try {

                Message msg=new Message();
                msg.obj=data;
                msg.what=data.length;
                if (envFragment!=null){
                    if (isWarning==1){
                        envFragment.handler.sendMessage(msg);
                    }
                }
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }

            }
        }
    };

    @Override
    protected void onResume() {
        isWarning=1;
        super.onResume();
    }

    @Override
    protected void onStop() {
        isWarning=0;
        super.onStop();
    }

    Handler handler1 = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    /**
     * 头标点击监听
     */
    public class MyOnClickListener implements View.OnClickListener {
        private int index = 0;

        public MyOnClickListener(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {
            switch(v.getId())   //判断按下的按钮id，设置标题两个textview的背景颜色
            {

                case R.id.tx_con:
                case R.id.img_con:
                {
                    tx_con.setTextColor(Color.parseColor("#43B253"));  //红色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(0x43B253);                //红色
                    tx_env.setTextColor(Color.GRAY);     //灰色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(Color.GRAY);  //灰色
                    tx_env.setTextColor(Color.GRAY);     //灰色
                    if (controlFragment == null){
                        controlFragment= new PowerControlFragment();
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.content,controlFragment).commit();

                    break;
                }
                case R.id.tx_envi:
                case R.id.img_env:
                {
                    tx_con.setTextColor(Color.GRAY);     //灰色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(Color.GRAY);  //灰色
                    tx_env.setTextColor(Color.GRAY);     //灰色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(0x43B253);  //灰色
                    tx_env.setTextColor(Color.parseColor("#43B253"));     //灰色

                    if (envFragment==null){
                        envFragment = new EnvironmentFragment();
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.content,envFragment).commit();
                    break;
                }
                default:break;
            }
        }
    };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            logoutApp();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private long exitTime = 0;
    private void logoutApp()
    {

        if (System.currentTimeMillis() - exitTime > 2000)
        {
            Toast.makeText(MainActivity.this,"再按一次退出",Toast.LENGTH_SHORT).show();

            exitTime = System.currentTimeMillis();
        } else
        {
//            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
