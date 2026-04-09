package com.github.re7r.phpStringField.references

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass

class MethodPathReference(
    element: PsiElement,
    range: TextRange,
    private val contextClass: PhpClass,
    private val methodName: String
) : PsiReferenceBase<PsiElement>(element, range) {
    override fun resolve(): PsiElement? {
        return contextClass.methods.firstOrNull {
            it.name == methodName && it.modifier.isPublic
        } as? Method
    }

    override fun getVariants(): Array<Any> {
        return contextClass.methods
            .asSequence()
            .filterIsInstance<Method>()
            .filter { it.modifier.isPublic }
            .map { method ->
                LookupElementBuilder
                    .create(method.name)
                    .withIcon(method.getIcon(0))
                    .withTypeText(method.type.toString(), true)
                    .withTailText(" in ${method.containingClass?.name}", true)
            }
            .toList()
            .toTypedArray()
    }
}