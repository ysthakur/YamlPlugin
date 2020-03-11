package edu.team449.lang

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionType.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue

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
                    if (text.matches(classNameDotRegex)) {
                        val pkgName = text.removeSuffix(".")
                        addElems(allChildrenNames(resolveToPackage(pkgName, project) ?: return))
                    } else if (text.matches(classNameRegex)) {
                        val pkgName = text.replaceAfterLast(".", "").removeSuffix(".")
                        val pkg = resolveToPackage(pkgName, project)
                        if (pkg != null) addElems(allChildrenNames(pkg))
                        //Perhaps this package name is incomplete, so go to the parent
                        //  and see everything else that could match
                        //addElems(allChildrenNames(pkg.parentPackage ?: return))
                    } else {
                        val constructorCall = element.parent.parent.parent
                        val cls = if (constructorCall is YAMLKeyValue) typeOf(constructorCall) else null
                        if (cls != null) {
                            val constr = findJsonCreator(cls) ?: return
                            addElems(constr.parameterList.parameters.map(PsiParameter::getName))
                        }
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