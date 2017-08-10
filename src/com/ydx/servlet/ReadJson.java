package com.ydx.servlet;  
  
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

  
public class ReadJson {  
  
    /** 
     * @param args 
     */  
    public static void main(String[] args) {  
        // 读取nameID.txt文件中的NAMEID字段（key）对应值（value）并存储  
        ArrayList<String> list = new ArrayList<String>();  
        // 读取原始json文件并进行操作和输出  
        try {  
            BufferedReader br = new BufferedReader(new FileReader(  
                    "src/com/ydx/servlet/data.json"));// 读取原始json文件  
            String s = null, ws = null;  
            while ((s = br.readLine()) != null) {  
                // System.out.println(s);  
                try {  
                	JSONObject jsonObject = JSONObject.fromObject(queryJson);
                    ws = dataJson.toString();  
                    System.out.println(ws);  
                } catch (JSONException e) {  
                    // TODO Auto-generated catch block  
                    e.printStackTrace();  
                }  
            }  
            br.close();
  
        } catch (IOException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
  
    }  
  
}