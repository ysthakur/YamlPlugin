package edu.team449

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

class MyYamlReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) =
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(YAMLPsiElement::class.java) as ElementPattern<out PsiElement>,
      object : PsiReferenceProvider() {
        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext
        ): Array<PsiReference> =
          when (element) {
            is YAMLKeyValue -> {
              arrayOf(
                //It's a constructor call
                if (isConstructorCall(element)) {
                  object : PsiPolyVariantReferenceBase<YAMLKeyValue>(element) {
                    //override fun resolve() = resolveToConstructor(element)

                    /**
                     * Return both the class declaration and its constructors
                     */
                    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
                      val cls = resolveToClass(element) ?: return emptyArray()
                      return allYAMLConstructors(cls).union<PsiElement>(listOf(cls))
                        .map { r: PsiElement -> PsiElementResolveResult(r) }.toTypedArray()
                    }
                  }
                  //Otherwise, it's a parameter
                } else object : PsiReferenceBase<YAMLKeyValue>(element) {
                  override fun resolve() =
                    resolveToParameter(element)
                })
            }
            is YAMLPlainTextImpl -> arrayOf(object : PsiReferenceBase<YAMLPlainTextImpl>(element) {
              override fun resolve() = resolveToIdDecl(element)
            })
            else -> emptyArray()
          }
      }
    )
}