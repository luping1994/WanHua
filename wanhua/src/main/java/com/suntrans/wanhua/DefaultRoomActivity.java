package com.suntrans.wanhua;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Adapter.DefaultRoomAdapter;
import Adapter.RoomControlAdapter;
import Utils.DbHelper;
import Utils.LogUtil;
import Utils.UiUtils;
import convert.Converts;
import services.MainService;
import views.WaitDialog;

import static java.lang.String.valueOf;

/**
 * Created by Looney on 2016/10/21.
 */

public class DefaultRoomActivity extends AppCompatActivity {
    private String area;
    private ArrayList<String> addrs = new ArrayList<>();
    private TextView title;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private GridLayoutManager manager;
    private LinearLayout ll_back;
    public MainService.ibinder binder;  //用于Activity与Service通信
    private DefaultRoomAdapter adapter;
    private WaitDialog mWaitDialog;
    private ArrayList<Map<String,Object>> datas = new ArrayList<>();
    private Handler handler = new Handler();
    private ServiceConnection con = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder=(MainService.ibinder)service;   //activity与service通讯的类，调用对象中的方法可以实现通讯
            new GetDataTask().execute();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplication(), "网络错误！", Toast.LENGTH_SHORT).show();

        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        initService();
        initData();
        initViews();
        setListener();
    }
    private void initService() {
        //绑定MainService
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        bindService(intent, con, Context.BIND_AUTO_CREATE);
        // 注册自定义动态广播消息。根据Action识别广播
        IntentFilter filter_dynamic = new IntentFilter();
        filter_dynamic.addAction("com.suntrans.beijing.RECEIVE");
        registerReceiver(receiver, filter_dynamic);
    }

    private void initData() {
        area = getIntent().getStringExtra("Area");
        datas.clear();
        DbHelper dh = new DbHelper(this, "IBMS", null, 1);
        SQLiteDatabase db=dh.getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = db.rawQuery("select DISTINCT RSAddr from switchs_tb where Area is null",null);
        if(cursor.getCount()>=1) {
            while (cursor.moveToNext()) {
                if (cursor.getString(0)!=null){
                    addrs.add(cursor.getString(0));
                }
            }
        }
//        for (String a : addrs) {
//            LogUtil.i("房间里有的开关地址:"+addrs);
//        }

        cursor = db.rawQuery("select RSAddr,Name,Channel,Image from switchs_tb where Area is null",null);

        if(cursor.getCount()>=1) {
            while (cursor.moveToNext()) {
                Map<String, Object> map = new HashMap<>();
                map.put("RSAddr",cursor.getString(0));
                map.put("Name",cursor.getString(1));
                map.put("Channel",cursor.getString(2));
                byte[] in = cursor.getBlob(3);     //获取图片
                if (in!=null)
                {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
                    map.put("Image", bitmap);
                }
                map.put("State","0");
                datas.add(map);
//                System.out.println("AREA=" + cursor.getString(0));
            }
        }
//        for (int i = 0;i<datas.size();i++){
//            LogUtil.i(datas.get(i).get("Name")+"");
//            LogUtil.i(datas.get(i).get("Channel")+"");
//            LogUtil.i(datas.get(i).get("RSAddr")+"");
//        }
        cursor.close();
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();

    }

    private void initViews() {
        ll_back = (LinearLayout) findViewById(R.id.layout_back);
        mWaitDialog = new WaitDialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        mWaitDialog.setCancelable(false);
        title = (TextView) findViewById(R.id.title_name);
        title.setText("未分类");
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshlayout);
        manager = new GridLayoutManager(this,3);
        adapter = new DefaultRoomAdapter(this,datas);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    private void setListener() {
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new GetDataTask().execute();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshLayout.isRefreshing()){
                            refreshLayout.setRefreshing(false);
                        }
                    }
                },2000);
            }
        });
        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        adapter.setOnItemClickListener(new DefaultRoomAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                showDialog();
                if (binder==null){
                    return;
                }
                String ps = (String) datas.get(position).get("State");
                String addr = (String) datas.get(position).get("RSAddr");
                String channel = (String) datas.get(position).get("Channel");
                if (channel.equals("10")){
                    if (TextUtils.equals(ps,"0")){
                        String order = "aa68 "+addr+"06 030"+"a"+"0001";
                        binder.sendOrder(order,2);
                    }else {
                        String order = "aa68 "+addr+"06 030"+"a"+"0000";
                        binder.sendOrder(order,2);
                    }

                }else if (channel.equals("0")){
                    if (TextUtils.equals(ps,"0")){
                        String order = "aa68 "+addr+"06 030"+channel+"0000";
                        binder.sendOrder(order,2);
                    }else {
                        String order = "aa68 "+addr+"06 030"+channel+"0000";
                        binder.sendOrder(order,2);
                    }
                }else {
                    if (TextUtils.equals(ps,"0")){
                        String order = "aa68 "+addr+"06 030"+channel+"0001";
                        binder.sendOrder(order,2);
                    }else {
                        String order = "aa68 "+addr+"06 030"+channel+"0000";
                        binder.sendOrder(order,2);
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DefaultRoomActivity.this);
                builder.setTitle("开关信息：");
                builder.setMessage("地址:"+datas.get(position).get("RSAddr")+";通道号:"+datas.get(position).get("Channel"));
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.create().show();
            }
        });
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



    private String return_addr;//命令返回的开关地址末位
    private String s;//收到的命令
    private  byte[] bits={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};     //从1到8只有一位是1，用于按位与计算，获取某一位的值
    //新建广播接收器，接收服务器的数据并解析，
    protected BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] bytes = intent.getByteArrayExtra("Content");
            s = Converts.Bytes2HexString(bytes);
            if (bytes.length < 10) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshLayout.isRefreshing())
                            refreshLayout.setRefreshing(false);
                    }
                });
                return;
            }

            s=s.toLowerCase();
//            System.out.println("我是sb+"+s);
//            aa6900010001 03 0e 040109bc0010ffff000000800000786a0d0a
            int effective=0;
            for (int k=0;k<addrs.size();k++){
                if (s.substring(0,12).equals("aa69"+addrs.get(k))){
                    effective=1;
                }
            }
            if (effective!=1){
                return;
            }
            try {
                if (s.length() > 20) {
                    return_addr = s.substring(4, 12);   //返回数据的开关地址
                    byte a[] = Converts.HexString2Bytes(s);
                    if (s.substring(12, 14).equals("03"))   //如果是读寄存器状态，解析出开关状态
                    {
//                        AA6900010001030E 0404 09B90010FFFF00000080000065A30D0A
                        if (s.substring(14, 16).equals("0e") || s.substring(14, 16).equals("07")) {
                            String[] states = {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};   //十个通道的状态，state[0]对应1通道
                            for (int i = 0; i < 8; i++)   //先获取前八位的开关状态
                            {
                                states[i] = ((a[9] & bits[i]) == bits[i]) ? "1" : "0";   //1-8通道

                            }
                            for (int i = 0; i < 2; i++) {
                                states[i + 8] = ((a[8] & bits[i]) == bits[i]) ? "1" : "0";  //9、10通道

                            }
                            for (int p = 0;p<states.length;p++){
                                System.out.println(states[p]);
                            }
                            for (int i = 0; i < datas.size(); i++) {//更新状态到集合中
//                                System.out.println(datas.size());
//                                System.out.println(return_addr+"相等吗？=>"+return_addr.equals(datas.get(i).get("RSAddr")));
                                if (return_addr.equals(datas.get(i).get("RSAddr"))){
                                    int channel = Integer.valueOf((String) datas.get(i).get("Channel"));
                                    if (channel!=0){
                                        datas.get(i).put("State",states[channel-1]);
                                    }
                                }
                            }

                        }
                    } else if (s.substring(12, 14).equals("06"))   //单个通道状态发生改变
                    {
                        //aa69 0001 0001 06 0301 000013690d0a
                        int k = 0;         //k是通道号
                        int state = Integer.valueOf(s.substring(21, 22));  //开关状态，1代表打开，0代表关闭
                        if (s.substring(17, 18).equals("a"))
                            k = 10;
                        else
                            k = Integer.valueOf(s.substring(17, 18));   //通道号,int型
                        if (k == 0)                                          //如果通道号为0，则是总开关
                        {
                            if (state == 0) {
                                for (int i = 0; i < datas.size(); i++) {//更新状态到集合中
                                    if (return_addr.equals(datas.get(i).get("RSAddr")))
                                    {
                                        datas.get(i).put("State", "0");
                                    }
                                }
                            }
                        } else     //如果通道号不为0，则更改data中的状态，并更新
                        {
//                                    String[] state2={"0","0","0","0","0","0","0","0","0","0"};   //十个通道的状态，state[0]对应1通道
//                                    state2[k-1] = state+"";
                            for (int i = 1; i < datas.size(); i++) {
                                if (return_addr.equals(datas.get(i).get("RSAddr")))
                                {
                                    if (datas.get(i).get("Channel").equals(valueOf(k))){
                                        datas.get(i).put("State", state == 1 ? "1" : "0");
                                    }
                                }

                            }
                        }
//                        showSuccessDialog();
                    }
                    if (mWaitDialog!=null&&mWaitDialog.isShowing()){
                       showSuccess();
                    }
                    if (adapter != null) {
                        UiUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (refreshLayout.isRefreshing())
                                    refreshLayout.setRefreshing(false);
//                            UiUtils.showToast(UiUtils.getContext(), "success！");
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }else {
                        adapter = new DefaultRoomAdapter(DefaultRoomActivity.this,datas);
                        recyclerView.setAdapter(adapter);
                    }

                }
            }catch (Exception e){
                return;
            }
        }

    } ;//广播接收器

    ///下拉刷新处理的函数。
    private class GetDataTask extends AsyncTask<Void, Void, String> {
        // 后台处理部分
        @Override
        protected String doInBackground(Void... params) {
            // Simulates a background job.
            String str = "1";
            try {
                Thread.sleep(500);
                getSwitchState();
                str = "1"; // 表示请求成功
            } catch (Exception e1) {

                e1.printStackTrace();
                str = "0"; // 表示请求失败
            }
            return str;
        }

        //这里是对刷新的响应，可以利用addFirst（）和addLast()函数将新加的内容加到LISTView中
        //根据AsyncTask的原理，onPostExecute里的result的值就是doInBackground()的返回值
        @Override
        protected void onPostExecute(String result) {

            if(result.equals("1"))  //请求数据成功，根据显示的页面重新初始化listview
            {

            }
            else            //请求数据失败
            {
                Toast.makeText(getApplicationContext(), "刷新失败！", Toast.LENGTH_SHORT).show();
            }
            // Call onRefreshComplete when the list has been refreshed.
            if (refreshLayout.isRefreshing())
                refreshLayout.setRefreshing(false);   //结束加载动作
            super.onPostExecute(result);//这句是必有的，AsyncTask规定的格式
        }
    }

    /**
     * 获取开关每个通道的状态
     */
    private void getSwitchState() {
        new Thread(){
            @Override
            public void run() {
                for (int j=0;j<addrs.size();j++){
                    String order = "aa68"+ addrs.get(j) +"03 0100"+"0007";
                    binder.sendOrder(order,2);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();


    }

    Timer timer = new Timer();
    class TimeTask1 extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mWaitDialog!=null&&mWaitDialog.isShowing()){
                        mWaitDialog.dismiss();
                    }
                }
            });
        }
    }

    Timer timer2 = new Timer();
    class TimeTask2 extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mWaitDialog!=null&&mWaitDialog.isShowing()){
                        mWaitDialog.dismiss();
                    }
                }
            });
        }
    }
    private void showSuccess(){
        mWaitDialog.setWaitText("成功");
        mWaitDialog.show();
        timer2.schedule(new TimeTask2(),500);
    }
    private void showDialog(){
        mWaitDialog.setWaitText("发送命令中...");
        mWaitDialog.show();
        timer.schedule(new TimeTask1(),2000);
    }


}
