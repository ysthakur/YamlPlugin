package edu.team449

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiParameter
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlCompletionContributor : CompletionContributor() {

  init {
    extend(
      BASIC,
      PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withLanguage(YAMLLanguage.INSTANCE),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
          parameters: CompletionParameters,
          context: ProcessingContext,
          resultSet: CompletionResultSet
        ) {
          fun addElems(elems: Iterable<String>) =
            elems.forEach { elem -> resultSet.addElement(LookupElementBuilder.create(elem)) }

          resultSet.addElement(LookupElementBuilder.create("org.usfirst.frc.team449.robot"))

          val element = parameters.position
          val project = element.project

          //Some idiot put in IntellijIdeaRulezzz into the text
          val text = element.text.replaceFirst(ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS, "")

          if (text.matches(classNameMaybeDot))
            resolveToPackage(
              text.replaceAfterLast(".", "").removeSuffix("."), project
            )?.let { addElems(allChildrenNames(it)) }
          else {
            val constructorCall = element.parent.parent.parent
            if (constructorCall is YAMLKeyValue)
              classOf(constructorCall)?.let { cls ->
                findConstructor(cls)?.let { ctor ->
                  addElems(ctor.parameterList.parameters.map(PsiParameter::getName))
                }
              }
          }
        }
      }
    )
  }

  companion object {
    /**
     * Return a list of all the classes and packages inside
     * a certain package
     */
    private fun allChildren(pkg: PsiPackage) =
      listOf<PsiElement>(*pkg.subPackages) + pkg.classes

    fun allChildrenNames(pkg: PsiPackage) = allChildren(pkg).mapNotNull { elem ->
      when (elem) {
        is PsiClass -> elem.qualifiedName
        is PsiPackage -> elem.qualifiedName
        else -> null
      }
    }
  }
}