package edu.team449.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl

class MyYamlAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        //Logger.getInstance(this::class.java).warn("Got to annotate")
        if (element !is YAMLKeyValue) {
            //holder.createWeakWarningAnnotation(element, "Not YAMLKeyValue")
            return
        }
        if (!(element.key ?: return).text.matches(classNameRegex)) {
            //holder.createWarningAnnotation(element, "Not class reference")
            return
        }
        val cls = resolveToClass(element)
        if (cls == null) {
            holder.createErrorAnnotation(
                element.key!!,
                if (resolveToPackage(element.key!!.text, element.project) != null)
                    "${element.key!!.text} is a package, not a class"
                else
                    "Could not find ${element.key!!.text}"
            )
        } else {
            val constructor = findJsonCreator(cls)
            if (constructor == null) {
                holder.createErrorAnnotation(element.key!!, "Could not find a constructor for class ${cls.name}")
                return
            }
            val params = constructor.parameterList.parameters.toMutableSet()
            val idAnnotation = cls.annotations.find { annot ->
                annot.text.matches(Regex("""@JsonIdentityInfo\(.*"""))
            }
            var id: String? = if (idAnnotation != null) {
                idAnnotation.findAttributeValue("property")?.text?.withoutQuotes() ?: "@id"
            } else {
                null
            }
            val args = element.value!! as YAMLBlockMappingImpl
            for (arg in args.keyValues) {
                val keyText = arg.keyText.removeSurrounding("\"")
                val param = params.find { p -> p.name == keyText }
                if (param == null) {
                    if (keyText == id) id = null
                    else holder.createErrorAnnotation(arg.key!!, "Could not find a parameter named ${arg.keyText}")
                } else {
                    params.remove(param)
                }
//                if (isConstructorCall(arg)) {
//
//                }
            }
            for (param in params) {
                //Check that all the parameters have been entered in
                if (param.annotations.find { a -> a.qualifiedName?.contains("NotNull") == true } != null) {
                    //Means it is annotated @NotNull
                    holder.createErrorAnnotation(element.key!!, "No argument given for NotNull property ${param.name}")
                }
            }
            if (id != null) {
                holder.createErrorAnnotation(element.key!!, "Property $id not given")
            }
            //holder.createInfoAnnotation(element.value!!, "Type is " + element.value!!.javaClass)
        }
        //todo add support for jsoncreators and check if all constructor parameters have been
        // passed and that the types are correct
    }
}