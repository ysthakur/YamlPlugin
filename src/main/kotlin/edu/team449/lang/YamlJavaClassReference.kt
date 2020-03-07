package edu.team449.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlJavaClassReference(constructorCall: YAMLKeyValue) : PsiReferenceBase<YAMLKeyValue>(constructorCall) {

    override fun resolve() = resolveToClass(element)

}