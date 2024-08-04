package org.example.async.log.client.annotation;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.example.async.log.client.patcher.ClassRootFinder;
import org.example.async.log.client.permit.Permit;


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

    public AsyncLogAnnotationProcessor() {
    }

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
            tree.accept(new TreeTranslator(){
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl tree) {
                    super.visitMethodDef(tree);
                    tree.body = transformMethodBody(tree.getBody());
                }
            });
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


    private JCTree.JCBlock transformMethodBody(JCTree.JCBlock body) {
        List<JCTree.JCStatement> statements = List.nil();
        for (JCTree.JCStatement statement : body.getStatements()) {
            if (statement instanceof JCTree.JCExpressionStatement) {
                JCTree.JCExpressionStatement exprStmt = (JCTree.JCExpressionStatement) statement;
                if (isLogInfoCall(exprStmt)) {
                    statements = statements.append(createAsyncLogCall(exprStmt));
                    continue;
                }
            }
            statements = statements.append(statement);
        }
        return treeMaker.Block(0, statements);
    }

    private boolean isLogInfoCall(JCTree.JCExpressionStatement exprStmt) {
        if (exprStmt.expr instanceof JCTree.JCMethodInvocation) {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) exprStmt.expr;
            if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodInvocation.meth;
                return fieldAccess.selected.toString().equals("log") && fieldAccess.name.toString().equals("info");
            }
        }
        return false;
    }

    private JCTree.JCStatement createAsyncLogCall(JCTree.JCExpressionStatement logCall) {
        JCTree.JCExpression logCallExpr = logCall.expr;
        JCTree.JCExpression execServiceExpr = treeMaker.Ident(names.fromString("Executors"));
        execServiceExpr = treeMaker.Select(execServiceExpr, names.fromString("newVirtualThreadPerTaskExecutor"));
        JCTree.JCMethodInvocation execServiceCall = treeMaker.Apply(
                List.nil(),
                execServiceExpr,
                List.nil()
        );

        JCTree.JCVariableDecl execServiceDecl = treeMaker.VarDef(
                treeMaker.Modifiers(0),
                names.fromString("executor"),
                treeMaker.Ident(names.fromString("ExecutorService")),
                execServiceCall
        );

        JCTree.JCExpression execServiceIdent = treeMaker.Ident(execServiceDecl.name);
        JCTree.JCExpression submitExpr = treeMaker.Select(execServiceIdent, names.fromString("submit"));

        JCTree.JCLambda logLambda = treeMaker.Lambda(
                List.nil(),
                logCallExpr
        );

        JCTree.JCMethodInvocation submitCall = treeMaker.Apply(
                List.nil(),
                submitExpr,
                List.of(logLambda)
        );

        JCTree.JCExpressionStatement submitStmt = treeMaker.Exec(submitCall);
        return treeMaker.Block(0, List.of(execServiceDecl, submitStmt));
    }









}
