package com.suntrans.wanhua;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import Utils.DbHelper;

import static android.R.attr.data;
import static com.suntrans.wanhua.R.id.addr;
import static com.tencent.bugly.crashreport.crash.c.f;

/**
 * Created by pc on 2016/8/18.
 */
public class Login_Activity extends AppCompatActivity {
    private EditText account =null;
    private EditText password=null;
    private  TextInputLayout ll1;
    private  TextInputLayout ll2;
    private Button login;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        account = (EditText) findViewById(R.id.account);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login);
        ll1 = (TextInputLayout) findViewById(R.id.ll1);
        ll2 = (TextInputLayout) findViewById(R.id.ll2);
//        String a = MyApplication.getSharedPreferences().getString("account","");
//        String b = MyApplication.getSharedPreferences().getString("password","");
        boolean frist = MyApplication.getSharedPreferences().getBoolean("isFristCome",true);
//       System.out.println("a="+a+"b="+b);
        if (frist){
            initDataBase();
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (account.getText().toString().equals("admin") && password.getText().toString().equals("admin")) {
                         String account1 = account.getText().toString();
                         String password1 = password.getText().toString();
                        MyApplication.getSharedPreferences().edit().putString("account",account1).commit();
                        MyApplication.getSharedPreferences().edit().putString("password",password1).commit();
                        System.out.println("保存成功");
                        Intent intent = new Intent();
                        intent.setClass(Login_Activity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);   //页面切换动画
                        finish();   //销毁此页面
                    } else{
                        Toast.makeText(Login_Activity.this,"账号或密码错误!",Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }else {
            ll1.setVisibility(View.INVISIBLE);
            ll2.setVisibility(View.INVISIBLE);
            login.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.setClass(Login_Activity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);   //页面切换动画
                    finish();   //销毁此页面
                }
            }, 1800);
        }

    }
//   private static String[] addrs = {"00060005","00050002","00040001","000e0005",
//            "00090002","000a0004","000f0001","00100003","00080002","00090005","000a0002",
//           "00080001","00070006","00090006","00080003"
//    };

    private static String[] addrs = {"00050001","00040006","00060003","00070001",
            "00040005","00050004"
    };
    private void initDataBase() {
        MyApplication.getSharedPreferences().edit().putBoolean("isFristCome",false).commit();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                DbHelper dh = new DbHelper(Login_Activity.this, "IBMS", null, 1);
                SQLiteDatabase db=dh.getWritableDatabase();
                db.beginTransaction();
//                db.delete("switchs_tb",null,null);
                for (int i=0;i<addrs.length;i++){
                    for (int j=0;j<11;j++){//保存11个开关状态0通道为总开关
                        ContentValues cv = new ContentValues();
                        cv.put("Channel",j+"");
                        cv.put("RSAddr",addrs[i]);
                        db.insert("switchs_tb", null, cv);
                    }
                }

                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
            }
        });
        t.start();
    }


}
