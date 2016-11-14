package Adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.suntrans.wanhua.R;

import java.util.ArrayList;
import java.util.Map;

import static com.suntrans.wanhua.R.layout.warning;

/**
 * Created by Looney on 2016/10/17.
 */

public class ControlMainAdapter extends RecyclerView.Adapter {
    private  Context context;
    private  ArrayList<Map<String, String>> data;
    private  onItemClickListener mOnItemClickListener;
    public ControlMainAdapter(ArrayList<Map<String, String>> data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder holder= new viewHolder1(LayoutInflater.from(context)
                .inflate(R.layout.mianlist_item, parent,false));
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ((viewHolder1)holder).imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onClick(position);
            }
        });
        ((viewHolder1)holder).imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOnItemClickListener.onLongClick(v,position);
                return true;
            }
        });
        ((viewHolder1)holder).setData(position);
    }



    @Override
    public int getItemCount() {
        return data.size()==0?1:data.size()+1;
    }

    class viewHolder1 extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView textView;
        public viewHolder1(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.iv);
            textView = (TextView) itemView.findViewById(R.id.tv);
        }

        public void setData(int position) {
            if (position==0){
                textView.setText("未分类");
                imageView.setImageResource(R.drawable.ic_room1);
            }else {
                textView.setText(data.get(position-1).get("Area"));
                imageView.setImageResource(R.drawable.ic_room1);
            }
        }
    }

    public interface onItemClickListener{
        void onClick(int position);

        void onLongClick(View v, int position);
    }
    public void setmOnItemClickListener(onItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
