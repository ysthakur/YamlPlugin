package edu.team449.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlJavaClassOrConstructorReference(constructorCall: YAMLKeyValue) :
    PsiPolyVariantReferenceBase<YAMLKeyValue>(constructorCall) {

    override fun resolve() = resolveToClass(element)
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        var size = 2
        val cls: PsiElement? = resolveToClass(element) ?: run { size--; null }
        val const: PsiElement? = resolveToConstructor(element) ?: run { size--; null }
        return Array<ResolveResult>(size) { i -> PsiElementResolveResult(if (i == 0) cls ?: const!! else const!!) }
    }

}