package edu.team449.lang

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage

sealed class JavaClassOrPackage<out T : PsiElement>(val held: T) : PsiElement {
    abstract val children: Iterable<JavaClassOrPackage<*>>
    //abstract val qualifiedName: String
}

class JavaClass(val jClass: PsiClass) : JavaClassOrPackage<PsiClass>(jClass), PsiClass by jClass {

    override fun getSourceElement(): PsiElement? {
        return super.getSourceElement()
    }

    override val children: Iterable<JavaClassOrPackage<*>>
        get() = jClass.allInnerClasses.map(::JavaClass)
}

class JavaPackage(val jPkg: PsiPackage) : JavaClassOrPackage<PsiPackage>(jPkg), PsiPackage by jPkg {
    override val children: Iterable<JavaClassOrPackage<*>>
        get() = jPkg.classes.toMutableList().map(::JavaClass) + jPkg.subPackages.map(::JavaPackage)
}