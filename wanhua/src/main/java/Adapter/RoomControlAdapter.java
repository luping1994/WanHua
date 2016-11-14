package Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.suntrans.wanhua.R;
import java.util.ArrayList;
import java.util.Map;

import convert.Converts;

/**
 * Created by Looney on 2016/9/18.
 */
public class RoomControlAdapter extends  RecyclerView.Adapter <RoomControlAdapter.MyViewHolder>{

//    private  SmartSwitch datas;
    private  Context context;
    private  int[] imageId = {R.drawable.ic_bulb_off,R.drawable.ic_bulb_on,
                                R.drawable.ic_dot_off,R.drawable.ic_dot_on};
    private  OnItemClickListener mOnItemClickListener;
    private ArrayList<Map<String,Object>> datas;


    public void setOnItemClickListener(OnItemClickListener listener ) {
        mOnItemClickListener =listener;
    }
    public interface OnItemClickListener {
        void onClick(View view, int position);
        void onLongClick(View view,int position);
    }
    Bitmap bitmap_off;
    Bitmap bitmap_on;
    Bitmap dot_off;
    Bitmap dot_on;
    public RoomControlAdapter(Context context, ArrayList<Map<String,Object>> datas){
         bitmap_off = BitmapFactory.decodeResource(context.getResources(),imageId[0]);
         bitmap_on = BitmapFactory.decodeResource(context.getResources(),imageId[1]);
         dot_off = BitmapFactory.decodeResource(context.getResources(),imageId[2]);
         dot_on = BitmapFactory.decodeResource(context.getResources(),imageId[3]);
        this.datas=datas;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.road_bulb_item, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.setData(position);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onClick(v,holder.getPosition());
            }
        });
        holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOnItemClickListener.onLongClick(v,position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }
    class  MyViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView ;
        ImageView dot ;
        TextView textView;
        public MyViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.iv);
            textView = (TextView) itemView.findViewById(R.id.name);
            dot = (ImageView) itemView.findViewById(R.id.dot);
        }
        public void setData(int position){
            Bitmap bitmap = (Bitmap) datas.get(position).get("Image");
            if (bitmap!=null){
                bitmap = Converts.toRoundCorner(bitmap,Converts.dip2px(context,10));
                imageView.setImageBitmap(bitmap);
            }else {
                imageView.setImageResource(R.drawable.ic_bulb_off);
            }
           if (datas.get(position).get("Name")==null){
               if (datas.get(position).get("Channel").equals("0")){
                   textView.setText("总开关"+"("+datas.get(position).get("RSAddr")+")");
                   dot.setImageBitmap(dot_on);
               }else {
                   textView.setText("通道"+datas.get(position).get("Channel")+"("+datas.get(position).get("RSAddr")+")");
                   dot.setImageBitmap(TextUtils.equals((String)datas.get(position).get("State"),"0")?dot_off:dot_on);
               }
           }else {
               if (datas.get(position).get("Channel").equals("0")){
                   textView.setText("总开关"+"("+datas.get(position).get("RSAddr")+")");
                   dot.setImageBitmap(dot_on);
               }else {
                   textView.setText(datas.get(position).get("Name")+"("+datas.get(position).get("RSAddr")+")");
                   dot.setImageBitmap(TextUtils.equals((String)datas.get(position).get("State"),"0")?dot_off:dot_on);
               }
           }
        }
    }
}
