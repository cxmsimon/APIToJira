package com.cxm;

import java.awt.*;

/**
 * 对UI进行处理
 * @author chenximeng
 * @version 1.0
 */
public class UI {

    private static UI UI;
    private String urlText;
    private String usernameText;
    private String passwordText;

    private UI() {
    }

    public static UI getUI() {
        if (UI == null)
            UI = new UI();
        return UI;
    }

    public String getUrlText()
    {
        return this.urlText;
    }

    public String getUsernameText()
    {
        return this.usernameText;
    }

    public String getPasswordText()
    {
        return new String(this.passwordText);
    }

    /**
     * 显示插件操作界面
     * @return status 表示对插件的操作
     */
    public int showUI()
    {
        UserConf dialog = new UserConf();
        this.urlText = dialog.getUrlText();
        this.usernameText = dialog.getUsernameText();
        this.passwordText = dialog.getPasswordText();
        dialog.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((dim.width - dialog.getWidth()) / 2, (dim.height - dialog.getHeight()) / 2);
        dialog.setVisible(true);
        return dialog.getStatus();
    }
}
