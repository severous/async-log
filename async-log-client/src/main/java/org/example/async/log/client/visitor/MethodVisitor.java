package org.example.async.log.client.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class MethodVisitor extends TreeTranslator {

    private TreeMaker treeMaker;

    private Names names;

    public MethodVisitor(TreeMaker treeMaker, Names names) {
        this.treeMaker = treeMaker;
        this.names = names;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        super.visitMethodDef(tree);

//        tree.body = transformMethodBody(tree.body);

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
