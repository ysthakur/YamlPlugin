package edu.team449.lang

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Find the class whose constructor/JsonCreator is being called
 * TODO make a separate method for constructor invocations
 */
fun resolveToClass(constructorCall: YAMLKeyValue): PsiClass? =
    resolveToClass(constructorCall.keyText, constructorCall.project)

/**
 * Helpful method to find a class by name
 */
fun resolveToClass(className: String, project: Project): PsiClass? =
    JavaPsiFacade.getInstance(project).findClass(
        className,
        GlobalSearchScope.allScope(project)
    )

/**
 * Find a package by name
 */
fun resolveToPackage(pkgName: String, project: Project): PsiPackage? =
    JavaPsiFacade.getInstance(project).findPackage(pkgName)