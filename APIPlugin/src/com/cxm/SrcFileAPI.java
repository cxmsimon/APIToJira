package com.cxm;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import java.util.HashSet;

/**
 * 对源文件进行处理
 * @author chenximeng
 * @version 1.0
 */
public class SrcFileAPI {

    private static SrcFileAPI srcFileAPI;

    private SrcFileAPI() {
    }

    public static SrcFileAPI getSrcFileAPI() {
        if (srcFileAPI == null)
        {
            srcFileAPI = new SrcFileAPI();
        }
        return srcFileAPI;
    }
    /**
     * 遍历选中的所有文件
     * @param project 插件动作对象
     * @return psiJavaFileSet 目标文件集
     */
    public HashSet<PsiJavaFile> chooseFiles(Project project)
    {
        HashSet<PsiJavaFile> psiJavaFileSet = new HashSet<>();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false,
                false, false, true);
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(descriptor, project, null);
        for (VirtualFile virtualFile : virtualFiles)
        {
            getAllFiles(project, virtualFile, psiJavaFileSet);
        }
        return psiJavaFileSet;
    }

    /**
     * 遍历文件夹的所有文件
     * @param project 插件动作对象
     * @param virtualFile 目标文件对象
     * @param psiJavaFileSet 目标文件集
     * @return null
     */
    private void getAllFiles(Project project, VirtualFile virtualFile, HashSet<PsiJavaFile> psiJavaFileSet)
    {
        if (virtualFile.isDirectory()) {
            for(VirtualFile virtualFileChild : virtualFile.getChildren())
            {
                getAllFiles(project, virtualFileChild, psiJavaFileSet);
            }
        }
        else {
            if (virtualFile.getName().endsWith(".java")) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(virtualFile);
                psiJavaFileSet.add(psiJavaFile);
            }
        }
    }
}
