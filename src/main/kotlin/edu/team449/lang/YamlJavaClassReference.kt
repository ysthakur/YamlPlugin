package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLPsiElement

class YamlJavaClassReference(val constructor: YAMLKeyValue) : PsiReferenceBase<YAMLKeyValue>(constructor) {

    override fun resolve(): PsiElement? {
        val key = constructor.getKey()
        val className = key!!.text

        //Logger.getInstance(this::class.java).warn("Got to resolve")
        try {
            //This means that it is probably a reference to some java class in nested packages
            val project = element.project
            val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
            if (sourceRoots.isEmpty()) return null
            val javaVirtDir = sourceRoots.find { file -> file.name == "java" }
                ?: throw Error("Java source root not found!")
            val javaPsiDir = PsiManager.getInstance(project).findDirectory(javaVirtDir)!!
            val jClass = JavaPsiFacade.getInstance(project).findClass(
                className,
                GlobalSearchScope.allScope(project)
            )
            if (jClass != null) {
                return jClass
            }
        } catch (npe: NullPointerException) {}

        return null
    }

}