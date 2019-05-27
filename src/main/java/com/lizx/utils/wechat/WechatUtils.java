package com.lizx.utils;

public class WechatUtils {

    public static String appid = "wx4638fe338dcb7fe2";
    public static String secret = "69ad4bd341767e9da85e4359f2ba248d";

    //获取access_token的url
    public static String access_token_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";

    //改版按钮的url
    public static String change_menu_url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN";

}
