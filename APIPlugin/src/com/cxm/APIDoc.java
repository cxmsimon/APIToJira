package com.cxm;

/*
*
* FileName：APIDoc.java
*
* Description：根据目标源文件注释自动生成Jira API文档
*
* History：
* 版本号     作者         日期         简要介绍相关操作
* 1.0       chenximeng   2017.6.28    插件生成
*/
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiJavaFile;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * 插件显示模块、注解提取模块、请求发送模块
 * @author chenximeng
 * @version 1.0
 */
public class APIDoc extends AnAction
{
    private Project project;
    private List<Map<String, String>> apiMaps;
    private HashSet<String> failedList;
    private HashSet<String> projectList;
    private HashSet<PsiJavaFile> psiJavaFileSet;
    private List<Map<String, String>> updAPIMaps;
    private String resPath = "E:/result.txt";

    /**
     * 启动器
     * @param e 触发的动作
     * @return null
     */
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        this.project = e.getProject();
        this.psiJavaFileSet = new HashSet<>();
        this.apiMaps = new LinkedList<>();
        this.failedList = new HashSet<>();
        this.updAPIMaps = new LinkedList<>();
        this.projectList = new HashSet<>();
        //启动插件，生成界面元素
        if (UI.getUI().showUI() != 1)
        {
            return;
        }
        //选择目标文件
        this.psiJavaFileSet = SrcFileAPI.getSrcFileAPI().chooseFiles(this.project);
        //提取注解
        for (PsiJavaFile psiJavaFile : this.psiJavaFileSet)
        {
            AnnotationAPI.getAPIAnno().getFieldsFromFile(psiJavaFile, this.apiMaps, this.failedList, this.projectList, this.psiJavaFileSet);
        }
        //发送请求
        JiraAPI.getJiraAPI().openConnection(UI.getUI().getUrlText());
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", UI.getUI().getUsernameText());
        loginData.put("password", UI.getUI().getPasswordText());
        JiraAPI.getJiraAPI().login(loginData);
        if (this.apiMaps.size() == 0 && this.failedList.size() == 0)
        {
            return;
        }
        this.updAPIMaps = new LinkedList<>();
        JiraAPI.getJiraAPI().setFieldMap();
        //获取已上传url
        Map<String, String> existingURLMap = JiraAPI.getJiraAPI().getExistingUrl();
        //待处理分离为新增和修改
        if(this.apiMaps != null && this.apiMaps.size() > 0)
        {
            for (int i = 0; i < this.apiMaps.size(); i++)
            {
                if (existingURLMap.keySet().contains(this.apiMaps.get(i).get("url")))
                {
                    this.apiMaps.get(i).put("key", existingURLMap.get(this.apiMaps.get(i).get("url")));
                    this.updAPIMaps.add(this.apiMaps.get(i));
                    this.apiMaps.remove(i);
                    i--;
                }
            }
        }
        //创建issues
        if(this.apiMaps != null && this.apiMaps.size() > 0)
        {
            String[] issueRes = JiraAPI.getJiraAPI().createAllIssues(this.apiMaps);
            //更新url
            if (issueRes[0].equals("201"))
            {
                JSONObject issueResObj = JSONObject.fromString(issueRes[1]);
                JSONArray issueResArr = JSONArray.fromString(issueResObj.get("issues").toString());
                LinkedList<Map> newURLs = new LinkedList<>();
                for (int i = 0; i < this.apiMaps.size(); i++)
                {
                    JSONObject issueObj = JSONObject.fromString(issueResArr.get(i).toString());
                    Map<String, String> newURLMap = new HashMap<>();
                    newURLMap.put("url", this.apiMaps.get(i).get("url"));
                    newURLMap.put("key", issueObj.get("key").toString());
                    newURLs.add(newURLMap);
                }
                JSONArray urlArr = new JSONArray();
                for (String key : existingURLMap.keySet())
                {
                    JSONObject urlObj = new JSONObject();
                    urlObj.put("url", key);
                    urlObj.put("key", existingURLMap.get(key));
                    urlArr.put(urlObj);
                }
                for (Map<String, String> url : newURLs)
                {
                    JSONObject urlObj = new JSONObject();
                    urlObj.put("url", url.get("url"));
                    urlObj.put("key", url.get("key"));
                    urlArr.put(urlObj);
                }
                JiraAPI.getJiraAPI().updateUrl(urlArr.toString());
            }
            else
            {
                //【失败】
            }
        }
        //更新issue
        if (this.updAPIMaps != null && this.updAPIMaps.size() > 0)
        {
            JiraAPI.getJiraAPI().updateAllIssues(this.updAPIMaps);
        }
        //关闭请求
        try {
            JiraAPI.getJiraAPI().closeConnection();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //输出结果
        writeRes();
        Messages.showMessageDialog(
                "成功新增API：" + this.apiMaps.size() + "\n成功修改API：" + this.updAPIMaps.size() + "\n失败上传API：" + this.failedList.size(),
                "result",
                Messages.getInformationIcon()
        );
    }

    /**
     * 记录API生成失败的接口及原因
     * @return null
     */
    private void writeRes()
    {
        FileOutputStream out = null;
        try
        {
            File file = new File(this.resPath);
            out = new FileOutputStream(file,true);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (String fail : this.failedList)
            {
                String line = df.format(new Date()) + ": " + fail + "\n\r";
                out.write(line.getBytes("utf-8"));
            }
        }
        catch (IOException e1)
        {
            //【日志】写文件失败，路径问题
            e1.printStackTrace();
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }
}
