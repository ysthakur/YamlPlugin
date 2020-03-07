package edu.team449.lang

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionType.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLScalar


class YamlCompletionContributor : CompletionContributor() {

    val LOG = Logger.getInstance(javaClass)

    init {
        extend(
            BASIC,
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withLanguage(YAMLLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    @NotNull parameters: CompletionParameters,
                    context: ProcessingContext,
                    @NotNull resultSet: CompletionResultSet
                ) {
                    fun addElem(elem: String) = resultSet.addElement(LookupElementBuilder.create(elem))
                    fun addElems(elems: Iterable<String>) = elems.forEach(::addElem)

                    addElem("org.usfirst.frc.team449.robot")
                    val element = parameters.position
//                    if (element !is YAMLPsiElement) {
//                        LOG.warn("Not yaml ${element.text} ${element.elementType} ${element.javaClass}")
//                        return
//                    }
                    val project = element.project
                    //Some idiot put in IntellijIdeaRulezzz into the text
                    val text = element.text.replaceFirst("IntellijIdeaRulezzz", "")
                    val pkgName = text.removeSuffix(".")
                    if (text.matches(MyYamlReferenceProvider.classNameDotRegex)) {
                        addElems(allChildrenNames(resolveToPackage(pkgName, project) ?: return))
                    } else if (text.matches(MyYamlReferenceProvider.classNameRegex)) {
                        val pkg = resolveToPackage(pkgName, project) ?: return
                        addElems(allChildrenNames(pkg))
                        //Perhaps this package name is incomplete, so go to the parent
                        //  and see everything else that could match
                        addElems(allChildrenNames(pkg.parentPackage ?: return))
                    }
                }
            }
        )
    }
    /**
     * Return a list of all the classes and packages inside
     * a certain package
     */
    fun allChildren(pkg: PsiPackage): Iterable<PsiElement> {
        val res = mutableListOf<PsiElement>(*pkg.subPackages)
        res.addAll(pkg.classes)
        return res
    }

    fun allChildrenNames(pkg: PsiPackage): Iterable<String> = allChildren(pkg).map {
        elem -> when (elem) {
            is PsiClass -> elem.qualifiedName ?: throw Error("It's an anonymous or local class! Aaahhh!")
            is PsiPackage -> elem.qualifiedName
            else -> throw Error("Not a class or package! Oh no!")
        }
    }

}