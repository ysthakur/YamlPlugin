package edu.team449.lang

import com.intellij.psi.PsiElement

class NoSuchParameterException(paramName: String, val elem: PsiElement) :
    Exception("Could not find a parameter by the name of $paramName") {
}