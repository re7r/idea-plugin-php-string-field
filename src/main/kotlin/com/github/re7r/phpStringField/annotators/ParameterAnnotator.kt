package com.github.re7r.phpStringField.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.removeUserData
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Parameter

class ParameterAnnotator : Annotator, DumbAware {
    class Shared {
        companion object {
            val IS_PATH_PARAM: Key<Boolean> = Key.create("@string-field:is-path-param")
            val IS_CALLABLE: Key<Boolean> = Key.create("@string-field:is-callable")

            fun visit(element: Parameter): Boolean? {
                var isPathParam = false
                var isCallable = false

                val text = element.docTag?.text

                if (text != null) {
                    val pos = text.indexOf("@string-field")
                    if (pos != -1) {
                        isPathParam = true
                        isCallable = text
                            .drop(pos + "@string-field".length)
                            .take(":call".length)
                            .isNotEmpty()
                    }
                }

                if (isCallable) {
                    element.putUserData(IS_PATH_PARAM, true)
                    element.putUserData(IS_CALLABLE, true)
                    return true
                }

                if (isPathParam) {
                    element.putUserData(IS_PATH_PARAM, true)
                    element.putUserData(IS_CALLABLE, false)
                    return false
                }

                element.putUserData(IS_PATH_PARAM, false)
                element.removeUserData(IS_CALLABLE)
                return null
            }
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is Parameter) {
            Shared.visit(element)
        }
    }
}
