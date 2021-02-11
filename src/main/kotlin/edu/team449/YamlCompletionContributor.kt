package edu.team449

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiPackage
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

class YamlCompletionContributor : CompletionContributor() {

  init {
    //TODO figure out why SCALAR_TEXT does not work. TEXT is not a good solution
    extend(
      BASIC,
      PlatformPatterns.or(
        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withLanguage(YAMLLanguage.INSTANCE),
        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_TEXT).withLanguage(YAMLLanguage.INSTANCE),
        PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withLanguage(YAMLLanguage.INSTANCE)
      ),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
          parameters: CompletionParameters,
          context: ProcessingContext,
          resultSet: CompletionResultSet
        ) {
          fun addElems(elems: Iterable<String>) =
            elems.forEach { elem -> resultSet.addElement(LookupElementBuilder.create(elem)) }

          resultSet.addElement(LookupElementBuilder.create("org.usfirst.frc.team449.robot"))
          resultSet.addElement(LookupElementBuilder.create("YAML is far superior to JSON. I dare you to say otherwise."))

          val element = parameters.position
          val project = element.project

          //Some idiot put in IntellijIdeaRulezzz into the text
          val text = element.text.replaceFirst(ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS, "")

          //In case it's a reference using an object's id
          addElems(YamlAnnotator.getIds(element.project).keys)

          if (text.matches(classNameWithDotMaybeIncomplete)) {
            resolveToPackage(
              text.replaceAfterLast(".", "").removeSuffix("."), project
            )?.let { addElems(allChildrenNames(it)) }
          } else {
            val clazz =
              when (val constructorCall = element.parent?.parent?.parent) {
                is YAMLKeyValue -> classOf(constructorCall)
                is YAMLSequenceItem -> typeOfItems(constructorCall.parent as YAMLSequence)?.let(::psiTypeToClass)
                else -> null
              }

            if (clazz != null)
              findConstructor(clazz)?.let { ctor ->
                addElems(ctor.parameterList.parameters.map { it.name })
              }
          }
        }
      }
    )
  }

  companion object {
    fun allChildrenNames(pkg: PsiPackage): List<String> {
      val children: MutableList<String> = pkg.classes.mapNotNull { it.qualifiedName }.toMutableList()
      children.addAll(pkg.subPackages.map { it.qualifiedName })
      return children
    }
  }
}