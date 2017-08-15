package com.cxm;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 与jira通信获取或设置数据
 * @author chenximeng
 * @version 1.0
 */
public class JiraAPI {

    public enum METHOD
    {
        POST, GET, PUT, DELETE
    }
    private static JiraAPI jiraAPI;
    private String preUrl;
    private Map<String, String> fieldMap;
    private CloseableHttpClient httpClient;

    private JiraAPI() {
    }

    public static JiraAPI getJiraAPI() {
        if (jiraAPI == null)
        {
            jiraAPI = new JiraAPI();
        }
        return jiraAPI;
    }

    public void setPreUrl(String preUrl) {
        this.preUrl = preUrl;
    }

    /**
     * 开启连接对象
     * @param preUrl url域名
     */
    public void openConnection(String preUrl)
    {
        if (httpClient == null)
        {
            this.httpClient = HttpClients.createDefault();
        }
        setPreUrl(preUrl);
    }

    /**
     * 关闭连接对象
     * @throws java.io.IOException
     */
    public void closeConnection() throws IOException
    {
        if (this.httpClient != null)
        {
            this.httpClient.close();
            this.httpClient = null;
        }
    }

    /**
     * 登陆Jira
     * @param loginMap 登陆所需信息
     * @return 登陆结果{返回码，返回信息}
     */
    public String[] login(Map<String, String> loginMap)
    {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("username", loginMap.get("username"));
        jsonParam.put("password", loginMap.get("password"));
        String[] res = sendRequest(METHOD.POST, this.preUrl + "/rest/auth/1/session", jsonParam.toString());
        return res;
    }

    /**
     * 获取现有的url列表
     * @return url列表{issueUrl, issueKey}
     */
    public Map<String, String> getExistingUrl()
    {
        String[] res = sendRequest(METHOD.GET, this.preUrl + "/rest/api/2/issue/URL-2", "");
        HashMap<String, String> existingURLMap = new HashMap<>();
        if (res[0].equals("200"))
        {
            String existingURLIssue = res[1];
            JSONObject existingFieldObj = JSONObject.fromString(existingURLIssue);
            String existingFieldStr = existingFieldObj.get("fields").toString();
            JSONObject existingURLObj = JSONObject.fromString(existingFieldStr);
            String existingURLStr = existingURLObj.get(this.fieldMap.get("response body")).toString();
            JSONArray existingURLArr = null;
            if (!existingURLStr.trim().equals(""))
            {
                existingURLArr = JSONArray.fromString(existingURLStr);
                for (int i = 0; i < existingURLArr.length(); i++)
                {
                    JSONObject url = JSONObject.fromString(existingURLArr.get(i).toString());
                    existingURLMap.put(url.get("url").toString(), url.get("key").toString());
                }
            }
        }
        return existingURLMap;
    }

    /**
     * 初始化各field的id
     * @return field{name, id}
     */
    public void setFieldMap()
    {
        this.fieldMap = new HashMap<>();
        String[] res = sendRequest(METHOD.GET, this.preUrl + "/rest/api/2/field", "");
        if (res[0].equals("200"))
        {
            JSONArray fieldArr = JSONArray.fromString(res[1]);
            for (int i = 0; i < fieldArr.length(); i++)
            {
                JSONObject fieldObj = JSONObject.fromString(fieldArr.get(i).toString());
                this.fieldMap.put(fieldObj.get("name").toString(), fieldObj.get("id").toString());
            }
        }
    }

    /**
     * 批量创建issue
     * @param apiMaps issue信息列表
     * @return 创建结果{返回码，返回信息}
     */
    public String[] createAllIssues(List<Map<String, String>> apiMaps)
    {
        JSONObject jsonParam = new JSONObject();
        JSONArray issueUpdates = new JSONArray();
        for (int i = 0; i < apiMaps.size(); i++)
        {
            JSONObject fieldsArr = new JSONObject();
            fieldsArr.put("fields", getFields(apiMaps.get(i)));
            issueUpdates.put(fieldsArr);
        }
        jsonParam.put("issueUpdates", issueUpdates);
        String[] issueRes = sendRequest(METHOD.POST, this.preUrl + "/rest/api/2/issue/bulk", jsonParam.toString());
        return issueRes;
    }

    /**
     * 更新已上传的url列表
     * @param newUrl
     * @return
     */
    public String[] updateUrl(String newUrl)
    {
        JSONObject jsonParam = new JSONObject();
        JSONObject fields = getFieldsForURL(newUrl);
        jsonParam.put("fields", fields);
        String[] issueRes = sendRequest(METHOD.PUT, this.preUrl + "/rest/api/2/issue/URL-2", jsonParam.toString());
        return issueRes;
    }

    /**
     * 更新单个issue
     * @param updAPIMaps
     * @return 更新结果集[{返回码，返回信息}]
     */
    public Set<String[]> updateAllIssues(List<Map<String, String>> updAPIMaps)
    {
        Set<String[]> res = new HashSet<>();
        for (int i = 0; i < updAPIMaps.size(); i++)
        {
            JSONObject jsonParam = new JSONObject();
            JSONObject fields = getFields(updAPIMaps.get(i));
            jsonParam.put("fields", fields);
            String[] updRes = sendRequest(METHOD.PUT, this.preUrl + "/rest/api/2/issue/" + updAPIMaps.get(i).get("key"), jsonParam.toString());
            res.add(updRes);
        }
        return res;
    }

    /**
     * 获取单个issue里面的字段
     * @param apiMap 字段映射表
     * @return JSONObject Json对象
     */
    private JSONObject getFields(Map<String, String> apiMap)
    {
        JSONObject fields = new JSONObject();

        JSONObject projectObj = new JSONObject();
//        projectObj.put("key", apiMap.get("projectKey"));
        projectObj.put("key", "API");
        fields.put("project", projectObj);

        JSONObject issuetype = new JSONObject();
        issuetype.put("name", "API");
        fields.put("issuetype", issuetype);

        fields.put("summary", apiMap.get("summary"));
        fields.put(this.fieldMap.get("API版本"), apiMap.get("version"));
        fields.put(this.fieldMap.get("url"), apiMap.get("url"));

        JSONObject option = new JSONObject();
        option.put("value", apiMap.get("method"));
        fields.put(this.fieldMap.get("Method"), option);

        option = new JSONObject();
        option.put("value", apiMap.get("contentType"));
        fields.put(this.fieldMap.get("Content-Type"), option);

        fields.put(this.fieldMap.get("headers"), apiMap.get("header"));
        fields.put(this.fieldMap.get("request body"), apiMap.get("request"));

        option = new JSONObject();
        option.put("value", apiMap.get("accept"));
        fields.put(this.fieldMap.get("Accept"), option);

        fields.put(this.fieldMap.get("response body"), apiMap.get("response"));
        fields.put("description", apiMap.get("description"));
        return fields;
    }

    /**
     * 获取单个issue里面的字段来更新URL list
     * @param urls 字段映射表
     * @return JSONObject Json对象
     */
    private JSONObject getFieldsForURL(String urls)
    {
        JSONObject fields = new JSONObject();

        JSONObject projectObj = new JSONObject();
        projectObj.put("key", "URL");
        fields.put("project", projectObj);

        JSONObject issuetype = new JSONObject();
        issuetype.put("name", "URL");
        fields.put("issuetype", issuetype);

        fields.put("summary", "API");
        fields.put(this.fieldMap.get("API版本"), " ");
        fields.put("customfield_10022", " ");

        JSONObject option = new JSONObject();
        option.put("value", "POST");
        fields.put("customfield_10016", option);

        fields.put("customfield_10019", " ");

        option = new JSONObject();
        option.put("value", "application/json");
        fields.put("customfield_10018", option);

        fields.put("customfield_10021", "\"" + urls + "\"");
        return fields;
    }

    /**
     * 准备发送请求需要的数据和处理返回结果
     * @param method 请求类型的枚举类型
     * @param url 目标的url
     * @param data json形式的字段
     * @return result {状态码, jira返回的json串}
     */
    public String[] sendRequest(METHOD method, String url, String data)
    {
        System.out.println("*************************************************");
        System.out.println("Request:" + data);
        //设置数据为utf-8编码
        StringEntity entity = new StringEntity(data, "utf-8");
        //设置请求编码
        entity.setContentEncoding("utf-8");
        //设置请求类型
        entity.setContentType("application/json");
        //执行请求
        CloseableHttpResponse httpResponse = null;
        StringBuilder stringBuilder = new StringBuilder();
        try
        {
            switch (method.toString())
            {
                case "POST":
                    HttpPost post = new HttpPost(url);
                    post.setEntity(entity);
                    System.out.println("POST 请求...." + post.getURI());
                    httpResponse = httpClient.execute(post);
                    break;
                case "GET":
                    HttpGet get = new HttpGet(url);
                    System.out.println("GET 请求...." + get.getURI());
                    httpResponse = httpClient.execute(get);
                    break;
                case "DELETE":
                    HttpDelete delete = new HttpDelete(url);
                    System.out.println("DELETE 请求...." + delete.getURI());
                    httpResponse = httpClient.execute(delete);
                    break;
                case "PUT":
                    HttpPut put = new HttpPut(url);
                    put.setEntity(entity);
                    System.out.println("PUT 请求...." + put.getURI());
                    httpResponse = httpClient.execute(put);
                    break;
                default:
                    break;
            }
            HttpEntity httpEntity = httpResponse.getEntity();
            if (null != httpEntity)
            {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"), 8 * 1024);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
            System.out.println("status code: " + httpResponse.getStatusLine().getStatusCode());
            System.out.println(stringBuilder.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new String[]{"400"};
        }
        finally
        {
            try
            {
                httpResponse.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new String[]{httpResponse.getStatusLine().getStatusCode() + "", stringBuilder.toString()};
        }
    }
}
