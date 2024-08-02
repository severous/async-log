package org.example.async.log.client.annotation;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import org.example.async.log.client.patcher.ClassRootFinder;
import org.example.async.log.client.permit.Permit;
import org.example.async.log.client.visitor.MethodVisitor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes({"*"})
public class AsyncLogAnnotationProcessor extends AbstractProcessor {

    /**
     * A {@code Messager} provides the way for an annotation processor to
     * report error messages, warnings, and other notices.
     */
    private Messager messager;

    /**
     * It offers a way to interact with the abstract syntax tree (AST)
     * that the Java compiler (javac) creates during the compilation process
     */
    private JavacTrees javacTrees;

    /**
     * It is used in conjunction with the JavacTrees class to
     * modify Java source code during the compilation process.
     */
    private TreeMaker treeMaker;
    /**
     * This class helps in creating and handling unique names for identifiers in the abstract syntax tree (AST).
     */
    private Names names;


    private AtomicBoolean ClassLoader_asyncLogAlreadyAddedTo = new AtomicBoolean(false);


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            findAndPatchClassLoader(processingEnv);
        } catch (Exception e) {
            ;
        }

        this.messager = processingEnv.getMessager();
        this.javacTrees = JavacTrees.instance(processingEnv);

        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);

    }



    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> rootElements = roundEnv.getRootElements();
        if(rootElements.isEmpty()){
            return true;
        }
        for (Element rootElement : rootElements) {
            JCTree tree = (JCTree) javacTrees.getTree(rootElement);
            tree.accept(new MethodVisitor(treeMaker, names));
        }

        return true;
    }


    void findAndPatchClassLoader(ProcessingEnvironment procEnv) throws Exception{
        ClassLoader environmentClassLoader = procEnv.getClass().getClassLoader();
        if (environmentClassLoader != null && environmentClassLoader.getClass().getCanonicalName().equals("org.codehaus.plexus.compiler.javac.IsolatedClassLoader")) {
            if (!ClassLoader_asyncLogAlreadyAddedTo.getAndSet(true)) {
                Method m = Permit.getMethod(environmentClassLoader.getClass(), "addURL", URL.class);
                URL selfUrl = new File(ClassRootFinder.findClassRootOfClass(AsyncLogAnnotationProcessor.class)).toURI().toURL();
                Permit.invoke(m, environmentClassLoader, selfUrl);
            }
        }

        ClassLoader ourClassLoader = AsyncLogAnnotationProcessor.class.getClassLoader();
        if (ourClassLoader == null) ourClassLoader =  ClassLoader.getSystemClassLoader();

        Processor processor;
        try {
            processor = (Processor) Class.forName("org.example.async.log.client.annotation.AsyncLogProcessor", false, ourClassLoader).getConstructor().newInstance();
            processor.init(procEnv);
        } catch (Exception e) {
            ;
        }
    }

}
