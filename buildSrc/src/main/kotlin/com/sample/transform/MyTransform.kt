package com.sample.transform

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.Descriptor
import java.io.File

class MyTransform : Transform() {
    override fun getName(): String = "com.sample.build.MyTransform"

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun isIncremental(): Boolean = false

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
            mutableSetOf(QualifiedContent.Scope.PROJECT)

    override fun getReferencedScopes(): MutableSet<in QualifiedContent.Scope> =
        mutableSetOf(
            QualifiedContent.Scope.EXTERNAL_LIBRARIES,
            QualifiedContent.Scope.SUB_PROJECTS )

    // output: app/build/intermediates/transforms/com.sample.build.MyTransform
    override fun transform(transformInvocation: TransformInvocation) {

        val androidJar = "/Users/ethan/Desktop/sdk/platforms/android-28/android.jar"

        transformInvocation.outputProvider.deleteAll()

        val outputDir : File = transformInvocation.outputProvider.getContentLocation(
            "classes",
            outputTypes,
            scopes,
            Format.DIRECTORY )

        transformInvocation.inputs.forEach { transformInput ->
            transformInput.directoryInputs.forEach { inputDirectory ->

                //inputDirectory.file.copyRecursively(outputDir, true)

                val pool = ClassPool()
                pool.appendSystemPath()
                pool.insertClassPath(inputDirectory.file.absolutePath)
                pool.insertClassPath(androidJar)

                inputDirectory.file.walkTopDown().forEach { originalClassFile ->
                    if (originalClassFile.isClassfile()) {
                        val classname = originalClassFile.relativeTo(inputDirectory.file).toClassname()
                        val clazz = pool.get(classname)
                        transformClass(clazz)
                        clazz.writeFile(outputDir.absolutePath)
                    }
                }
            }
        }
    }

    private fun transformClass(clazz: CtClass) {

        clazz.constructors.forEach {
            println( "${it.name} : ${it.signature} Calls Super ${it.callsSuper()}" +
                    " Num Parameters ${Descriptor.numOfParameters( it.methodInfo.descriptor )}" )

            it.insertBefore( "throw new Exception();" )
        }

    }
}

private fun File.toClassname(): String =
    path.replace("/", ".")
        .replace("\\", ".")
        .replace(".class", "")

private fun File.isClassfile(): Boolean = isFile && path.endsWith(".class")