package com.forestar.service;

import groovy.ui.Console;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.xfire.client.Client;
import org.springframework.beans.factory.annotation.Autowired;

import com.forestar.core.spring.SpringBeanFactory;
import com.forestar.utils.DynamicClientUtil;
import com.forestar.utils.JsonUtil;
import com.forestar.utils.PropertiesUtil;
import com.forestar.utils.XmlToJson;

import data.common.exception.ServiceException;
import data.general.QueryFilter;
import data.general.RowBase;
import data.general.UpdateFilter;
import data.service.BaseDataService;

public class logininfoService extends HttpServlet {
	public static String url;
	public static String password;
	@Autowired(required = false)
	private BaseDataService dataService = (BaseDataService) SpringBeanFactory
			.getBean("baseDataService2");

	public logininfoService() {
		super();
	}

	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");
		Date start = new Date();
		getDataFromWS();
		Date end = new Date();
		System.out.println("同步共花費時間:" + (end.getTime() - start.getTime()));
		//返回到前台
		response.getWriter().print("sucess");
	}
	/**
	 * 从政务内网的接口中取数据
	 */
	private void getDataFromWS()
	{
		url = PropertiesUtil.getValue("wsdlUrl");
		password = PropertiesUtil.getValue("password");
		String ProgramID = PropertiesUtil.getValue("ProgramID");
		Client client = null;
		String xml = null;
		try {
			client = new Client(new URL(url));
			Object[] result = client.invoke("Get_RootRecords", new Object[]{ProgramID});
			xml=result[0].toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//更新节点数据到组织机构表与用户表
		get_saveData(xml, ProgramID);
		//更新父子关系，判断条件parid为空的
		update_pid();
	}
	
	/**
	 * 保存数据
	 * @param xml
	 * @param ProgramID
	 */
	private void get_saveData(String xml, String ProgramID) {
		//报错返回
		if (xml.contains("Err")) {
			return;
		}

		List<Map<String, Object>> listMaps = new ArrayList<Map<String, Object>>();
		String json_result = null;
		//保存用户row
		List<RowBase> listUserRow = null;
		//保存组织机构row
		List<RowBase> listOrgRow = null;
		try {
			json_result = XmlToJson.xml2String(xml);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (null != json_result && !json_result.isEmpty())
			listMaps = JsonUtil.stringToJsonArray(JsonUtil.stringToJson(json_result).getString("User"));
		try {
			//同时取得用户与组织机构数组【用户对象list，组织机构对象list】
			Object[] user_org_Array=this.getRowBases(listMaps);
			listUserRow=(List<RowBase>) user_org_Array[0];
			listOrgRow = (List<RowBase>) user_org_Array[1];
			//取得需要保存的用户 暂时不用
			//listUserRow = getUserRowBases(listMaps);
			//取得需要保存的组织机构 暂时不用
			//listOrgRow = getOrgRowBases(listMaps);
			if(null!=listUserRow&&listUserRow.size()>0)
			{
				dataService.saveList("FS_PT_YW_BASE_USER", listUserRow);
			}

			if(null!=listOrgRow&&listOrgRow.size()>0)
			{
				dataService.saveList("FS_PT_YW_BASE_ORG", listOrgRow);
			}
			//更新父节点的值,换用新的方式，这种太慢
		//	if(null!=listOrgRow&&null!=listUserRow)
		//	{
			//	setORGPid(listMaps);
			//	setUserOrgID(listMaps);
			//}
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		//循环子节点
		for (Map<String, Object> tepmmap : listMaps) {
			Client client = null;
			String tepmxml = null;
			try {
				client = new Client(new URL(url));
				Object[] result1 = client.invoke("Get_LowerRecords", new Object[]{ProgramID,tepmmap.get("ID")});
				tepmxml=result1[0].toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			//取子节点的数据
			get_saveData(tepmxml, ProgramID);
		}
	}
	
	/**
	 * 更新组织机构与用户的pid
	 */
	private void update_pid() {

			int update_num = 0;
			try {
				//更新除了根节点外的数据父节点
				update_num = dataService.executeSql("FS_PT_YW_BASE_ORG", "update FS_PT_YW_BASE_ORG T set T.I_ORGPID=(select T1.I_ORGID from FS_PT_YW_BASE_ORG_V T1 where T.C_GUID_P=T1.C_GUID) WHERE T.I_ORGPID IS NULL");
				//更新根节点外的父节点的i_orgpid=0
				update_num+=dataService.executeSql("FS_PT_YW_BASE_ORG", "update FS_PT_YW_BASE_ORG T set T.I_ORGPID=0 WHERE T.I_ORGPID IS NULL AND T.C_ORGNAME='湖南省林业系统'");
				update_num+=dataService.executeSql("FS_PT_YW_BASE_USER", "update FS_PT_YW_BASE_USER T set T.I_ORGID=(select T1.I_ORGID from FS_PT_YW_BASE_ORG T1 where T.C_GUID_P=T1.C_GUID)");
				System.out.println("同步了"+update_num+"条用户及组织机构的父节点！");
			} catch (ServiceException e1) {
				e1.printStackTrace();
			}
		
	}
	
	

	/**
	 * 为orgid赋值
	 * 
	 * @param listMaps
	 * @return
	 */
	private void setORGPid(List<Map<String, Object>> listMaps) {
		for (Map<String, Object> map : listMaps) {
			String pid = map.get("ParentID").toString();
			QueryFilter queryFilter = new QueryFilter();
			queryFilter.setWhereString("C_GUID='" + pid + "'");
			List<RowBase> rows = null;
			try {
				rows = dataService.getEntityList("FS_PT_YW_BASE_ORG",
						queryFilter);
				if (null != rows && rows.size() <1) {
					continue;// 无数据，不添加。
				}
				String i_orgid = rows.get(0).getByFieldName("I_ORGID")
						.toString();
				UpdateFilter updateFilter = new UpdateFilter();
				updateFilter.setWhereString("C_GUID='" + map.get("ID") + "'");
				updateFilter.setSetFields("I_ORGPID='" + i_orgid + "'");
				dataService.update("FS_PT_YW_BASE_ORG", updateFilter);
			} catch (ServiceException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 为USER表中orgid赋值
	 * 
	 * @param listMaps
	 * @return
	 */
	private void setUserOrgID(List<Map<String, Object>> listMaps) {
		for (Map<String, Object> map : listMaps) {
			String id = map.get("ID").toString();
			String orgid = map.get("ParentID").toString();
			QueryFilter queryFilter = new QueryFilter();
			queryFilter.setWhereString("C_GUID='" + orgid + "'");
			List<RowBase> rows = null;
			try {
				rows = dataService.getEntityList("FS_PT_YW_BASE_ORG",
						queryFilter);
				if (null != rows && rows.size() <1) {
					continue;// 无数据，不添加。
				}
				String i_orgid = rows.get(0).getByFieldName("I_ORGID")
						.toString();
				UpdateFilter updateFilter = new UpdateFilter();
				updateFilter.setWhereString("C_GUID='" + id + "'");
				updateFilter.setSetFields("I_ORGID='" + i_orgid + "'");
				dataService.update("FS_PT_YW_BASE_USER", updateFilter);
			} catch (ServiceException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param listMaps
	 * @return
	 */
	private Object[] getRowBases(List<Map<String, Object>> listMaps) {
		List<RowBase> rowUserBases = new ArrayList<RowBase>();
		List<RowBase> rowOrgBases = new ArrayList<RowBase>();
		Object[] rtn_Array = new Object[] { rowUserBases, rowOrgBases };
		 int rows_size =0;
		 String name ="";
		for (Map<String, Object> map : listMaps) {
			String type = map.get("Type").toString();
			rows_size =0;
			name ="";
			if ("1".equals(type)) {
						name = map.get("Name").toString();
						//QueryFilter queryFilter = new QueryFilter();
						//List<RowBase> rows = null;
						try {
							//取得符合条件的数量
							 rows_size = dataService.executeSql("FS_PT_YW_BASE_ORG", "select * from FS_PT_YW_BASE_ORG where C_ORGNAME='" + name + "'");
							//判断是否是已经存在的帐号	
							 if (rows_size> 0) {
									continue;// 已保存，不添加。
								}
						} catch (ServiceException e) {
							e.printStackTrace();
							continue;
						}
						// 添加rowBase
						RowBase rowBase = new RowBase();
						rowBase.setByFieldName("I_ORGID", null);
						rowBase.setByFieldName("I_ORDER", map.get("Sort"));
						rowBase.setByFieldName("C_ZQCODE", map.get("Code"));
						rowBase.setByFieldName("C_ORGBH", null);// 主键自增
						rowBase.setByFieldName("C_ORGNAME", map.get("Name"));
						rowBase.setByFieldName("C_GUID", map.get("ID"));
						rowBase.setByFieldName("C_GUID_P", map.get("ParentID"));
						rowOrgBases.add(rowBase);
				} else if ("0".equals(type)) {
						name = map.get("Account").toString();
						//QueryFilter queryFilter = new QueryFilter();
						//List<RowBase> rows = null;
						try {
								rows_size = dataService.executeSql("FS_PT_YW_BASE_USER", "select * from FS_PT_YW_BASE_USER where C_USERNAME='" + name + "'");
								//判断是否是已经存在的帐号	
								 if (rows_size> 0) {
										continue;// 已保存，不添加。
									}
						} catch (ServiceException e) {
							e.printStackTrace();
							continue;
						}
						// 添加rowBase
						RowBase rowBase = new RowBase();
						rowBase.setByFieldName("I_USERID", null);// 主键自增
						rowBase.setByFieldName("C_USEREALNAME", map.get("Name"));
						rowBase.setByFieldName("C_USERNAME", map.get("Account"));
						rowBase.setByFieldName("I_ORDER", map.get("Sort"));
						rowBase.setByFieldName("C_ZQCODE", map.get("Code"));
						rowBase.setByFieldName("C_GUID", map.get("ID"));
						rowBase.setByFieldName("C_GUID_P", map.get("ParentID"));
						rowUserBases.add(rowBase);
			}
		}
		return rtn_Array;
	}

	/**
	 * 拼接用户 RowBases
	 * 
	 * @param listMaps
	 * @return
	 */
	private List<RowBase> getUserRowBases(List<Map<String, Object>> listMaps) {
		List<RowBase> rowBases = new ArrayList<RowBase>();
		for (Map<String, Object> map : listMaps) {
			String type = map.get("Type").toString();
			if (!"0".equals(type)) {
				continue;// 不是用户，不添加
			}
			String name = map.get("Account").toString();
			//QueryFilter queryFilter = new QueryFilter();
			//List<RowBase> rows = null;
			try {
				// 查询"C_USERNAME",如果数据库中已有则不添加。
				//queryFilter.setWhereString("C_USERNAME='" + name + "'");  
				//rows = dataService.getEntityList("FS_PT_YW_BASE_USER",queryFilter);
				//if (null != rows && rows.size() > 0) {
				//	continue;// 已保存，不添加。
				//}
				//取得符合条件的数量
			 int rows_size = dataService.executeSql("FS_PT_YW_BASE_USER", "select * from FS_PT_YW_BASE_USER where C_USERNAME='" + name + "'");
			//判断是否是已经存在的帐号	
			 if (rows_size> 0) {
					continue;// 已保存，不添加。
				}
				
			} catch (ServiceException e) {
				e.printStackTrace();
				continue;
			}
			// 添加rowBase
			RowBase rowBase = new RowBase();
			rowBase.setByFieldName("I_USERID", null);// 主键自增
			rowBase.setByFieldName("C_USEREALNAME", map.get("Name"));
			rowBase.setByFieldName("C_USERNAME", map.get("Account"));
			rowBase.setByFieldName("I_ORDER", map.get("Sort"));
			rowBase.setByFieldName("C_ZQCODE", map.get("Code"));
			rowBase.setByFieldName("C_GUID", map.get("ID"));
			rowBases.add(rowBase);
		}
		return rowBases;
	}

	/**
	 * 拼接机构 RowBases
	 * 
	 * @param listMaps
	 * @return
	 */
	private List<RowBase> getOrgRowBases(List<Map<String, Object>> listMaps) {
		List<RowBase> rowBases = new ArrayList<RowBase>();
		for (Map<String, Object> map : listMaps) {
			String type = map.get("Type").toString();
			if (!"1".equals(type)) {
				continue;// 不是组织机构，不添加
			}
			String name = map.get("Name").toString();
			QueryFilter queryFilter = new QueryFilter();
			//List<RowBase> rows = null;
			try {
				// 查询"C_USERNAME",如果数据库中已有则不添加。
				//queryFilter.setWhereString("C_ORGNAME='" + name + "'");
				//rows = dataService.getEntityList("FS_PT_YW_BASE_ORG",queryFilter);
				// if (null != rows && rows.size() > 0) {
				// continue;// 已保存，不添加。
				// }
				//取得符合条件的数量
				 int rows_size = dataService.executeSql("FS_PT_YW_BASE_ORG", "select * from FS_PT_YW_BASE_ORG where C_ORGNAME='" + name + "'");
				//判断是否是已经存在的帐号	
				 if (rows_size> 0) {
						continue;// 已保存，不添加。
					}
			} catch (ServiceException e) {
				e.printStackTrace();
				continue;
			}
			// 添加rowBase
			RowBase rowBase = new RowBase();
			rowBase.setByFieldName("I_ORGID", null);
			rowBase.setByFieldName("I_ORDER", map.get("Sort"));
			rowBase.setByFieldName("C_ZQCODE", map.get("Code"));
			rowBase.setByFieldName("C_ORGBH", null);// 主键自增
			rowBase.setByFieldName("C_ORGNAME", map.get("Name"));
			rowBase.setByFieldName("C_GUID", map.get("ID"));

			rowBases.add(rowBase);
		}
		return rowBases;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the POST method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	public void init() throws ServletException {
		// Put your code here
	}
}
