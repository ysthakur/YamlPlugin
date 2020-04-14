package edu.team449.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        if (element.project.robotClass() == null) {
            Logger.getInstance(javaClass).error("Oh no, robot class is null!!!!!")
        }

        //val cls = JavaClassReferenceSet("")
        /*val proj = element.project
        val modules = ProjectRootManager.getInstance(proj).contentRootsFromAllModules.map { vf ->
            ProjectFileIndex.getInstance(proj).getModuleForFile(vf)!!
        }
        for (module in modules) {
            val libs = mutableListOf<String?>()
            module.rootManager.orderEntries().forEachLibrary { lib ->
                libs.add(lib.name); true
            }
            //Messages.showInfoMessage(proj, "Module $module's libraries = $libs", "A message")
        }*/

        //Logger.getInstance(this::class.java).warn("Got to annotate")
        if (element !is YAMLKeyValue) {
            //holder.createWeakWarningAnnotation(element, "Not YAMLKeyValue")
            return
        }
        if (!(element.key ?: return).text.matches(classNameRegex)) {
            //holder.createWarningAnnotation(element, "Not class reference")
            return
        }
        val value = element.value
        //This means that it is merely a reference to another object
        if (value is YAMLPlainTextImpl) {
            //Invalid reference
            val ref = resolveToObjectDef(value)
            if (ref.first == null) {
                holder.createErrorAnnotation(value, "Could not find previous object with id ${value.text}")
            } else if (ref.second) {
                holder.createErrorAnnotation(value, "Illegal forward reference")
            }
            return
        }

        /*if (element.keyText.contains("edu.wpi")) {
            val refset = JavaClassReferenceSet(element.keyText, element, element.key!!.startOffsetInParent, false, JavaClassReferenceProvider())
            val refs = refset.references
            for (ref in refs) {
                val cls = ref.resolve()
            }
        }*/

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
            val constructor = resolveToConstructor(cls)
            if (constructor == null) {
                holder.createErrorAnnotation(element.key!!, "Could not find a constructor for class ${cls.name}")
                return
            }
            val params = constructor.parameterList.parameters.toMutableList()
            var id: String? = getIdName(cls)
            //val args = element.value!! as YAMLMappingImpl
            val args = try {
                getAllArgs(element)
            } catch (nspe: NoSuchParameterException) {
                holder.createErrorAnnotation(nspe.elem, "Could not resolve parameter")
                return
            }
            for (argKey in args.keys) {
                val keyText = argKey.argName.withoutQuotes()
                val param = params.find { p -> p.name == keyText }
                if (param == null) {
                    if (keyText == id) id = null
                    else holder.createErrorAnnotation(argKey.elem, "Could not find a parameter named $keyText")
                } else {
                    params.remove(param)
                }
//                if (isConstructorCall(arg)) {
//
//                }
            }
            for (param in params) {
                //Check that all the parameters have been entered in
                if (param.annotations.find { a -> a.text.contains(Regex("""@JsonProperty\(.*required( )?=( )?true""")) } != null) {
                    //Means it is annotated @NotNull
                    holder.createErrorAnnotation(element.key!!, "No argument given for required property ${param.name}")
                }
            }
            if (id != null) {
                holder.createErrorAnnotation(element.key!!, "Id property $id not given")
            }
            //holder.createInfoAnnotation(element.value!!, "Type is " + element.value!!.javaClass)
        }
    }
}