package com.suntrans.wanhua;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Utils.DbHelper;
import Utils.Utils;

/**
 * Created by Looney on 2016/10/20.
 */
public class DeviceManagerActivity extends AppCompatActivity{
    private LinearLayout ll_add;
    private LinearLayout ll_back;
    private RecyclerView recyclerView;
    private GridLayoutManager manager;
    private ArrayList<Map<String,String>> data = new ArrayList<>();
    private MyAdapter adapter = new MyAdapter();
    private TextView textView_empty;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicemanager);
        initData();
        initViews();
        setListener();
    }



    private void initData() {
        data.clear();
        DbHelper dh = new DbHelper(this, "IBMS", null, 1);
        SQLiteDatabase db=dh.getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = db.rawQuery("select DISTINCT RSAddr from switchs_tb",null);
        if(cursor.getCount()>=1) {
            while (cursor.moveToNext()) {
                Map<String, String> map = new HashMap<>();
                map.put("RSAddr",cursor.getString(0));
                data.add(map);
            }
        }
        cursor.close();
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    private void initViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        textView_empty = (TextView) findViewById(R.id.text_empty);
        ll_back = (LinearLayout) findViewById(R.id.layout_back);
        ll_add = (LinearLayout) findViewById(R.id.layout_add);
        manager = new GridLayoutManager(this,3);
        adapter = new MyAdapter();
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        if (data.size()==0){
            textView_empty.setVisibility(View.VISIBLE);
        }else {
            textView_empty.setVisibility(View.GONE);
        }
    }

    private void setListener() {
        ll_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });
        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private Handler handler = new Handler();
    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = View.inflate(this,R.layout.add_inpput2,null);
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int flag =0;
                String addr = ((EditText) view.findViewById(R.id.addr)).getText().toString();
                DbHelper dh = new DbHelper(DeviceManagerActivity.this, "IBMS", null, 1);
                if (Utils.checkNameAndAddr2(addr)){
                    if (addr==null){
                        Toast.makeText(DeviceManagerActivity.this, "添加失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SQLiteDatabase db=dh.getWritableDatabase();
                    db.beginTransaction();
                    Cursor cursor = db.query(true, "switchs_tb", new String[]{"RSAddr"}, "RSAddr=?", new String[]{addr}, null, null, null, null);
                    if (cursor.getCount() > 0) {
                        flag = 0;
                        Toast.makeText(DeviceManagerActivity.this, "该开关地址已存在，添加失败！", Toast.LENGTH_SHORT).show();
                    }else {
                        for (int i=0;i<11;i++){//保存11个开关状态0通道为总开关
                            ContentValues cv = new ContentValues();
                            cv.put("Channel",i+"");
                            cv.put("RSAddr",addr+"");
                            db.insert("switchs_tb", null, cv);
                        }
                        Toast.makeText(getApplicationContext(), "添加成功！", Toast.LENGTH_SHORT).show();
                        flag = 1;
                    }
                    cursor.close();
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.close();
                    if (flag==1)
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initData();
                                textView_empty.setVisibility(View.GONE);
                                adapter.notifyDataSetChanged();
                            }
                        },500);
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private class MyAdapter extends RecyclerView.Adapter{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder= new ViewHolder(LayoutInflater.from(DeviceManagerActivity.this)
                    .inflate(R.layout.devicesman_item, parent,false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ((ViewHolder) (holder)).setData(position);
            ((ViewHolder) (holder)).imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showModifyDialog(v,position);
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            TextView textView;
            ImageView imageView;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.tv);
                imageView = (ImageView) itemView.findViewById(R.id.iv);
            }

            public void setData(final int position) {
                textView.setText(data.get(position).get("RSAddr"));
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.putExtra("RSAddr",data.get(position).get("RSAddr"));
                        intent.setClass(DeviceManagerActivity.this, SwitchDetailActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);   //页面切换动画
                    }
                });
            }
        }
    }


    //修改名称dialog
    private void showModifyDialog(View view, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(new String[]{ "删除设备"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
//                    case 0://修改名称
//                        AlertDialog.Builder builder1 = new AlertDialog.Builder(DeviceManagerActivity.this);
//                        View v1 = View.inflate(DeviceManagerActivity.this, R.layout.modify_input,null);
//                        final EditText text = (EditText) v1.findViewById(R.id.name);
//                        TextView textView = (TextView) v1.findViewById(R.id.tv_title);
//                        textView.setText("修改名称");
//                        builder1.setView(v1);
//                        builder1.setPositiveButton("修改", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                DbHelper dh1=new DbHelper(DeviceManagerActivity.this,"IBMS",null,1);
//                                SQLiteDatabase db = dh1.getWritableDatabase();
//                                db.beginTransaction();
//                                ContentValues cv = new ContentValues();    //内容数组
//                                cv.put("Name",text.getText().toString());
//                                db.update("sixsensor_tb",cv,"RSAddr=?",new String[]{data.get(position).get("RSAddr")});
//                                db.setTransactionSuccessful();
//                                db.endTransaction();
//                                db.close();
//                                initData();
//                                adapter.notifyDataSetChanged();
//                                Toast.makeText(DeviceManagerActivity.this,"修改成功！",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                        builder1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        });
//                        builder1.show();
//                        break;
                    case 0:
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(DeviceManagerActivity.this);
                        builder2.setTitle("删除设备");
                        builder2.setMessage("你确认要删除吗?");
                        builder2.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DbHelper dh1=new DbHelper(DeviceManagerActivity.this,"IBMS",null,1);
                                SQLiteDatabase db = dh1.getWritableDatabase();
                                db.beginTransaction();
                                db.delete("switchs_tb","RSAddr=?",new String[]{data.get(position).get("RSAddr")});
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                db.close();
                                initData();
                                adapter.notifyDataSetChanged();
                                Toast.makeText(DeviceManagerActivity.this,"删除成功！",Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder2.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        builder2.show();
                        break;
                }
            }
        });
        builder.setTitle(data.get(position).get("Name"));
        builder.create().show();
    }
}
