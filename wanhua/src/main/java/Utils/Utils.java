package Utils;

import android.widget.Toast;

import com.suntrans.wanhua.MyApplication;

/**
 * Created by Looney on 2016/10/20.
 */

public class Utils {

    public static String DecConvert2Hex(String a) {
        int b = Integer.valueOf(a);
        String c = Integer.toHexString(b);
        System.err.println("a=" + a + ";b=" + b + ";c=" + c);
        StringBuffer sb;
        sb = new StringBuffer();
        if (c.length()>4){
            return  null;
        }
        if (c.length() != 4) {
            for (int i = 0; i < 4 - c.length(); i++) {
                sb.append(0);
            }
        }
        sb.append(c);

        return sb.toString();
    }

    public static  boolean checkNameAndAddr(String name, String addr) {
        String expression ="[^\\s]{1,}";
        String expression2 ="[^\\s]{1,}";
        boolean a = addr.matches(expression2);
        boolean b = name.matches(expression);
        if (!a&&b){
            Toast.makeText(MyApplication.getApplication1(),"地址格式不对",Toast.LENGTH_SHORT).show();
        }
        if (a&&!b){
            Toast.makeText(MyApplication.getApplication1(),"名称为空或含有空格",Toast.LENGTH_SHORT).show();
        }
        if (!a&&!b){
            Toast.makeText(MyApplication.getApplication1(),"名称为空,地址格式不对",Toast.LENGTH_SHORT).show();
        }
        return a&&b;
    }

    public static  boolean checkNameAndAddr2(String addr) {
        String expression ="[^\\s]{1,}";
        String expression2 ="[^\\s]{8,}";
        boolean a = addr.matches(expression2);
        if (!a){
            Toast.makeText(MyApplication.getApplication1(),"地址格式不对",Toast.LENGTH_SHORT).show();
        }
        return a;
    }
}
