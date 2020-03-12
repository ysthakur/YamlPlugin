package edu.team449.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalarText
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

/**
 * A reference using the id property
 */
class IdReference(idArg: YAMLPlainTextImpl) : PsiReferenceBase<YAMLPlainTextImpl>(idArg) {
    override fun resolve() = resolveToIdDecl(element)
}