package com.lizx.controller;


import com.lizx.pojo.message.Event;
import com.lizx.pojo.message.TextImgMessage;
import com.lizx.pojo.message.TextMessage;
import com.lizx.utils.wechat.XMLUtil;
import org.jdom2.JDOMException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("wechatHandler")
public class WechatHandler {


    /**
     * 微信接入
     * @return
     * @throws IOException
     */
    @RequestMapping(value="/connect",method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public void connectWeixin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 将请求、响应的编码均设置为UTF-8（防止中文乱码）
        request.setCharacterEncoding("UTF-8");  //微信服务器POST消息时用的是UTF-8编码，在接收时也要用同样的编码，否则中文会乱码；
        response.setCharacterEncoding("UTF-8"); //在响应消息（回复消息给用户）时，也将编码方式设置为UTF-8，原理同上；
        boolean isGet = request.getMethod().toLowerCase().equals("get");
        PrintWriter out = response.getWriter();

        try {
            if (isGet) {
                System.out.println("getgetget");
                String signature = request.getParameter("signature");// 微信加密签名
                String timestamp = request.getParameter("timestamp");// 时间戳
                String nonce = request.getParameter("nonce");// 随机数
                String echostr = request.getParameter("echostr");//随机字符串

                // 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败  if (SignUtil.checkSignature(DNBX_TOKEN, signature, timestamp, nonce)) {
//                LOGGER.info("Connect the weixin server is successful.");
                response.getWriter().write(echostr);
            } else {
//                LOGGER.error("Failed to verify the signature!");
            }
        }catch (Exception e) {
        }
        System.out.println("22222222222222222222222222222222222222");

        boolean isPost = request.getMethod().toLowerCase().equals("post");

        if(isPost) {
            //用户发送了一条消息或关注了我的公众号，都会发一条消息，消息是流形式
            InputStream inStream = request.getInputStream();
            //把消息流转换成文本类型
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            String result = new String(outSteam.toByteArray(), "UTF-8");

            System.out.println("result==="+result);

            //SAX解析xml
            Map<String, String> map = null;
            //通过sax解析xml技术，把xml文本解析成map对象
            try {
                map = XMLUtil.doXMLParse(result);
            } catch (JDOMException e) {
                e.printStackTrace();
            }

            if(map.get("MsgType").equals("text")){

                //在redis中放着一些数据
                String compareStr = ""; //redis.get("");

                if(map.get("Content").equals(compareStr)){

                    returnTextMsg(response,"你好",map);
                }

                if(map.get("Content").equals("吃饭了吗")){
                    returnTextMsg(response,"吃了",map);
                }

                returnTextMsg(response,"您发送的消息无法识别！！！",map);
            }

            if(map.get("MsgType").equals("news")){

                //先从redis当中查询出一个list数据
                List<Map<String,Object>> list = null; //从redis当中拿一个list数据

                if(map.get("Content").equals("查询得奖结果")){
//                    returnNewsMsg(response,list,map);
                     sendNewsMsg(response,map);
                }

            }

            System.out.println("=======================================");
        }


    }




    public void returnTextMsg(HttpServletResponse response,String text,Map<String,String> map){

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<xml>");
        stringBuffer.append("<ToUserName><![CDATA["+map.get("fromUser")+"]]></ToUserName>");
        stringBuffer.append("<FromUserName><![CDATA["+map.get("toUser")+"]]></FromUserName>");
        stringBuffer.append("<CreateTime>"+System.currentTimeMillis()+"</CreateTime>");
        stringBuffer.append("<MsgType><![CDATA[text]]></MsgType>");
        stringBuffer.append("<Content><![CDATA["+text+"]]></Content>");
        stringBuffer.append("</xml>");
        try {
            response.getWriter().write(stringBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendTextMsg(HttpServletResponse response,String text,Map<String,String> map){

        TextMessage textMessage = new TextMessage();
        textMessage.setFromUserName(map.get("toUser"));
        textMessage.setToUserName(map.get("fromUser"));
        textMessage.setCreateTime(System.currentTimeMillis());
        textMessage.setMsgType(Event.TEXT);
        textMessage.setContent(text);
        try {
            response.getWriter().write(textMessage.toXML());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 回复的是图文消息
     * @param response
     * @param paramList
     * @param paramMap
     */
    public void returnNewsMsg(HttpServletResponse response, List<Map<String, Object>> paramList, Map<String, String> paramMap){
        TextImgMessage textImgMessage = new TextImgMessage();
        //基础数据
        textImgMessage.setFromUserName(paramMap.get("toUser"));//发送给给微信公众号发消息的人
        textImgMessage.setToUserName(paramMap.get("fromUser"));//微信公众号的id
        textImgMessage.setMsgType(Event.NEWS);//消息的类型，类型是图文消息
        textImgMessage.setCreateTime(System.currentTimeMillis());//回复的时间戳

        //这个list是用于拼接数据的
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();

        //从redis当中查询的list数据，用户发送一个固定的文字，我给用户回复固定好的list数据
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> stringObjectMap = list.get(i);

            Map<String,Object> map = new HashMap<String,Object>();
            map.put("Title", stringObjectMap.get("Title"));
            map.put("Description", stringObjectMap.get("Description"));
            map.put("PicUrl", stringObjectMap.get("PicUrl"));
            map.put("Url", stringObjectMap.get("Url"));
            list.add(map);
        }
        //把拼接好的图文消息放入到list中
        textImgMessage.setList(list);

        try {
            //返回给微信，微信会转发到用户端的公众号里
            response.getWriter().write(textImgMessage.toXML());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendNewsMsg(HttpServletResponse response, Map<String,String> paramMap) {
        TextImgMessage textImgMessage = new TextImgMessage();
        //基础数据
        textImgMessage.setFromUserName(paramMap.get("toUser"));//发送给给微信公众号发消息的人
        textImgMessage.setToUserName(paramMap.get("fromUser"));//微信公众号的id
        textImgMessage.setMsgType(Event.NEWS);//消息的类型，类型是图文消息
        textImgMessage.setCreateTime(System.currentTimeMillis());//回复的时间戳

        //这个list是用于拼接数据的
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();


        Map<String,Object> map = new HashMap<String,Object>();
        map.put("Title", "得奖结果");
        map.put("Description","得奖结果");
        map.put("PicUrl", "http://k.zol-img.com.cn/sjbbs/7692/a7691515_s.jpg");
        map.put("Url", "http://www.jd.com");
        list.add(map);

        Map<String,Object> map2 = new HashMap<String,Object>();
        map2.put("Title", "得奖结果");
        map2.put("Description","得奖结果");
        map2.put("PicUrl", "http://pic9.nipic.com/20100923/2531170_140325352643_2.jpg");
        map2.put("Url", "http://www.baidu.com");
        list.add(map2);

        //把拼接好的图文消息放入到list中
        textImgMessage.setList(list);

        try {
            //返回给微信，微信会转发到用户端的公众号里
            response.getWriter().write(textImgMessage.toXML());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
