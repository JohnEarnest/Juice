import java.io.*;
import java.util.*;

/**
* These classes represent a tree-walker style interpreter.
* Statements comprise the base-level structure of programs, and
* Nodes are recursively composed to form expression trees.
* The Environment captures everything about the state of the
* program's execution (including IO) and could be inspected
* for debugging purposes if desired.
*
* @author John Earnest
**/

abstract class Statement {
	public abstract void eval(Environment e);
}

abstract class Node {
	public abstract Object eval(Environment e);
}

class Environment {
	public final Scanner in;
	public final PrintStream out;
	public final PrintStream err;
	private final Stack<Map<String, Object>> values = new Stack<Map<String, Object>>();
	Environment(Scanner in, PrintStream out, PrintStream err) {
		this.in  = in;
		this.out = out;
		this.err = err;
		push();
	}
	Environment() { this(null, null, null); }
	public boolean stub() { return this.out == null; }
	public void push() { values.push(new HashMap<String, Object>()); }
	public void pop()  { values.pop(); }
	private Map<String, Object> find(String name) {
		for(int z = values.size() - 1; z >= 0; z--) {
			if (values.get(z).containsKey(name)) { return values.get(z); }
		}
		return null;
	}
	public boolean contains(String name) {
		return find(name) != null;
	}
	public void set(String name, Object value) {
		Map<String, Object> scope = find(name);
		if (scope == null) { scope = values.peek(); }
		scope.put(name, value);
	}
	public Object get(String name) {
		Map<String, Object> scope = find(name);
		if (scope != null) { return scope.get(name); }
		throw new Error("internal error: no binding for '"+name+"'");
	}
}

class Block extends Statement {
	public final List<Statement> body = new ArrayList<Statement>();
	public void eval(Environment e) {
		e.push();
		for(Statement s : body) { s.eval(e); }
		e.pop();
	}
}

class While extends Statement {
	public final Node condition;
	public final Statement body;
	While(Node condition, Statement body) {
		this.condition = condition;
		this.body = body;
	}
	public void eval(Environment e) {
		while((Boolean)condition.eval(e)) {
			body.eval(e);
		}
	}
}

class If extends Statement {
	public final Node condition;
	public final Statement a;
	public final Statement b;
	If(Node condition, Statement a, Statement b) {
		this.condition = condition;
		this.a = a;
		this.b = b;
	}
	public void eval(Environment e) {
		if ((Boolean)condition.eval(e)) { a.eval(e); }
		else if (b != null)             { b.eval(e); }
	}
}

class Print extends Statement {
	public final Node source;
	Print(Node source) {
		this.source = source;
	}
	public void eval(Environment e) {
		if (e.out == null) { return; }
		if (source == null) { e.out.println(); return; }
		e.out.print(source.eval(e));
	}
}

class Assignment extends Statement {
	public final String dest;
	public final Node expression;
	Assignment(String dest, Node expression) {
		this.dest = dest;
		this.expression = expression;
	}
	public void eval(Environment e) {
		e.set(dest, expression.eval(e));
	}
}

class ArrayAssignment extends Assignment {
	public final Node index;
	ArrayAssignment(String dest, Node index, Node expression) {
		super(dest, expression);
		this.index = index;
	}
	public void eval(Environment e) {
		Object l = e.get(dest);
		int    i = (int)index.eval(e);
		Object v = expression.eval(e);
		if      (l instanceof Integer[]) { ((Integer[])l)[i] = (int)    v; }
		else if (l instanceof Float  []) { ((Float  [])l)[i] = (float)  v; }
		else if (l instanceof Boolean[]) { ((Boolean[])l)[i] = (boolean)v; }
		else { throw new Error("internal error: invalid array destination."); }
	}
}

class VariableRef extends Node {
	public final String name;
	VariableRef(String name) { this.name = name; }
	public Object eval(Environment e) { return e.get(name); }
}

class ArrayRef extends VariableRef {
	public final Node index;
	ArrayRef(String name, Node index) {
		super(name);
		this.index = index;
	}
	public Object eval(Environment e) {
		Object l = e.get(name);
		int    i = (int)index.eval(e);
		// if the output source is stubbed out, provide a dummy value of the
		// appropriate type for expression validation:
		if      (l instanceof Integer[]) { return e.stub() ? 1     : (int)    ((Integer[])l)[i]; }
		else if (l instanceof Float  []) { return e.stub() ? 1.0   : (float)  ((Float  [])l)[i]; }
		else if (l instanceof Boolean[]) { return e.stub() ? false : (boolean)((Boolean[])l)[i]; }
		else { throw new Error("internal error: invalid array destination."); }
	}
}

class ArrayInit extends Node {
	public final Node size;
	public final Class type;
	ArrayInit(Node size, Class type) {
		this.size = size;
		this.type = type;
	}
	public Object eval(Environment e) {
		int elements = (int)size.eval(e);
		if      (type == Integer.class) { return new Integer[elements]; }
		else if (type == Float  .class) { return new Float  [elements]; }
		else if (type == Boolean.class) { return new Boolean[elements]; }
		else { throw new Error("internal error: invalid array type '"+type+"'"); }
	}
}

class ArraySize extends Node {
	public final Node source;
	ArraySize(Node source) {
		this.source = source;
	}
	public Object eval(Environment e) {
		Object l = source.eval(e);
		if      (l instanceof Integer[]) { return ((Integer[])l).length; }
		else if (l instanceof Float  []) { return ((Float  [])l).length; }
		else if (l instanceof Boolean[]) { return ((Boolean[])l).length; }
		else { throw new Error("internal error: invalid array source."); }
	}
}

class Constant extends Node {
	public final Object value;
	Constant(Object value) { this.value = value; }
	public Object eval(Environment e) { return value; }
}

class Input extends Node {
	public static final int INT       = 0;
	public static final int HAS_INT   = 1;
	public static final int FLOAT     = 2;
	public static final int HAS_FLOAT = 3;
	public static final int BOOL      = 4;
	public static final int HAS_BOOL  = 5;
	public final int func;
	Input(int func) {
		this.func = func;
	}
	public Object eval(Environment e) {
		switch(func) {
			// If the input source is stubbed out, provide a dummy
			// value of an appropriate type. This allows the parser to
			// perform typechecking by simply doing a test evaluation
			// of each expression:
			case INT:       return e.in == null ? 1     : e.in.nextInt();
			case HAS_INT:   return e.in == null ? false : e.in.hasNextInt();
			case FLOAT:     return e.in == null ? 1.0   : e.in.nextFloat();
			case HAS_FLOAT: return e.in == null ? false : e.in.hasNextFloat();
			case BOOL:      return e.in == null ? false : e.in.nextBoolean();
			case HAS_BOOL:  return e.in == null ? false : e.in.hasNextBoolean();
			default: throw new Error("internal error: invalid input function");
		}
	}
}