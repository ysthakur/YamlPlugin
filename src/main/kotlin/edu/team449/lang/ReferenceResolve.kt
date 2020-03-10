package edu.team449.lang

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.YAMLKeyValue


val classNameRegexStr = """([A-Za-z_][A-Za-z_0-9]*\.)+[A-Za-z_][A-Za-z_0-9]*"""
val classNameDotRegex = Regex("$classNameRegexStr\\.")
val classNameRegex = Regex(classNameRegexStr)

/**
 * Find the class whose constructor/JsonCreator is being called
 * TODO make a separate method for constructor invocations
 */
fun resolveToClass(constructorCall: YAMLKeyValue): PsiClass? =
    resolveToClass(constructorCall.keyText, constructorCall.project)

/**
 * Find a constructor for a class (assuming there's only one annotated
 * JsonCreator)
 */
fun resolveToConstructor(constructorCall: YAMLKeyValue): PsiMethod? {
    return resolveToConstructor(resolveToClass(constructorCall) ?: return null)
}

/**
 * Find a constructor in the class by the name `className`, assuming there's
 * only one constructor used by Jackson
 */
fun resolveToConstructor(cls: PsiClass): PsiMethod? {
    return cls.constructors.find { method -> method.name == cls.name }
}

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
