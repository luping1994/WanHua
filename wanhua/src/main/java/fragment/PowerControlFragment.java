package fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.suntrans.wanhua.DefaultRoomActivity;
import com.suntrans.wanhua.DeviceManagerActivity;
import com.suntrans.wanhua.MyApplication;
import com.suntrans.wanhua.R;
import com.suntrans.wanhua.RoomControlActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Adapter.ControlMainAdapter;
import Utils.DbHelper;
import Utils.UiUtils;

/**
 * Created by Looney on 2016/10/20.
 */
public class PowerControlFragment extends Fragment{
    private View rootView;
    private ArrayList<Map<String,String>> data = new ArrayList<>();
    private RecyclerView recyclerView;
    private LinearLayout layout_device;
    private GridLayoutManager manager;
    private ControlMainAdapter adapter;
    private LinearLayout layout_title;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.control_fragment,null);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        initViews();
        setListener();
    }


    int isUpdate =0;
    private void initData() {
        data.clear();
        DbHelper dh = new DbHelper(getActivity(), "IBMS", null, 1);
        SQLiteDatabase db=dh.getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = db.rawQuery("select DISTINCT Area from switchs_tb",null);
        if(cursor.getCount()>=1) {
            while (cursor.moveToNext()) {
                if (cursor.getString(0)!=null){
                    Map<String, String> map = new HashMap<>();
                    map.put("Area",cursor.getString(0));
                    data.add(map);
//                    System.out.println("AREA=" + cursor.getString(0));
                }
            }
        }
        cursor.close();
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
    private void initViews() {
        isUpdate=0;
        layout_title = (LinearLayout) rootView.findViewById(R.id.layout_title);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        layout_device = (LinearLayout) rootView.findViewById(R.id.layout_device);
        manager = new GridLayoutManager(getActivity(),3);
        adapter = new ControlMainAdapter(data,getActivity());
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    private void setListener() {
        layout_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), DeviceManagerActivity.class));
            }
        });
        adapter.setmOnItemClickListener(new ControlMainAdapter.onItemClickListener() {
            @Override
            public void onClick(int position) {
                Intent intent = new Intent();
                if (position==0){
                    intent.putExtra("Area","defaultRoom");
                    intent.setClass(getActivity(), DefaultRoomActivity.class);
                    startActivity(intent);
                }else {
                    intent.putExtra("Area",data.get(position-1).get("Area"));
                    intent.setClass(getActivity(), RoomControlActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onLongClick(View v, int position) {

            }
        });
        layout_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 1000)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View view = View.inflate(getActivity(),R.layout.add_inpput,null);
                    final EditText ed1 = (EditText) view.findViewById(R.id.name);
                    final EditText add = (EditText) view.findViewById(R.id.addr);
                    builder.setView(view);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MyApplication.getSharedPreferences().edit().putString("sixSensorIp",ed1.getText().toString()).commit();
                            MyApplication.getSharedPreferences().edit().putInt("sixSensorPort",Integer.valueOf(add.getText().toString())).commit();
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
        });
    }

    private long[] mHits = new long[5];

    @Override
    public void onResume() {
        super.onResume();
        if (adapter!=null&&isUpdate==1){
            System.out.println("update==>被执行了");
            initData();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        isUpdate=1;
        super.onStop();
    }
}
