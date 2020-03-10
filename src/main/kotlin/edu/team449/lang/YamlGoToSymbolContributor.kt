package edu.team449.lang

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope

class YamlGoToSymbolContributor : ChooseByNameContributor {
    override fun getItemsByName(
        name: String?,
        pattern: String?,
        project: Project?,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        Logger.getInstance(this::class.java).warn("Entered getItemsByName in gotosymbol")
        if (name == null || project == null) return emptyArray()
        if (name.matches(classNameRegex)) {
            val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
            if (sourceRoots.isEmpty()) return emptyArray()
            val javaVirtDir = sourceRoots.find { file -> file.name == "java" }
                ?: throw Error("Java source root not found!")
            val javaPsiDir = PsiManager.getInstance(project).findDirectory(javaVirtDir)!!
            val jClass = JavaPsiFacade.getInstance(project).findClass(
                name,
                GlobalSearchScope.allScope(project)
            )
            if (jClass != null) {
                return arrayOf(jClass)
            }
        }
        return emptyArray()
    }

    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<String> {
        return emptyArray()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}