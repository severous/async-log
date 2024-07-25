package org.example.async.log.client.annotation;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;


import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;


@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes({"*"})
@AutoService(Processor.class)
public class AsyncLogProcessor extends AbstractProcessor {
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

    private Filer file;


    public static final String COMPILE_TIME_LOG_TEMPLATE_QUALIFIED_NAME = "com.operation.log.common.cache.CompileTimeLogTemplate";


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.javacTrees = JavacTrees.instance(processingEnv);
        this.file = processingEnv.getFiler();

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

        /*
          ListBuffer provides an efficient way to construct and manipulate lists,offering a
          linked list-like structure for better performance in specific scenarios, especially within the compiler.
         */
        ListBuffer<JCTree.JCClassDecl> existedTemplateClass = new ListBuffer<>();
        Set<String> pkgNameSet = new HashSet<>();
        for (Element rootElement : rootElements) {
            JCTree tree = javacTrees.getTree(rootElement);
            if (!(tree instanceof JCTree.JCClassDecl)) {
                continue;
            }

            tree.accept(new TreeTranslator(){

                @Override
                public void visitMethodDef(JCTree.JCMethodDecl tree) {
                    super.visitMethodDef(tree);
                }
            });


        }

        return false;
    }
}
