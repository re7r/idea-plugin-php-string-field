package com.github.re7r.phpStringField.contributors

import com.github.re7r.phpNavigator.references.MethodPathReference
import com.github.re7r.phpStringField.annotators.ParameterAnnotator
import com.github.re7r.phpStringField.references.PropertyPathReference
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType

class PathReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java), object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement, context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is StringLiteralExpression) {
                        return emptyArray()
                    }

                    val paramList = PsiTreeUtil.getParentOfType(
                        element, ParameterList::class.java
                    ) ?: return emptyArray()

                    val paramIndex = paramList.parameters.indexOf(element)
                    val methodRef = paramList.parent as? MethodReference ?: return emptyArray()
                    val method = resolveMethod(methodRef) ?: return emptyArray()
                    val param = method.parameters.getOrNull(paramIndex) ?: return emptyArray()


                    val isPathParam = param.getUserData(ParameterAnnotator.Shared.IS_PATH_PARAM)

                    val isCallable = when (isPathParam) {
                        true -> param.getUserData(ParameterAnnotator.Shared.IS_CALLABLE) == true
                        null -> ParameterAnnotator.Shared.visit(param)
                        false -> null
                    } ?: return emptyArray()

                    val references = mutableListOf<PsiReference>()
                    val segments = parseElementPath(element) ?: return emptyArray()

                    var currentClass = resolveClassFromMethodRef(methodRef) ?: return emptyArray()
                    var currentOffset = 1

                    for (segment in segments) {
                        if (segment.isEmpty()) {
                            currentOffset += 1
                            continue
                        }

                        references.add(
                            PropertyPathReference(
                                element, TextRange(
                                    currentOffset, currentOffset + segment.length
                                ), currentClass, segment
                            )
                        )

                        if (isCallable) {
                            references.add(
                                MethodPathReference(
                                    element,
                                    TextRange(currentOffset, currentOffset + segment.length),
                                    currentClass,
                                    segment
                                )
                            )

                            val method = currentClass.methods
                                .filterIsInstance<Method>()
                                .firstOrNull { it.name == segment }

                            if (method != null) {
                                break
                            }
                        }

                        val field = currentClass.fields.filterIsInstance<Field>().firstOrNull { it.name == segment }

                        if (field != null) {
                            val fieldType = field.type
                            val nextClass = resolveClassFromType(fieldType, element.project)
                            if (nextClass != null) {
                                currentClass = nextClass
                                currentOffset += segment.length + 1
                                continue
                            }
                        }

                        break
                    }

                    return references.toTypedArray()
                }
            })
    }

    private fun parseElementPath(element: PsiElement): List<String>? {
        val contents = (element as? StringLiteralExpression)?.contents ?: return emptyList()
        return contents.split('.').takeIf { it.any { sublist -> sublist.isNotEmpty() } }
    }

    private fun resolveMethod(methodRef: MethodReference): Method? {
        return methodRef.resolve() as? Method ?: methodRef.resolveLocal().filterIsInstance<Method>().firstOrNull()
    }

    private fun resolveClassFromMethodRef(methodRef: MethodReference): PhpClass? {
        val parent = methodRef.firstChild
        val phpType = ((parent as? PhpTypedElement)?.type ?: PhpType.EMPTY).global(parent.project)
        if (phpType == PhpType.EMPTY) return null

        return resolveClassFromType(phpType, methodRef.project)
    }

    private fun resolveClassFromType(phpType: PhpType, project: Project): PhpClass? {
        return phpType.types.asSequence().map { it.removePrefix("\\") }.mapNotNull { fqn ->
            PhpIndex.getInstance(project).getClassesByFQN(fqn).firstOrNull()
        }.firstOrNull()
    }
}


