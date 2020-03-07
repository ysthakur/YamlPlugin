package edu.team449.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

class MyYamlAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        //Logger.getInstance(this::class.java).warn("Got to annotate")
        if (element !is YAMLKeyValue) {
            //holder.createWeakWarningAnnotation(element, "Not YAMLKeyValue")
            return
        }
        if (!(element.key ?: return).text.matches(MyYamlReferenceProvider.classNameRegex)) {
            //holder.createWarningAnnotation(element, "Not class reference")
            return
        }
        val ref = YamlJavaClassReference(element).resolve()
        if (ref == null) {
            holder.createErrorAnnotation(element.key!!,
                if (resolveToPackage(element.key!!.text, element.project) != null)
                    "${element.key!!.text} is a package, not a class"
                else
                    "Could not resolve reference to ${element.key!!.text}")
        }
        //todo add support for jsoncreators and check if all constructor parameters have been
        // passed and that the types are correct
    }
}