package edu.team449.lang.repr

import com.intellij.psi.PsiClass

typealias CompiledClass = Class<*>

sealed class ClassRepr<T>(open val cls: T) {

}

data class PsiClassWrapper(override val cls: PsiClass): ClassRepr<PsiClass>(cls) {

}

data class CompiledClassWrapper(override val cls: CompiledClass): ClassRepr<CompiledClass>(cls) {

}