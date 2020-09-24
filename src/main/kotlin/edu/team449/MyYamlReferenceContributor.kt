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
            is YAMLKeyValue ->
              arrayOf(
                if (isConstructorCall(element)) ConstructorRef(element) //It's a constructor call
                else ParamRef(element) //otherwise, it's an argument/reference to a parameter
              )
            is YAMLPlainTextImpl -> arrayOf(IdRef(element))
            else -> emptyArray()
          }
      }
    )
}

class ConstructorRef(val ctor: YAMLKeyValue) : PsiPolyVariantReferenceBase<YAMLKeyValue>(ctor) {
  //override fun resolve() = resolveToConstructor(element)
  /**
   * Return both the class declaration and its constructors
   */
  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    val cls = resolveToClass(ctor) ?: return emptyArray()
    return (allYAMLConstructors(cls) + cls)
      .map(::PsiElementResolveResult)
      .toTypedArray()
  }
}

class ParamRef(val param: YAMLKeyValue) : PsiReferenceBase<YAMLKeyValue>(param) {
  override fun resolve() =
    resolveToParameter(param)
}

class IdRef(val id: YAMLPlainTextImpl) : PsiReferenceBase<YAMLPlainTextImpl>(id) {
  override fun resolve() = resolveToObjectDef(id).first
}