//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
///////   建表语句模板：                                                       ///////
///////   create table lsShopCart (                                            ///////
///////      ShopCartID           int                  identity(1,1),          ///////
///////      ProductID            int                  not null,               ///////
///////      UserID               int                  not null,               ///////
///////      Price                decimal(10,2)        not null,               ///////
///////      Amount               int                  not null,               ///////
///////      ShopTime             datetime             null,                   ///////
///////      constraint PK_LSSHOPCART primary key (ShopCartID)                 ///////
///////   )                                                                    ///////
///////   注意事项：                                                           ///////
///////         1. 主键一定要为第一个字段                                      ///////
///////         2. 数据类型支持大小写，但不能有空格，                          ///////
///////            例如decimal(10,2)不能为decimal(10, 2)                       ///////
///////         3. 以下数据类型不支持：                                        ///////
///////            binary、geography、geometry、hierarchyid、image、           ///////
///////            sql_variant、timestamp、uniqueidentifier、varbinary、xml    ///////
///////         4. 由于使用逗号来判断此行是否是数据库字段                      ///////
///////            所以每个字段要以逗号作为这一句的结束                        ///////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace EntityGenerate
{
    class Program
    {
        private static string inPath = @"C:\Users\0826\Desktop\crebas5.sql";                       //数据库建表文件位置
        private static string namespaces = "oilApp";                                               //项目名空间
        private static string tablePreName = "ls";                                                 //表前缀
        private static string tableName = "AfterSale";                                                 //表名
        private static string saveDir = @"C:\Users\0826\Desktop\file";                             //存储目录
        private static string tableHeader = "create table " + tablePreName + tableName + " (";     //建表语句第一行  
        
//***************************************************以上，参数填写部分*******************************************************

        static void Main(string[] args)
        {
            //创建文件夹
            if (Directory.Exists(saveDir + @"\Entity") == false)//如果不存在就创建file文件夹
            {
                Directory.CreateDirectory(saveDir + @"\Entity");
            }
            if (Directory.Exists(saveDir + @"\BLL") == false)//如果不存在就创建file文件夹
            {
                Directory.CreateDirectory(saveDir + @"\BLL");
            }
            if (Directory.Exists(saveDir + @"\DAL") == false)//如果不存在就创建file文件夹
            {
                Directory.CreateDirectory(saveDir + @"\DAL");
            }

            Read(inPath);
        }

        #region void Read 读取sql文件，获取字段信息
        static public void Read(string inPath)
        {
            string outPathEntity = saveDir + @"\Entity\" + tableName + "Entity.cs";  //Entity文件输出位置
            string outPathBLL = saveDir + @"\BLL\" + tableName + "BLL.cs";        //BLL文件输出位置
            string outPathDAL = saveDir + @"\DAL\" + tableName + "DAL.cs";        //DAL文件输出位置

            //数据库中存在的数据类型
            string[] intType = { "BigInt", "Int", "SmallInt", "TinyInt" };                                               //整型字段
            string[] dataType = { "Date", "DateTime2", "DateTimeOffset", "DateTime", "SmallDateTime", "Time" };                 //日期型字段
            string[] stringType = { "Char", "NChar", "NText", "NVarChar", "Text", "VarChar" };                                  //字符串型字段
            string[] decimalType = { "Decimal", "Float", "Money", "numeric", "Real", "SmallMoney" };                            //小数型字段
            string[] boolType = { "Bit"};                                                                                       //bool字段
            //复制一份全小写字段类型，以方便转化为C#类型
            string[] intTypeLower = new string[intType.Length];
            string[] dataTypeLower = new string[dataType.Length];
            string[] stringTypeLower = new string[stringType.Length];
            string[] decimalTypeLower = new string[decimalType.Length];
            string[] boolTypeLower = new string[boolType.Length];
            for (int i = 0; i < intType.Length || i < dataType.Length || i < stringType.Length || i < decimalType.Length || i < boolType.Length; i++)
            {
                if(i<intType.Length){
                    intTypeLower[i] = intType[i].ToLower();
                }
                if(i<dataType.Length){
                    dataTypeLower[i] = dataType[i].ToLower();
                }
                if(i<stringType.Length){
                    stringTypeLower[i] = stringType[i].ToLower();
                }
                if(i<decimalType.Length){
                    decimalTypeLower[i] = decimalType[i].ToLower();
                }
                if (i < boolType.Length)
                {
                    boolTypeLower[i] = boolType[i].ToLower();
                }
            }

            //数据表原始信息
            List<string> fieldName = new List<string>();    //字段名
            List<string> fieldType = new List<string>();    //字段数据类型
            List<bool> fieldAttr = new List<bool>();        //字段是否空
            //数据表分析信息
            List<string> fieldPostType = new List<string>();           //去除长度后的字段长度
            List<int> fieldLen = new List<int>();                      //字段长度，以字节为单位
            List<string> codeType = new List<string>();                //字段在C#中的数据类型
            int fieldCount = 0;                                        //字段数
            bool auto = false;                                         //主键是否自增

            StreamReader sr = new StreamReader(inPath, Encoding.Default);
            string line;
            while ((line = sr.ReadLine()) != null)
            {
                //寻找目标表，搜集原始字段信息
                if (line == tableHeader)
                {
                    line = sr.ReadLine();
                    //按行读取字段
                    while (line.Contains(","))
                    {
                        string[] words = line.Split(new char[] { ' ' });    //【空格】拆分有问题
                        string[] word = { "", "", "" };     //存储当前行的字段名、类型、是否为空
                        for (int i = 0,j = 0; i < words.Length; i++)
                        {
                            if (words[i] != "") {
                                word[j++] = words[i];
                                if (j == 3) {
                                    break;
                                }
                            }
                        }
                        fieldName.Insert(fieldCount, word[0]);
                        fieldType.Insert(fieldCount, word[1]);
                        if (word[2].ToLower() == "not")  //不可为空
                        {
                            fieldAttr.Insert(fieldCount++, false);
                        }
                        else if (word[2].ToLower() == "null,")//可为空
                        {
                            fieldAttr.Insert(fieldCount++, true);
                        }
                        else    //特殊情况
                        {
                            fieldAttr.Insert(fieldCount++, false);
                            auto = true;
                        }
                        line = sr.ReadLine();
                    }
                }
            }

            //截取并保存字段类型长度
            for (int i = 0; i < fieldCount; i++) {
                if (fieldType[i].Contains(","))     //含精度
                { 
                    int leftIndex = fieldType[i].IndexOf("(")+1;
                    int rightIndex = fieldType[i].IndexOf(",");
                    fieldLen.Insert(i, Convert.ToInt32(fieldType[i].Substring(leftIndex, rightIndex - leftIndex))/2+1);
                    fieldPostType.Insert(i, fieldType[i].Remove(fieldType[i].IndexOf("(")));
                }
                else if (fieldType[i].Contains("("))    //含长度
                {
                    int leftIndex = fieldType[i].IndexOf("(") + 1;
                    int rightIndex = fieldType[i].IndexOf(")");
                    fieldLen.Insert(i, Convert.ToInt32(fieldType[i].Substring(leftIndex, rightIndex - leftIndex)));
                    fieldPostType.Insert(i, fieldType[i].Remove(fieldType[i].IndexOf("(")));
                }
                else    //不含长度
                {
                    fieldPostType.Insert(i, fieldType[i]);
                    if (fieldType[i].ToLower() == "bit" || fieldType[i].ToLower() == "tinyint")
                    {
                        fieldLen.Insert(i, 1);
                    }
                    else if (fieldType[i].ToLower() == "smallint")
                    {
                        fieldLen.Insert(i, 2);
                    }
                    else if (fieldType[i].ToLower() == "int" || fieldType[i].ToLower() == "float" || fieldType[i].ToLower() == "smallmoney" || fieldType[i].ToLower() == "real")
                    {
                        fieldLen.Insert(i, 4);
                    }
                    else if (fieldType[i].ToLower() == "bigint" || fieldType[i].ToLower().Contains("date") || fieldType[i].ToLower().Contains("money"))
                    {
                        fieldLen.Insert(i, 8);
                    }
                    else if (fieldType[i].ToLower().Contains("text"))
                    {
                        fieldLen.Insert(i, 8000);
                    }
                    else
                    {
                        fieldLen.Insert(i, 0);
                    }
                }
            }

            //数据库字段类型转化为C#数据类型
            for(int i=0; i<fieldCount; i++) {
                if (intTypeLower.Contains(fieldPostType[i].ToLower()))   //整型
                {
                    codeType.Insert(i, "int");
                    //将原始数据类型转化为驼峰法表示
                    int index = intTypeLower.ToList().IndexOf(fieldPostType[i].ToLower());
                    fieldPostType[i] = intType[index];
                }
                else if (decimalTypeLower.Contains(fieldPostType[i].ToLower()))  //小数型
                {
                    codeType.Insert(i, "decimal");
                    //将原始数据类型转化为驼峰法表示
                    int index = decimalTypeLower.ToList().IndexOf(fieldPostType[i].ToLower());
                    fieldPostType[i] = decimalType[index];
                }
                else if (dataTypeLower.Contains(fieldPostType[i].ToLower())) //日期型
                {
                    codeType.Insert(i, "DateTime");
                    //将原始数据类型转化为驼峰法表示
                    int index = dataTypeLower.ToList().IndexOf(fieldPostType[i].ToLower());
                    fieldPostType[i] = dataType[index];
                }
                else if (boolTypeLower.Contains(fieldPostType[i].ToLower())) //bool型
                {
                    codeType.Insert(i, "bool");
                    //将原始数据类型转化为驼峰法表示
                    int index = boolTypeLower.ToList().IndexOf(fieldPostType[i].ToLower());
                    fieldPostType[i] = boolType[index];
                }
                else {  //字符型
                    codeType.Insert(i, "string");
                    //将原始数据类型转化为驼峰法表示
                    int index = stringTypeLower.ToList().IndexOf(fieldPostType[i].ToLower());
                    fieldPostType[i] = stringType[index];
                }
            }

            for(int i=0; i<fieldCount; i++) {
                Console.WriteLine(fieldName[i] + "\t\t" + fieldType[i] + "\t\t" + fieldAttr[i] + "\t" + fieldPostType[i] + "\t" + fieldLen[i]);
            }

            GenerateEntity(fieldName, codeType, fieldAttr, outPathEntity, namespaces, tableName, fieldCount);
            GenerateBLL(fieldName[0], outPathBLL, namespaces, tableName);
            GenerateDAL(fieldName, fieldPostType, fieldLen, fieldAttr, codeType, outPathDAL, namespaces, tablePreName, tableName, fieldCount, auto);
        }
        #endregion

        #region void GenerateEntity 生成实体文件
        static public void GenerateEntity(List<string> fieldName, List<string> codeType, List<bool> fieldAttr, string outPathEntity, string namespaces, string tableName, int fieldCount)
        {
            FileStream fs = new FileStream(outPathEntity, FileMode.Create);
            //订制输出内容
            string output = "";
            output +=   "using System;\n\n";

            output += "namespace " + namespaces + ".Entity\n" +
                        "{\n" +
                        "   public class " + tableName + "Entity\n" +
                        "   {\n" +
                        "       public " + tableName + "Entity() { }\n\n";

            for(int i=0; i<fieldCount; i++)
            {
                output += "       private " + codeType[i];
                if (fieldAttr[i] && codeType[i] != "string") {
                    output += "?";
                }
                output += " _" + fieldName[i].ToLower() + ";\n";
            }

            output += "\n";
            
            for (int i = 0; i < fieldCount; i++)
            {
                output += "       /// <summary>\n";
                output += "       /// \n";
                output += "       /// <summary>\n";
                output += "       public " + codeType[i];

                if (fieldAttr[i] && codeType[i] != "string")
                {
                    output += "?";
                }
                output += " " + fieldName[i] + "\n";
                output += "       {\n";
                output += "           set { _" + fieldName[i].ToLower() + " = value; }\n";
                output += "           get { return _" + fieldName[i].ToLower() + "; }\n";
                output += "       }\n";
            }

            output += "    }\n";
            output += "}\n";

            //获得字节数组
            byte[] data = System.Text.Encoding.Default.GetBytes(output);
            //开始写入
            fs.Write(data, 0, data.Length);
            //清空缓冲区、关闭流
            fs.Flush();
            fs.Close();
        }
        #endregion

        #region void GenerateBLL 生成BLL文件
        static public void GenerateBLL(string primary, string outPathBLL, string namespaces, string tableName)
        {
            FileStream fs = new FileStream(outPathBLL, FileMode.Create);
            //订制输出内容
            string output = "";
            output += "using System.Data;\n" +
                        "using " + namespaces + ".Entity;\n\n";

            output += "namespace " + namespaces + ".BLL\n" +
                        "{\n" +
                        "   public class " + tableName + "BLL\n" +
                        "   {\n" +
                        "       private readonly DAL." + tableName + "DAL dal = new DAL." + tableName + "DAL();\n\n" +
                        "       public " + tableName + "BLL() { }\n\n";

            output += "       /// <summary>\n" + 
                        "       /// 获取列表 select\n" +
                        "       /// </summary>\n" +
                        "       /// <param name=\"strWhere\">过滤条件</param>\n" +
                        "       /// <returns></returns>\n";

            output += "       public DataSet Select(string strWhere)\n" +
                        "       {\n" +
                        "           return dal.Select(strWhere);\n" +
                        "       }\n";

            output += "       /// <summary>\n" + 
                        "       /// 得到一个对象实体\n" +
                        "       /// </summary>\n";

            output += "       public " + tableName + "Entity GetEntity(int ID)\n" +
                        "       {\n" +
                        "           return dal.GetEntity(ID);\n" +
                        "       }\n";

            output += "       /// <summary>\n" + 
                        "       /// 增加一条数据\n" +
                        "       /// </summary>\n";

            output += "       public int Add(" + tableName + "Entity entity)\n" +
                        "       {\n" +
                        "           return dal.Add(entity);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 更新一条数据\n" +
                        "       /// </summary>\n";

            output += "       public void Update(" + tableName + "Entity entity)\n" +
                        "       {\n" +
                        "           dal.Update(entity);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 删除一条数据\n" +
                        "       /// </summary>\n";

            output += "       public void Delete(int ID)\n" +
                        "       {\n" +
                        "           dal.Delete(ID);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 执行一条dml语句\n" +
                        "       /// </summary>\n";

            output += "       public void DoDml(string sql)\n" +
                        "       {\n" +
                        "           dal.DoDml(sql);\n" +
                        "       }\n";

            output += "    }\n";
            output += "}\n";

            //获得字节数组
            byte[] data = System.Text.Encoding.Default.GetBytes(output);
            //开始写入
            fs.Write(data, 0, data.Length);
            //清空缓冲区、关闭流
            fs.Flush();
            fs.Close();
        }
        #endregion

        #region void GenerateDAL 生成DAL文件
        static public void GenerateDAL(List<string> fieldName, List<string> fieldPostType, List<int> fieldLen, List<bool> fieldAttr, List<string>codeType, string outPathDAL, string namespaces, string tablePreName, string tableName, int fieldCount, bool auto)
        {
            FileStream fs = new FileStream(outPathDAL, FileMode.Create);
            //订制输出内容
            string output = "";
            output += "using System;\n" +
                        "using System.Data;\n" +
                        "using System.Data.SqlClient;\n" +
                        "using System.Text;\n" +
                        //"using system.web;\n" +
                        //"using " + namespaces + ".common;\n" +
                        "using " + namespaces + ".DBUtility;\n" +
                        "using " + namespaces + ".Entity;\n\n";

            output += "namespace " + namespaces + ".DAL\n" +
                        "{\n" +
                        "   public class " + tableName + "DAL\n" +
                        "   {\n" +
                        "       public " + tableName + "DAL() { }\n\n";

            output += "       /// <summary>\n" +
                        "       /// 获取列表 select\n" +
                        "       /// </summary>\n" +
                        "       /// <param name=\"strWhere\">过滤条件</param>\n" +
                        "       /// <returns></returns>\n";

            output += "       public DataSet Select(string strWhere)\n" +
                        "       {\n" +
                        "           string cmdtext = \"select * from " + tablePreName + tableName + " where 1=1 \" + strWhere;\n" +
                        "           return DbHelperSQL.ExecuteDataSet(DbHelperSQL.ConnectionString, \"dt\", CommandType.Text, cmdtext, null);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 得到一个对象实体\n" +
                        "       /// </summary>\n";

            output += "       public " + tableName + "Entity GetEntity(int ID)\n" +
                        "       {\n" +
                        "           " + tableName + "Entity entity = new " + tableName + "Entity();\n" +
                        "           StringBuilder strSql = new StringBuilder();\n" +
                        "           strSql.Append(\"select *\");\n" +
                        "           strSql.Append(\" FROM " + tablePreName + tableName + " \");\n" +
                        "           strSql.Append(\" where " + fieldName[0] + "=\" + ID + \" \");\n\n" +
                        "           DataSet ds = DbHelperSQL.GetDataSet(strSql.ToString());\n" +
                        "           if (ds.Tables[0].Rows.Count > 0)\n" +
                        "           {\n" +
                        "               if (ds.Tables[0].Rows[0][\"" + fieldName[0] + "\"].ToString() != \"\")\n" +
                        "               {\n" +
                        "                   entity." + fieldName[0] + " = Convert.ToInt32(ds.Tables[0].Rows[0][\"" + fieldName[0] + "\"]);\n" +
                        "               }\n";

            for (int i = 1; i < fieldCount; i++) {
                if (codeType[i] != "string" && fieldAttr[i])    //允许为空的非字符串需要做判断
                {
                    output += "               if (ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"].ToString() != \"\")\n" +
                                "                {\n    ";
                }
                switch(codeType[i]){
                    case "int":
                        output += "               entity." + fieldName[i] + " = Convert.ToInt32(ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"]);\n";
                        break;
                    case "decimal":
                        output += "               entity." + fieldName[i] + " = Convert.ToDecimal(ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"]);\n";
                        break;
                    case "DateTime":
                        output += "               entity." + fieldName[i] + " = Convert.ToDateTime(ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"]);\n";
                        break;
                    case "string":
                        output += "               entity." + fieldName[i] + " = ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"].ToString();\n";
                        break;
                    case "bool":
                        output += "               entity." + fieldName[i] + " = Convert.ToBoolean(ds.Tables[0].Rows[0][\"" + fieldName[i] + "\"]);\n";
                        break;
                    default:
                        break;
                }
                if (codeType[i] != "string" && fieldAttr[i])    //允许为空的非字符串需要做判断
                {
                    output += "                }\n";
                }
            }

            output += "           }\n" +
                        "           return entity;\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 增加一条数据\n" +
                        "       /// </summary>\n";

            output += "       public int Add(" + tableName + "Entity entity)\n" +
                        "       {\n" +
                        "            StringBuilder strSql = new StringBuilder();\n" +
                        "            strSql.Append(\"insert into " + tablePreName + tableName + "(\");\n" +
                        "            strSql.Append(\"";
            
            for (int i = 0; i < fieldCount; i++)
            {
                if(fieldAttr[i] || (i==0 && auto))    //主键是自增类型，不需手动插入
                {
                    continue;
                }
                output += fieldName[i] + ",";
            }
            output = output.Remove(output.LastIndexOf(","));

            output += ")\");\n" +
                        "            strSql.Append(\" values (\");\n" +
                        "            strSql.Append(\"";

            for (int i = 0; i < fieldCount; i++)
            {
                if (fieldAttr[i] || (i == 0 && auto))    //主键是自增类型，不需手动插入
                {
                    continue;
                }
                output += "@" + fieldName[i] + ",";
            }
            output = output.Remove(output.LastIndexOf(","));

            output += ")\");\n";

            if (auto)
            {
                output += "            strSql.Append(\";select @@IDENTITY\");\n";
            }

            output += "            SqlParameter[] pars = {\n";

            for (int i = 0; i < fieldCount; i++)
            {
                if (fieldAttr[i] || (i == 0 && auto))    //主键是自增类型，不需手动插入
                {
                    continue;
                }
                output += "                                      DbHelperSQL.MakeInParam";
                output += "(\"@" + fieldName[i] + "\", SqlDbType." + fieldPostType[i] + "," + fieldLen[i] + ",entity." + fieldName[i] + "),\n";
            }
            output = output.Remove(output.LastIndexOf(","));

            output += "\n					};\n\n";
            
            if(auto)
            {
                output += "            object obj = DbHelperSQL.ExecuteScalar(CommandType.Text, strSql.ToString(), pars);\n";
            } 
            else
	        {
                output += "            object obj = DbHelperSQL.ExecuteNonQuery(DbHelperSQL.ConnectionString, CommandType.Text, strSql.ToString(), pars);\n";
	        }
                        
            output += "            if (obj == null)\n" +
                        "            {\n" +
                        "                return 1;\n" +
                        "            }\n" +
                        "            else\n" +
                        "            {\n" +
                        "                return Convert.ToInt32(obj);\n" +
                        "            }\n" +
                        "        }\n";

            output += "       /// <summary>\n" +
                        "       /// 更新一条数据\n" +
                        "       /// </summary>\n";

            output += "       public void Update(" + tableName + "Entity entity)\n" +
                        "       {\n" + 
                        "            StringBuilder strSql = new StringBuilder();\n" +
                        "            strSql.Append(\"update "+tablePreName+tableName+ " set \");\n" +
                        "            strSql.Append(\"";
            
            for (int i = 1; i < fieldCount; i++)
            {
                output += fieldName[i] + "=@" + fieldName[i] + ",";
            }
            output = output.Remove(output.LastIndexOf(","));

            output += "\");\n" +
                        "            strSql.Append(\" where " + fieldName[0] + "=\" + entity." + fieldName[0] + " + \" \");\n" +
                        "            SqlParameter[] pars = {\n";

            for (int i = 1; i < fieldCount; i++)
            {
                output += "                                      DbHelperSQL.MakeInParam";
                output += "(\"@" + fieldName[i] + "\", SqlDbType." + fieldPostType[i] + "," + fieldLen[i] + ",entity." + fieldName[i] + "),\n";
            }
            output = output.Remove(output.LastIndexOf(","));

            output += "\n					};\n\n" +
                        "            DbHelperSQL.ExecuteNonQuery(DbHelperSQL.ConnectionString, CommandType.Text, strSql.ToString(), pars);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 删除一条数据\n" +
                        "       /// </summary>\n";

            output += "       public void Delete(int ID)\n" +
                        "       {\n" +
                        "            StringBuilder strSql = new StringBuilder();\n" +
                        "            strSql.Append(\"delete from " + tablePreName + tableName + "\");\n" +
                        "            strSql.Append(\" where " + fieldName[0] + "=\" + ID + \" \");\n" +
                        "            DbHelperSQL.ExecuteNonQuery(DbHelperSQL.ConnectionString, CommandType.Text, strSql.ToString(), null);\n" +
                        "       }\n";

            output += "       /// <summary>\n" +
                        "       /// 执行一条dml语句\n" +
                        "       /// </summary>\n";

            output += "       public void DoDml(string sql)\n" +
                        "       {\n" +
                        "            DbHelperSQL.ExecuteNonQuery(DbHelperSQL.ConnectionString, CommandType.Text, sql, null);\n" +
                        "       }\n";

            output += "    }\n" +
                        "}\n";

            //获得字节数组
            byte[] data = System.Text.Encoding.Default.GetBytes(output);
            //开始写入
            fs.Write(data, 0, data.Length);
            //清空缓冲区、关闭流
            fs.Flush();
            fs.Close();
        }
        #endregion
    }
}