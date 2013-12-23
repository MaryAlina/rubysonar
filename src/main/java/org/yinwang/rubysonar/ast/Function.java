package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.Binder;
import org.yinwang.rubysonar.Binding;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.ClassType;
import org.yinwang.rubysonar.types.FunType;
import org.yinwang.rubysonar.types.Type;

import java.util.List;


public class Function extends Node {

    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    public Name vararg;  // *args
    public Name kwarg;   // **kwarg
    public Name blockarg = null;   // block arg of Ruby
    public List<Node> afterRest = null;   // after rest arg of Ruby
    public Node body;
    public boolean called = false;
    public boolean isLamba = false;


    public Function(Name name, List<Node> args, Node body, List<Node> defaults,
                    Name vararg, Name kwarg, List<Node> afterRest, Name blockarg,
                    String file, int start, int end)
    {
        super(file, start, end);
        if (name != null) {
            this.name = name;
        } else {
            isLamba = true;
            String fn = genLambdaName();
            this.name = new Name(fn, file, start, start + "lambda".length());
            addChildren(this.name);
        }

        this.args = args;
        this.body = body;
        this.defaults = defaults;
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.afterRest = afterRest;
        this.blockarg = blockarg;
        addChildren(args);
        addChildren(defaults);
        addChildren(afterRest);
        addChildren(name, body, vararg, kwarg, blockarg);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        FunType fun = new FunType(this, s);
        fun.table.setParent(s);
        fun.table.setPath(s.extendPath(name.id));
        fun.setDefaultTypes(resolveList(defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (isLamba) {
            return fun;
        } else {
            if (s.getStateType() == State.StateType.CLASS) {
                if ("initialize".equals(name.id)) {
                    funkind = Binding.Kind.CONSTRUCTOR;
                } else {
                    funkind = Binding.Kind.METHOD;
                }
            } else {
                funkind = Binding.Kind.FUNCTION;
            }

            Type outType = s.type;
            if (outType instanceof ClassType) {
                fun.setCls(outType.asClassType());
            }

            Binder.bind(s, name, fun, funkind);
            return Type.CONT;
        }
    }


    private static int lambdaCounter = 0;


    @NotNull
    public static String genLambdaName() {
        lambdaCounter = lambdaCounter + 1;
        return "lambda%" + lambdaCounter;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Function) {
            Function fo = (Function) obj;
            return (fo.file.equals(file) && fo.start == start);
        } else {
            return false;
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "(func:" + name + ")";
    }

}
