package com.ydx.servlet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SaveDataServlet extends HttpServlet {

	/**
	 * Constructor of the object.
	 */
	public SaveDataServlet() {
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
		// Put your code here
	}

	/**
	 * The doGet method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		doPost(request, response);
	}

	/**
	 * The doPost method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to
	 * post.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		InputStream is = null;

		HttpClient client = new HttpClient();

		PostMethod method = new PostMethod(
				"http://yingyan.baidu.com/api/v3/track/addpoints");
		
		BufferedReader brname = new BufferedReader(new FileReader("src/json/nameID.txt"));// 读取NAMEID对应值  

		NameValuePair[] data = {

				// 设置表单元素，和填值
				new NameValuePair("ak", "Lastname++"),
				new NameValuePair("service_id", "Firstname++"),
				new NameValuePair("point_list", "Firstname++"),

		};
		// 将表单的值放入postMethod中
		method.setRequestBody(data);

		try {

			client.executeMethod(method);

			is = method.getResponseBodyAsStream();

			Document document = Jsoup.parse(is, "gbk", "");

			System.err.println(document);

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			method.releaseConnection();

			try {

				if (is != null) {

					is.close();

				}

			} catch (IOException e) {

				e.printStackTrace();

			}

		}
	}

	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             if an error occurs
	 */
	public void init() throws ServletException {
		// Put your code here
	}

}
