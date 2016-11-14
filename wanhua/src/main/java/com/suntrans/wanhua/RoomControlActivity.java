package com.suntrans.wanhua;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Adapter.RoomControlAdapter;
import Utils.DbHelper;
import Utils.LogUtil;
import Utils.UiUtils;
import convert.Converts;
import services.MainService;
import views.WaitDialog;

import static com.suntrans.wanhua.R.id.addr;
import static java.lang.String.valueOf;

/**
 * Created by Looney on 2016/10/21.
 */

public class RoomControlActivity extends AppCompatActivity {
    private String area;
    private ArrayList<String> addrs = new ArrayList<>();
    private TextView title;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private GridLayoutManager manager;
    private LinearLayout ll_back;
    public MainService.ibinder binder;  //用于Activity与Service通信
    private RoomControlAdapter adapter;
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
        Cursor cursor = db.rawQuery("select DISTINCT RSAddr from switchs_tb where Area=?",new String[]{area});
        if(cursor.getCount()>=1) {
            while (cursor.moveToNext()) {
                if (cursor.getString(0)!=null){
                    addrs.add(cursor.getString(0));
                }
            }
        }
        for (String a : addrs) {
            LogUtil.i("房间里有的开关地址:"+addrs);
        }

        cursor = db.rawQuery("select RSAddr,Name,Channel,Image from switchs_tb where Area=?",new String[]{area});

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
                System.out.println("RSaddr=" + cursor.getString(0));
                System.out.println("Name=" + cursor.getString(1));
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
        title.setText(area);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshlayout);
        manager = new GridLayoutManager(this,3);
        adapter = new RoomControlAdapter(this,datas);
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
        adapter.setOnItemClickListener(new RoomControlAdapter.OnItemClickListener() {
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
                        String order = "aa68 "+addr+"06 030"+"0"+"0000";
                        binder.sendOrder(order,2);
                    }else {
                        String order = "aa68 "+addr+"06 030"+"0"+"0000";
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
                showChangedPicDialog(view,position);
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
//                            for (int p = 0;p<states.length;p++){
//                                System.out.println(states[p]);
//                            }
//
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
                            for (int i = 0; i < datas.size(); i++) {
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
                        adapter = new RoomControlAdapter(RoomControlActivity.this,datas);
                        recyclerView.setAdapter(adapter);
                    }

                }
            }catch (Exception e){
                e.printStackTrace();
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

    private void showDialog(){
        mWaitDialog.setWaitText("发送命令中...");
        mWaitDialog.show();
        timer.schedule(new TimeTask1(),2000);
    }

    private void showSuccess(){
        mWaitDialog.setWaitText("成功");
        mWaitDialog.show();
        timer2.schedule(new TimeTask2(),500);
    }

    /**
     * 裁剪图片方法实现
     * @param uri
     */
    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 设置裁剪
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);// 去黑边
        intent.putExtra("scaleUpIfNeeded", true);// 去黑边
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", convert.Converts.dip2px(RoomControlActivity.this.getApplicationContext(), 90));
        intent.putExtra("outputY", convert.Converts.dip2px(RoomControlActivity.this.getApplicationContext(), 90));
//        intent.putExtra("return-data", false);
        startActivityForResult(intent, result_code);
    }
    private int result_code=0;
    private void showChangedPicDialog(View v, final int position) {
        final String  name = datas.get(position).get("Name")+"";
        final AlertDialog.Builder builder = new AlertDialog.Builder(RoomControlActivity.this);
        builder.setTitle("编辑开关信息：");
        builder.setItems(new String[]{"更换图标","开关信息","更改名称"}, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //点击后弹出窗口选择了第几项
                //Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
                switch(which)
                {
                    case 0:  //  更换图标
                    {
                        builder.setTitle("更换图标：");
                        builder.setItems(new String[]{"本地图库","拍照","取消"}, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //点击后弹出窗口选择了第几项
                                //Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
                                switch(which)
                                {
                                    case 0:    //选择本地图库
                                    {
                                        result_code=position;
                                        //打开图库
                                        Intent i = new Intent(
                                                Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                        startActivityForResult(i,10000); //请求码为10000，  用来区分图片是裁剪前的还是裁剪后的
                                        break;
                                    }
                                    case 1:    //选择拍照
                                    {
                                        result_code=position;
                                        //拍照
                                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                                        startActivityForResult(intent, 10001);  //请求码为10001， 用来区分图片是裁剪前的还是裁剪后的
                                        break;
                                    }
                                    case 2://点击取消
                                    {
                                        break;
                                    }
                                    default:break;
                                }}});
                        builder.create().show();
                        break;
                    }
                    case 1:
                        AlertDialog.Builder builder = new AlertDialog.Builder(RoomControlActivity.this);
                        builder.setTitle("开关信息：");
                        builder.setMessage("地址:"+datas.get(position).get("RSAddr")+";通道号:"+datas.get(position).get("Channel"));
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        builder.create().show();
                        break;
                    case 2:
                        showChangedNameDialog(position);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data1) {
        super.onActivityResult(requestCode, resultCode, data1);
        if (resultCode != SwitchDetailActivity.RESULT_CANCELED)
        {
            if(requestCode==10000||requestCode==10001)//如果是刚刚选择完，还未裁剪，则跳转到裁剪的activity
            {
                if (data1 != null) {
                    //取得返回的Uri,基本上选择照片的时候返回的是以Uri形式，但是在拍照中有得机子呢Uri是空的，所以要特别注意
                    Uri mImageCaptureUri = data1.getData();
                    //返回的Uri不为空时，那么图片信息数据都会在Uri中获得。如果为空，那么我们就进行下面的方式获取
                    if (mImageCaptureUri != null) {
                        Bitmap image;
                        try {
                            //这个方法是根据Uri获取Bitmap图片的静态方法
                            image = MediaStore.Images.Media.getBitmap(RoomControlActivity.this.getContentResolver(), mImageCaptureUri);
                            if (image != null) {
                                startPhotoZoom(mImageCaptureUri);    //打开裁剪activity
                            }
                        } catch (Exception e) {
                            Log.i("IBM","URI出错"+e.toString());
                        }
                    }
                    else {
                        Bundle extras = data1.getExtras();
                        Bitmap image=null;
                        if (extras != null) {
                            //这里是有些拍照后的图片是直接存放到Bundle中的所以我们可以从这里面获取Bitmap图片
                            image = extras.getParcelable("data");
                        }
                        // 判断存储卡是否可以用，可用进行存储
                        String state = Environment.getExternalStorageState();
                        if (state.equals(Environment.MEDIA_MOUNTED)) {
                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                            File tempFile = new File(path, "image.jpg");
                            FileOutputStream b = null;
                            try {
                                b = new FileOutputStream(tempFile);
                                image.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    b.flush();
                                    b.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            startPhotoZoom(Uri.fromFile(tempFile));
                        }
                        else {
                            Toast.makeText(RoomControlActivity.this.getApplicationContext(), "未找到存储卡，无法存储照片！", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
            else   //如果是裁剪后返回，调用的回调方法
            {
                if (data1 != null) {
                    LogUtil.i("裁剪完成");
                    //取得返回的Uri,基本上选择照片的时候返回的是以Uri形式，但是在拍照中有得机子呢Uri是空的，所以要特别注意
                    Uri mImageCaptureUri = data1.getData();
                    //返回的Uri不为空时，那么图片信息数据都会在Uri中获得。如果为空，那么我们就进行下面的方式获取
                    if (mImageCaptureUri != null) {
                        Log.i("IBM","URI不为空"+mImageCaptureUri.toString());
                        Bitmap image;
                        try {
                            //这个方法是根据Uri获取Bitmap图片的静态方法
                            image = MediaStore.Images.Media.getBitmap(RoomControlActivity.this.getContentResolver(), mImageCaptureUri);
                            if (image != null) {
                                datas.get(result_code).put("Image",image);
                                DbHelper dh1=new DbHelper(RoomControlActivity.this,"IBMS",null,1);
                                SQLiteDatabase db = dh1.getWritableDatabase();
                                ContentValues cv = new ContentValues();    //内容数组
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
                                cv.put("Image", os.toByteArray());
                                db.update("switchs_tb", cv, "RSAddr=? and Channel=?", new String[]{datas.get(result_code).get("RSAddr")+"",datas.get(result_code).get("Channel")+""});
//                                ((Adapter)grid.getAdapter()).notifyDataSetChanged();   //刷新
                                adapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            Log.i("IBM","URI出错"+e.toString());
                        }
                    }
                    else {
                        Bundle extras = data1.getExtras();
                        Log.i("IBM is a sb",extras.toString());
                        if (extras != null) {
                            //这里是有些拍照后的图片是直接存放到Bundle中的所以我们可以从这里面获取Bitmap图片
                            Bitmap image = extras.getParcelable("data");
                            if (image != null) {
                                LogUtil.e(image==null?"null":"不为空");
                                LogUtil.e(result_code+"==>");
                                datas.get(result_code).put("Image",image);
                                DbHelper dh1=new DbHelper(RoomControlActivity.this,"IBMS",null,1);
                                SQLiteDatabase db = dh1.getWritableDatabase();
                                db.beginTransaction();
                                ContentValues cv = new ContentValues();    //内容数组
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
                                cv.put("Image", os.toByteArray());
                                System.out.println(result_code+"sbbbbbbbbb");
//                                System.out.println(datas.get(result_code).get("RSAddr").toString()+"ssss"+datas.get(result_code).get("Channel").toString());
                                db.update("switchs_tb", cv, "RSAddr=? and Channel=?", new String[]{datas.get(result_code).get("RSAddr")+"",datas.get(result_code).get("Channel")+""});
//                                ((Adapter)grid.getAdapter()).notifyDataSetChanged();   //刷新
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                db.close();
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }

        }
        else   //如果点击了取消，则判断是不是裁剪的activity，若是则返回图片选择或拍照的页面，若不是则不进行操作
        {
            if((requestCode==10000||requestCode==10001))//如果是在选择图片中，点击了取消，不进行操作
            {}
            else
            {
                //如果是在裁剪的页面选择了取消，则打开图库
                Intent i = new Intent(
                        Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,10000); //请求码为10000，  用来区分图片是裁剪前的还是裁剪后的


            }
        }
    }

    private void showChangedNameDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this,R.layout.add_inpput3,null);
        final EditText ed = (EditText)view.findViewById(R.id.addr);
        TextView tv = (TextView) view.findViewById(R.id.tv_name);
        tv.setText("更改通道名");
        ed.setHint("请输入通道名");
        ed.setText(datas.get(position).get("Name")+"");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DbHelper dh = new DbHelper(RoomControlActivity.this, "IBMS", null, 1);
                SQLiteDatabase db=dh.getWritableDatabase();
                db.beginTransaction();
                ContentValues cv = new ContentValues();

                cv.put("Name",ed.getText().toString());
                db.update("switchs_tb",cv,"RSAddr=? and Channel=?",new String[]{(String) datas.get(position).get("RSAddr"),(String) datas.get(position).get("Channel")});
                datas.get(position).put("Name",ed.getText().toString());
                System.out.println(ed.getText().toString());
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                Toast.makeText(RoomControlActivity.this,"修改成功",Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create().show();
    }
}
