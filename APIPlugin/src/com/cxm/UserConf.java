package com.cxm;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * UI生成类
 * @author intern_chenximeng
 * @version 1.0
 */
public class UserConf extends JDialog
{
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField urlText;
    private JTextField usernameText;
    private JPasswordField passwordText;
    private JButton connectBtn;
    private JLabel resultText;
    private JButton saveInfoBtn;
    private String userPath = "E:/userInfo.conf";
    private int status;

    public int getStatus()
    {
        return status;
    }

    public String getUrlText()
    {
        return urlText.getText();
    }

    public String getUsernameText()
    {
        return usernameText.getText();
    }

    public String getPasswordText()
    {
        return new String(passwordText.getPassword());
    }

    public UserConf()
    {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        //点击Cancel
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                status = 0;
                dispose();
            }
        });

        //点击Ok
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                status = 1;
                JiraAPI.getJiraAPI().setPreUrl(getUrlText());
                dispose();
            }
        });

        //点击Save Info
        saveInfoBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringBuilder data = new StringBuilder();
                data.append("url:" + getUrlText());
                data.append("\r\nusername:" + getUsernameText());
                data.append("\r\npassword:" + getPasswordText());

                FileOutputStream out = null;
                boolean isSucceed = true;
                try {
                    File file = new File(userPath);
                    out = new FileOutputStream(file,false);
                    out.write(data.toString().getBytes("utf-8"));
                }
                catch (IOException e1) {
                    isSucceed = false;
                    //【日志】写文件失败，路径问题
                    e1.printStackTrace();
                }
                finally
                {
                    if (isSucceed)
                    {
                        resultText.setText("信息保存成功！");
                    }
                    else
                    {
                        resultText.setText("信息保存失败！");
                    }
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
        });

        //点击Test connection
        connectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JiraAPI.getJiraAPI().openConnection(getUrlText());
                Map<String, String> loginData = new HashMap<>();
                loginData.put("username", getUsernameText());
                loginData.put("password", getPasswordText());
                resultText.setText("域名无法连接！");
                String[] res = JiraAPI.getJiraAPI().login(loginData);
                if (res[0].equals("200"))
                {
                    resultText.setText("测试成功！");
                }
                else
                {
                    resultText.setText("账号或密码错误！");
                }
                try {
                    JiraAPI.getJiraAPI().closeConnection();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                status=0;
                dispose();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                status=0;
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        //读用户信息
        File file = new File(userPath);
        if (file.exists())
        {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String tempString = null;
                while ((tempString = reader.readLine()) != null) {
                    if (tempString.contains("url"))
                    {
                        this.urlText.setText(tempString.substring(tempString.indexOf(":") + 1));
                    }
                    else if (tempString.contains("username"))
                    {
                        this.usernameText.setText(tempString.substring(tempString.indexOf(":") + 1));
                    }
                    else if (tempString.contains("password"))
                    {
                        this.passwordText.setText(tempString.substring(tempString.indexOf(":") + 1));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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
