package com.ydx.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class FetchTest {
	public String getTokenInfo(String url, String param) {
		String result = "";
		BufferedReader in = null;
		String urlNameString = url + "?" + param;
		try {
			URL realUrl = new URL(urlNameString);
			URLConnection connection = realUrl.openConnection(); 
			connection.connect();
			in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
			String line;
			while (null != (line = in.readLine())) {
				result += line;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public static void main(String[] args) {
		String url = "https://sdwatch.aisportage.com/api/auth/token";
		String param = "accessKey=940540323879472fdced1563d933b914";
		FetchTest httpReq = new FetchTest();
		System.out.println(httpReq.getTokenInfo(url, param));
	}
}
