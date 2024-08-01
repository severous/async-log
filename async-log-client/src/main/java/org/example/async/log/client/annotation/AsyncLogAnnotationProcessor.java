package org.example.async.log.client.annotation;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import org.example.async.log.client.patcher.ClassRootFinder;
import org.example.async.log.client.permit.Permit;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SupportedAnnotationTypes({"*"})
public class AsyncLogAnnotationProcessor extends AbstractProcessor {

    private AtomicBoolean ClassLoader_asyncLogAlreadyAddedTo = new AtomicBoolean(false);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            findAndPatchClassLoader(processingEnv);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
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
            processor = (Processor) Class.forName("lombok.javac.apt.LombokProcessor", false, ourClassLoader).getConstructor().newInstance();
        } catch (Exception e) {
            ;
        } catch (NoClassDefFoundError e) {
            ;
        }


    }




}
