package Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.suntrans.wanhua.R;

import java.util.ArrayList;
import java.util.Map;

import convert.Converts;
import views.Switch;

import static android.R.attr.data;

/**
 * Created by Looney on 2016/10/20.
 */

public class SwitchDetailAdapter extends RecyclerView.Adapter {


    private  Context context;
    private  ArrayList<Map<String, Object>> datas;

    public SwitchDetailAdapter(ArrayList<Map<String, Object>> datas, Context context) {
        this.datas = datas;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder= new ViewHolder1(LayoutInflater.from(context)
                .inflate(R.layout.item_switchadapter, parent,false));
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder1)(holder)).setData(position);
        ((ViewHolder1)(holder)).setListener(position);
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    class ViewHolder1 extends RecyclerView.ViewHolder{
        Switch aSwitch;
        TextView name;
        TextView area;
        TextView name_title;
        ImageView imageView;
        public ViewHolder1(View itemView) {
            super(itemView);
            aSwitch = (Switch) itemView.findViewById(R.id._switch);
            name = (TextView) itemView.findViewById(R.id.name);
            name_title = (TextView) itemView.findViewById(R.id.title_name);
            area = (TextView) itemView.findViewById(R.id.area);
            imageView = (ImageView) itemView.findViewById(R.id.iv_icon);
        }
        public void setData(int position){
            if (datas.get(position).get("Image")!=null){
                Bitmap bitmap = (Bitmap) datas.get(position).get("Image");
                bitmap = Converts.toRoundCorner(bitmap,Converts.dip2px(context,10));
                imageView.setImageBitmap(bitmap);
            }else {
                imageView.setImageResource(R.drawable.ic_bulb_off);
            }
            if (position==0){
                name_title.setText("总开关:");
                name.setText("总开关");
                area.setText(datas.get(position).get("Area")+"");
                aSwitch.setState(datas.get(position).get("State").equals("0")?false:true);
            }else {
                name_title.setText("通道"+datas.get(position).get("Channel")+":");
                name.setText(datas.get(position).get("Name")+"");
                area.setText(datas.get(position).get("Area")+"");
                aSwitch.setState(datas.get(position).get("State").equals("0")?false:true);
            }
        }
        public void setListener(final int position){
            aSwitch.setOnChangeListener(new Switch.OnSwitchChangedListener() {
                @Override
                public void onSwitchChange(Switch switchView, boolean isChecked) {
                    mOnClickListener.onSwitchClick(switchView,position,isChecked);
                }
            });
            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.onNameClick(v,position);
                }
            });
            area.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.onAreaClick(v,position);
                }
            });
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.onPicClick(v,position);
                }
            });
        }
    }

    public void SetOnItemClickListener(OnItemClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    private  OnItemClickListener mOnClickListener;
    public interface OnItemClickListener{
        void onSwitchClick(View v,int position,boolean isChecked);
        void onNameClick(View v,int position);
        void onAreaClick(View v,int position);
        void onPicClick(View v,int position);
    }
}
