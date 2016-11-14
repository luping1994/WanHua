package views;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import com.suntrans.wanhua.R;


/**
 * Created by Looney on 2016/9/26.
 */

public class BottomMentTab extends TabLayout {
    public static final int[] tabIcon_gray = new int[]{
            R.mipmap.ic_powercontrol,
            R.mipmap.ic_envir,
            };

    public static final int[] tabIcon_bule = new int[]{
            R.mipmap.ic_powercontrol,
            R.mipmap.ic_envir,
            };
    private static final String[] tabTitle = new String[]{"能源管控", "室内环境"};
    private static final int NUM_TAD = 2;

    public BottomMentTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public BottomMentTab(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public BottomMentTab(Context context) {
        super(context);
        init();
    }

    @Override
    public void setOnTabSelectedListener(OnTabSelectedListener onTabSelectedListener) {
        super.setOnTabSelectedListener(onTabSelectedListener);
    }

    private void init() {

        for (int i = 0; i < NUM_TAD; i++) {
            if (i == 0) {
                addTab(newTab().setText(tabTitle[i]).setIcon(tabIcon_bule[i]));
            } else {
                addTab(newTab().setText(tabTitle[i]).setIcon(tabIcon_gray[i]));
            }
        }
    }
}
