import java.io.*;
import java.util.*;

/**
* Operators are unary or binary pure functions which return a result.
* Every primitive operator must specify its behavior for all combinations
* of valid types here.
*
* @author John Earnest
**/

abstract class Operator extends Node {
	public List<Node> children = new ArrayList<Node>();
	public Mark mark = null;

	public Object eval(Environment e) {
		switch(children.size()) {
			case 1: return apply(children.get(0).eval(e));
			case 2: return apply(children.get(0).eval(e), children.get(1).eval(e));
		}
		throw new Error("internal error: operator child count "+children.size());
	}

	private Object apply(Object a) {
		try {	
			if      (a instanceof Integer) { return eval((Integer)a); }
			else if (a instanceof Float  ) { return eval((Float)  a); }
			else if (a instanceof Boolean) { return eval((Boolean)a); }
			else { throw new UnsupportedOperationException(); }
		}
		catch(UnsupportedOperationException e) {
			throw new ParseError(
				String.format("bad operand type %s for unary operator '%s'",
					Parser.typeName(a.getClass()),
					this.toString()
				),
				mark
			);
		}
	}

	protected Object eval(int a)     { throw new UnsupportedOperationException(); }
	protected Object eval(float a)   { throw new UnsupportedOperationException(); }
	protected Object eval(boolean a) { throw new UnsupportedOperationException(); }

	private Object apply(Object a, Object b) {
		try {
			if      (a instanceof Float   && b instanceof Float  ) { return eval((Float)a,   (Float)b  ); }
			else if (a instanceof Integer && b instanceof Float  ) { return eval((Integer)a, (Float)b  ); }
			else if (a instanceof Float   && b instanceof Integer) { return eval((Float)a,   (Integer)b); }
			else if (a instanceof Integer && b instanceof Integer) { return eval((Integer)a, (Integer)b); }
			else if (a instanceof Boolean && b instanceof Boolean) { return eval((Boolean)a, (Boolean)b); }
			else { throw new UnsupportedOperationException(); }
		}
		catch(UnsupportedOperationException e) {
			throw new ParseError(
				String.format("bad operand types %s, %s for binary operator '%s'",
					Parser.typeName(a.getClass()),
					Parser.typeName(b.getClass()),
					this.toString()
				),
				mark
			);
		}
		catch(ArithmeticException e) {
			throw new ParseError("attempted division by zero", mark);
		}
	}

	protected Object eval(int a, int b)         { throw new UnsupportedOperationException(); }
	protected Object eval(float a, float b)     { throw new UnsupportedOperationException(); }
	protected Object eval(boolean a, boolean b) { throw new UnsupportedOperationException(); }

	public abstract String toString();
}

class Add extends Operator {
	protected Object eval(int a) { return a; }
	protected Object eval(float a) { return a; }
	protected Object eval(int a, int b) { return a+b; }
	protected Object eval(float a, float b) { return a+b; }
	public String toString() { return "+"; }
}

class Sub extends Operator {
	protected Object eval(int a) { return -a; }
	protected Object eval(float a) { return -a; }
	protected Object eval(int a, int b) { return a-b; }
	protected Object eval(float a, float b) { return a-b; }
	public String toString() { return "-"; }
}

class Mul extends Operator {
	protected Object eval(int a, int b) { return a*b; }
	protected Object eval(float a, float b) { return a*b; }
	public String toString() { return "*"; }
}

class Div extends Operator {
	protected Object eval(int a, int b) { return a/b; }
	protected Object eval(float a, float b) { return a/b; }
	public String toString() { return "/"; }
}

class Rem extends Operator {
	protected Object eval(int a, int b) { return a%b; }
	protected Object eval(float a, float b) { return a%b; }
	public String toString() { return "%"; }
}

class Not extends Operator {
	protected Object eval(boolean a) { return !a; }
	public String toString() { return "!"; }
}

class Greater extends Operator {
	protected Object eval(int a, int b) { return a>b; }
	protected Object eval(float a, float b) { return a>b; }
	public String toString() { return ">"; }
}

class Less extends Operator {
	protected Object eval(int a, int b) { return a<b; }
	protected Object eval(float a, float b) { return a<b; }
	public String toString() { return "<"; }
}

class GreaterEqual extends Operator {
	protected Object eval(int a, int b) { return a>=b; }
	protected Object eval(float a, float b) { return a>=b; }
	public String toString() { return ">="; }
}

class LessEqual extends Operator {
	protected Object eval(int a, int b) { return a<=b; }
	protected Object eval(float a, float b) { return a<=b; }
	public String toString() { return "<="; }
}

class Equal extends Operator {
	protected Object eval(int a, int b) { return a==b; }
	protected Object eval(float a, float b) { return a==b; }
	protected Object eval(boolean a, boolean b) { return a==b; }
	public String toString() { return "=="; }
}

class NotEqual extends Operator {
	protected Object eval(int a, int b) { return a!=b; }
	protected Object eval(float a, float b) { return a!=b; }
	protected Object eval(boolean a, boolean b) { return a!=b; }
	public String toString() { return "!="; }
}

class ToInt extends Operator {
	protected Object eval(int a) { return a; }
	protected Object eval(float a) { return (int)a; }
	public String toString() { return "(int)"; }
}

class ToFloat extends Operator {
	protected Object eval(int a) { return (float)a; }
	protected Object eval(float a) { return a; }
	public String toString() { return "(float)"; }
}

/**
* Short-circuiting is a little interesting.
* I can modify the semantics of an operator's evaluation
* quite easily by overloading eval(), but doing so loses the
* type-validation machinery in the base implementation and
* when we're evaluating expressions to validate types we *don't*
* want to short circuit anything. Thus, && and || fall back
* to a bitwise (boolean-only) & or | during a validation pass:
**/

class And extends Operator {
	public Object eval(Environment e) {
		if (e.stub()) { super.eval(e); }
		if (!(Boolean)children.get(0).eval(e)) { return false; }
		if (!(Boolean)children.get(1).eval(e)) { return false; }
		return true;
	}
	protected Object eval(boolean a, boolean b) { return a&b; }
	public String toString() { return "&&"; }
}

class Or extends Operator {
	public Object eval(Environment e) {
		if (e.stub()) { super.eval(e); }
		if ((Boolean)children.get(0).eval(e)) { return true; }
		if ((Boolean)children.get(1).eval(e)) { return true; }
		return false;
	}
	protected Object eval(boolean a, boolean b) { return a|b; }
	public String toString() { return "||"; }
}