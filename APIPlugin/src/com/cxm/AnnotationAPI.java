package com.cxm;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 注释类，主要对注释进行处理
 * @author chenximeng
 * @version 1.0
 */
public class AnnotationAPI {

    private static AnnotationAPI apiAnno;
    private String projectVal = "";
    private String urlCommon = "";
    private String returnName = "";
    private String PROJECT_API = "project";
    private String VERSION_API = "version";
    private String URL_API = "url";
    private String METHOD_API = "method";
    private String HEADERS_API = "header";
    private String REQUEST_API = "param";
    private String RESPONSE_API = "return";
    private String DESCRIPTION_API = "description";
    private String fieldPath = "E:/fields.conf";
    private HashSet<String> basicType = new HashSet<String>(){{
        add("boolean");
        add("byte");
        add("char");
        add("short");
        add("int");
        add("long");
        add("float");
        add("double");
        add("String");
    }};

    public static AnnotationAPI getAPIAnno() {
        if (apiAnno == null)
        {
            apiAnno = new AnnotationAPI();
        }
        return apiAnno;
    }

    public void setPROJECT_API(String PROJECT_API) {
        this.PROJECT_API = PROJECT_API;
    }

    public void setVERSION_API(String VERSION_API) {
        this.VERSION_API = VERSION_API;
    }

    public void setURL_API(String URL_API) {
        this.URL_API = URL_API;
    }

    public void setMETHOD_API(String METHOD_API) {
        this.METHOD_API = METHOD_API;
    }

    public void setHEADERS_API(String HEADERS_API) {
        this.HEADERS_API = HEADERS_API;
    }

    public void setREQUEST_API(String REQUEST_API) {
        this.REQUEST_API = REQUEST_API;
    }

    public void setRESPONSE_API(String RESPONSE_API) {
        this.RESPONSE_API = RESPONSE_API;
    }

    public void setDESCRIPTION_API(String DESCRIPTION_API) {
        this.DESCRIPTION_API = DESCRIPTION_API;
    }

    private AnnotationAPI() {
    }

    /**
     * 从单个文件中提取注解
     * @param psiJavaFile 目标文件入口
     * @param apiMaps 提取成功的文件列表
     * @param failedList 提取失败的文件列表
     * @param projectList 提取成功的项目名列表
     * @param psiJavaFileSet 所有文件集合
     * @return null
     */
    public void getFieldsFromFile(PsiJavaFile psiJavaFile, List<Map<String, String>> apiMaps, HashSet<String> failedList,
                                  HashSet<String> projectList, HashSet<PsiJavaFile> psiJavaFileSet)
    {
        readParaConf();
        PsiClass[] psiClasses = psiJavaFile.getClasses();
        PsiClass psiClass = null;
        PsiDocComment comment = null;
        PsiDocTag[] tags = null;
        PsiMethod[] methods = null;
        Map<String, String> paraAnnoMap = new HashMap<>();
        Map<String, String> returnAnnoMap = new HashMap<>();
        this.projectVal = "";
        this.urlCommon = "";
        for(int i = 0; i < psiClasses.length; i++)
        {
            psiClass = psiClasses[i];

            //没有类注释的话则这个类无法上传
            if ((comment = psiClass.getDocComment()) == null)
            {
                for(PsiMethod m : psiClass.getMethods())
                {
                    failedList.add(psiClass.getName() + ":" + m.getName() + ":" + "缺少类注释");
                }
                continue;
            }

            dealClassDoc(psiClass, comment);

            //没有project字段的话则整个类都无法上传
            methods = psiClass.getMethods();
            if (projectVal.equals(""))
            {
                for(PsiMethod m : methods)
                {
                    failedList.add(psiClass.getName() + ":" + m.getName() + ":" + "类注释中缺少@project标签");
                }
                continue;
            }

            dealMethodDoc(methods, psiClass, apiMaps, failedList, projectList, psiJavaFileSet, paraAnnoMap, returnAnnoMap);
        }
    }

    /**
     * 处理类注释部分
     * @param psiClass
     * @param comment
     */
    private void dealClassDoc(PsiClass psiClass, PsiDocComment comment)
    {
        //获取RequestMapping里面的属性, 可被标签覆盖
        PsiElement classDocSibling = psiClass.getDocComment().getNextSibling();
        String classRmText = "";
        while (classDocSibling != null)
        {
            classRmText = classDocSibling.getText();
            if (classRmText.contains("@RequestMapping"))
            {
                if(classRmText.contains("\""))
                {
                    this.urlCommon = classRmText.substring(classRmText.indexOf("\"") + 1, classRmText.lastIndexOf("\""));
                }
                break;
            }
            classDocSibling = classDocSibling.getNextSibling();
//                    另外两种筛选方式 PsiModifierList
//                    System.out.println(docSibling.getClass().toString());
//                    System.out.println(docSibling instanceof PsiModifierList);
        }

        //获取类标签，优先
        PsiElement[] classDocChildren = comment.getChildren();
        for (PsiElement docChild : classDocChildren)
        {
            if (docChild.getText().contains("@" + this.PROJECT_API))
            {
                this.projectVal = getTagValue(this.PROJECT_API, docChild.getText());
            }
            else if (docChild.getText().contains("@" + this.URL_API))
            {
                this.urlCommon = getTagValue(this.URL_API, docChild.getText());
            }
        }
    }

    /**
     * 处理方法注释部分
     * @param methods
     * @param psiClass
     * @param apiMaps
     * @param failedList
     * @param projectList
     * @param psiJavaFileSet
     */
    private void dealMethodDoc(PsiMethod[] methods, PsiClass psiClass, List<Map<String, String>> apiMaps, HashSet<String> failedList,
                               HashSet<String> projectList, HashSet<PsiJavaFile> psiJavaFileSet, Map<String, String> paraAnnoMap,
                               Map<String, String> returnAnnoMap)
    {
        PsiDocComment methodDoc = null;
        PsiMethod method = null;
        String summary = "";
        //获取方法注释
        for (int j = 0; j < methods.length; j++)
        {
            method = methods[j];
            //没有方法注释的话则这个方法无法上传
            String a = method.getDocComment().toString();
            String b = method.getDocComment().getText();
            if (method.getDocComment() == null)
            {
                failedList.add(psiClass.getName() + ":" + method.getName());
                continue;
            }

            //开始提取各字段
            Map<String, String> apiMap = new HashMap<>();
            apiMap.put("project", projectVal);
            //用于动态生成project
            apiMap.put("projectKey", projectVal.toUpperCase().replaceAll("\\s", ""));

            //获取RequestMapping里面的属性, 可被标签覆盖
            methodDoc = method.getDocComment();
            PsiElement docSibling = methodDoc.getNextSibling();
            String rmText = "";
            while (docSibling != null)
            {
                rmText = docSibling.getText();
                if (rmText.contains("@RequestMapping"))
                {
                    if(rmText.contains("value") && rmText.contains("\""))
                    {
                        apiMap.put("url", urlCommon + rmText.substring(rmText.indexOf("\"") + 1, rmText.lastIndexOf("\"")).trim());
                    }
                    if(rmText.contains("method") && rmText.contains("="))
                    {
                        apiMap.put("method", rmText.substring(rmText.lastIndexOf("=") + 1).trim());
                    }
                    break;
                }
                docSibling = docSibling.getNextSibling();
            }

            getMethodTag(methodDoc, apiMap, paraAnnoMap, returnAnnoMap);

            //获取形参具体内容
            PsiParameterList paraArr = method.getParameterList();
            PsiParameter[] paras = paraArr.getParameters();
            Map<String, String> paraMap = new HashMap<>();
            for (PsiParameter para : paras)
            {
                String paraType = para.getType().toString().split(":")[1];
                //非基本类型数据，需要获取里面的具体内容
                if (!this.basicType.contains(paraType))
                {
                    paraMap.put(para.getName(), getNonBasicField(paraType, psiJavaFileSet));
                }
                else
                {
                    paraMap.put(para.getName(), paraAnnoMap.get(para.getName()));
                }
            }
            JSONObject paraObj = JSONObject.fromMap(paraMap);
            apiMap.put("request", "\"" + paraObj.toString() + "\"");

            //获取返回值具体内容
            String returnType = method.getReturnType().toString().split(":")[1];
            Map<String, String> returnMap = new HashMap<>();
            //非基本类型数据，需要获取里面的具体内容
            if (this.basicType.contains(returnType) || returnType.contains("void"))
            {
                returnMap = returnAnnoMap;
                //允许@RESPONSE_API后面没有参数值
//                    PsiElement[] bodyChildren = method.getBody().getChildren();
//                    for (PsiElement bodyChild : bodyChildren)
//                    {
//                        if (bodyChild.getText().contains("return"))
//                        {
//                            String returnLine = bodyChild.getText();
//                            String returnName = returnLine.substring(returnLine.indexOf(" ") + 1, returnLine.lastIndexOf(";")).trim();
//                            returnMap.put(returnName, returnAnno);
//                        }
//                    }
            }
            else
            {
                returnMap.put(this.returnName, getNonBasicField(returnType, psiJavaFileSet));
            }
            JSONObject returnObj = JSONObject.fromMap(returnMap);
            apiMap.put("response", "\"" + returnObj.toString() + "\"");

            //从注释开始截取到第一个标签
            if (methodDoc.getText().contains("@"))
            {
                summary = methodDoc.getText().substring(0, methodDoc.getText().indexOf("@"));
            }
            else
            {
                summary = methodDoc.getText();
            }
            //概要不允许有换行符，不允许超过255个字符
            String[] summarySubs = summary.split("\r|\n");
            StringBuilder summaryBuild = new StringBuilder();
            for (String sub : summarySubs)
            {
                summaryBuild.append(sub.replaceAll("\\s*\\*\\s*", ""));
            }
            summary = summaryBuild.toString().substring(1);
            summary = summary.length() > 255 ? summary.substring(0, 255) : summary;
            apiMap.put("summary", summary);
            apiMap.put("contentType", "application/json");
            apiMap.put("accept", "application/json");
            //缺少必备字段的话则这个方法无法上传
            if (apiMap.containsKey("version") && apiMap.containsKey("url") && apiMap.containsKey("method") && apiMap.containsKey("header") && apiMap.containsKey("response"))
            {
                projectList.add(projectVal);
                apiMaps.add(apiMap);
            }
            else
            {
                failedList.add(psiClass.getName() + ":" + method.getName() + ":" + "标签中缺少必需字段");
            }
        }
    }

    /**
     * 获取方法标签, 优先
     * @param methodDoc
     * @param apiMap
     */
    private void getMethodTag(PsiDocComment methodDoc, Map<String, String> apiMap, Map<String, String> paraAnnoMap,
                              Map<String, String> returnAnnoMap)
    {
        PsiElement[] docChildren = methodDoc.getChildren();
        String tagValue = "";
        this.returnName = "";
        for (PsiElement docChild : docChildren)
        {
            if (docChild.getText().contains("@" + this.VERSION_API))
            {
                apiMap.put("version", getTagValue(this.VERSION_API, docChild.getText()));
            }
            else if (docChild.getText().contains("@" + this.URL_API))
            {
                apiMap.put("url", urlCommon + getTagValue(this.URL_API, docChild.getText()));
            }
            else if (docChild.getText().contains("@" + this.METHOD_API))
            {
                apiMap.put("method", getTagValue(this.METHOD_API, docChild.getText()));
            }
            else if (docChild.getText().contains("@" + this.HEADERS_API))
            {
                apiMap.put("header", getTagValue(this.HEADERS_API, docChild.getText()));
            }
            else if (docChild.getText().contains("@" + this.DESCRIPTION_API))
            {
                apiMap.put("description", getTagValue(this.DESCRIPTION_API, docChild.getText()));
            }
            //【不按 标签 值 解释 的格式怎么办，会导致键为空】
            else if (docChild.getText().contains("@" + this.RESPONSE_API))
            {
                //取出@RESPONSE_API后面的值，包括参数和参数说明
                tagValue = getTagValue(this.RESPONSE_API, docChild.getText());
                //通过第一次出现的空白符号分解参数名和参数说明
                Pattern p = Pattern.compile("\\s+");
                Matcher m = p.matcher(tagValue);
                while(m.find())
                {
                    this.returnName = tagValue.substring(0, tagValue.indexOf(m.group()));
                    String returnAnno = tagValue.substring(tagValue.indexOf(m.group()) + 1).replaceAll("\\s*\\*+\\s*", "");
                    returnAnnoMap.put(this.returnName, returnAnno.replaceAll("\r|\n", "").trim());
                    break;
                }
            }
            else if (docChild.getText().contains("@" + this.REQUEST_API))
            {
                //取出@REQUEST_API后面的值
                tagValue = getTagValue(this.REQUEST_API, docChild.getText());
                //通过第一次出现的空白符号分解参数名和参数说明
                Pattern p = Pattern.compile("\\s+");
                Matcher m = p.matcher(tagValue);
                while(m.find())
                {
                    String paraName = tagValue.substring(0, tagValue.indexOf(m.group()));
                    String paraAnno = tagValue.substring(tagValue.indexOf(m.group()) + 1).replaceAll("\\s*\\*+\\s*", "");
                    paraAnnoMap.put(paraName, paraAnno.replaceAll("\r|\n", "").trim());
                    break;
                }
            }
        }
    }

    /**
     * 获取请求中的非基本类型参数内容
     * @param paraType 参数类型
     * @param psiJavaFileSet 所有文件集
     * @return nonBasicField 非基本类型参数的json字串
     */
    private String getNonBasicField(String paraType, HashSet<PsiJavaFile> psiJavaFileSet)
    {
        HashMap<String, String> nonBasicField = new HashMap<>();
        //集合类型数据，具体内容在本文件中
        if (paraType.toLowerCase().contains("set") || paraType.toLowerCase().contains("map") || paraType.toLowerCase().contains("list"))
        {
            //【后期考虑】
        }
        //其他类数据，具体内容在其他文件中
        else
        {
            dealClassField(nonBasicField, paraType, psiJavaFileSet);
        }
        return JSONObject.fromMap(nonBasicField).toString();
    }

    /**
     * 遍历文件集,找出该类对应的类文件
     * @param nonBasicField
     * @param paraType
     * @param psiJavaFileSet
     */
    private void dealClassField(HashMap<String, String> nonBasicField, String paraType, HashSet<PsiJavaFile> psiJavaFileSet){
        for (PsiJavaFile jFile : psiJavaFileSet)
        {
            if (jFile.getName().contains(paraType))
            {
                dealClassFieldDetail(nonBasicField, psiJavaFileSet, jFile);
            }
        }
    }

    /**
     * 判断该类中成员是否为基本类型
     * @param nonBasicField
     * @param psiJavaFileSet
     * @param jFile
     */
    private void dealClassFieldDetail(HashMap<String, String> nonBasicField, HashSet<PsiJavaFile> psiJavaFileSet, PsiJavaFile jFile){
        PsiField[] fields = jFile.getClasses()[0].getFields();
        for (PsiField field : fields)
        {
            String fieldType = field.getType().toString().split(":")[1];
            //子字段也是非基本类型
            if (!this.basicType.contains(fieldType))
            {
                nonBasicField.put(field.getName(), getNonBasicField(fieldType, psiJavaFileSet));
            }
            else
            {
                dealBasicField(nonBasicField, field);
            }
        }
    }

    /**
     * 处理基本类型的成员属性
     * @param nonBasicField
     * @param field
     */
    private void dealBasicField(HashMap<String, String> nonBasicField, PsiField field) {
        PsiElement[] fieldChildren = field.getDocComment().getChildren();
        for (PsiElement fieldChild : fieldChildren)
        {
            if (fieldChild.getText().contains("@" + this.REQUEST_API))
            {
                nonBasicField.put(field.getName(), getTagValue(this.REQUEST_API, fieldChild.getText()));
            }
        }
    }

    /**
     * 从整个标签中获取标签的值
     * @param tagName 便签键
     * @param tagText 整个标签文本
     * @return null
     */
    private String getTagValue(String tagName, String tagText)
    {
        if(tagText.contains("\n"))
        {
            return tagText.substring(tagText.indexOf(tagName) + tagName.length() + 1, tagText.lastIndexOf("\n")).trim();
        }
        return tagText.substring(tagText.indexOf(tagName) + tagName.length() + 1).trim();
    }


//     if (false)
//    {
//        //获取现有的project
//        String existingProjsStr = sendRequest(httpClient, METHOD.GET, this.url + "/rest/api/2/project", "").split("&&")[1];
//        JSONArray existingProjsArr = new JSONArray(existingProjsStr);
//        HashSet<String> existingProjsSet = new HashSet<String>();
//        for(int i = 0; i < existingProjsArr.length(); i++)
//        {
//            existingProjsSet.add(existingProjsArr.getJSONObject(i).get("name").toString());
//        }
//        //现有项目没有的话则需新建这个项目
//        for(int i = 0; i < apiMaps.size(); i++)
//        {
//            String proj = apiMaps.get(i).get("project");
//            String projKey = proj.toUpperCase().replaceAll("\\s", "");
//            if (!existingProjsSet.contains(proj))
//            {
//                //创建project
//                jsonParam = new JSONObject();
//                jsonParam.put("key", projKey);
//                jsonParam.put("name", proj);
//                jsonParam.put("projectTypeKey", "software");
//                jsonParam.put("lead", this.username);
//                jsonParam.put("avatarId", 10324);
//                jsonParam.put("assigneeType", "UNASSIGNED");
////                        "projectTemplateKey": "com.atlassian.jira-core-project-templates:jira-core-project-management",
////                        "description": "Example Project description",
////                        "url": "http://atlassian.com",
////                        "issueSecurityScheme": 10001,
////                        "permissionScheme": 10011,
////                        "notificationScheme": 10021,
////                        "categoryId": 10120
//                String addProjRes = sendRequest(httpClient, METHOD.POST,this.url + "/rest/api/2/project", jsonParam.toString()).split("&&")[0];
//                if (!addProjRes.equals("201"))
//                {
//                    apiMaps.remove(i);
//                    failedList.add("");
//                }
//                else
//                {
//
//
//                    String getProjRes = sendRequest(httpClient, METHOD.GET,this.url + "/rest/api/2/project/" + projKey, "").split("&&")[1];
//                    JSONObject issueTypes = new JSONObject();
//                    issueTypes.put("id", "10008");
//                    JSONArray issueTypesArr = new JSONArray();
//                    issueTypesArr.put(issueTypes);
//                    JSONObject projRes = new JSONObject(getProjRes);
//                    projRes.remove("issueTypes");
//                    projRes.put("issueTypes", issueTypesArr);
//                    sendRequest(httpClient, METHOD.PUT,url + "/rest/api/2/project/" + projKey + "/type/software", projRes.toString());
//
//                    existingProjsSet.add(proj);
//                }
//            }
//        }
//    }

    /**
     * 读取字段的配置文件
     */
    private void readParaConf()
    {
        //读取配置文件
        File file = new File(this.fieldPath);
        if (file.exists())
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(file));
                String tempString = null;
                String temp = "";
                while ((tempString = reader.readLine()) != null) {
                    temp = tempString.contains(":") ? tempString.substring(tempString.indexOf(":") + 1) : "";
                    if (!temp.equals(""))
                    {
                        switch (tempString)
                        {
                            case "PROJECT_API":
                                this.setPROJECT_API(temp);
                                break;
                            case "VERSION_API":
                                this.setVERSION_API(temp);
                                break;
                            case "URL_API":
                                this.setURL_API(temp);
                                break;
                            case "METHOD_API":
                                this.setMETHOD_API(temp);
                                break;
                            case "HEADERS_API":
                                this.setHEADERS_API(temp);
                                break;
                            case "REQUEST_API":
                                this.setREQUEST_API(temp);
                                break;
                            case "RESPONSE_API":
                                this.setRESPONSE_API(temp);
                                break;
                            case "DESCRIPTION_API":
                                this.setDESCRIPTION_API(temp);
                                break;
                        }
                    }
                }
            } catch (IOException e1) {
                //error to read fields.conf
                e1.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
