package edu.team449.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlJavaParameterReference(arg: YAMLKeyValue) : PsiReferenceBase<YAMLKeyValue>(arg) {
    override fun resolve() = resolveToParameter(element)
}