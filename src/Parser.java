import java.util.*;

/**
* The Parser uses the Cursor to tokenize the input stream and then
* build up a program AST via recursive descent. As expressions and statements
* are assembled, type consistency is checked. The Parser is stateless,
* so it is written with static methods for clarity.
*
* @author John Earnest
**/

class Parser {

	public static Statement parse(Cursor c) {
		Environment e = new Environment();
		Block ret = new Block();
		while(!c.eof()) {
			Statement s = parseStatement(c, e);
			ret.body.add(s);
			if (!(s instanceof If)    &&
			    !(s instanceof While) &&
			    !(s instanceof Block)
			) { c.expect(";"); }
		}
		return ret;
	}

	static Statement parseBlock(Cursor c, Environment e) {
		Block ret = new Block();
		c.expect("{");
		e.push();
		while(!c.match("}")) {
			Statement s = parseStatement(c, e);
			ret.body.add(s);
			if (!(s instanceof If)    &&
			    !(s instanceof While) &&
			    !(s instanceof Block)
			) { c.expect(";"); }
		}
		e.pop();
		return ret;
	}

	static Statement parseStatement(Cursor c, Environment e) {
		if (c.at() == '{')           { return parseBlock(c, e); }
		if (c.matchToken("while"  )) { return parseWhile(c, e); }
		if (c.matchToken("if"     )) { return parseIf(c, e); }
		if (c.matchToken("print"  )) { return parsePrint(c, e, true);  }
		if (c.matchToken("println")) { return parsePrint(c, e, false); }
		if (c.matchToken("int"    )) { return parseAssignment(c, e, Integer.class); }
		if (c.matchToken("float"  )) { return parseAssignment(c, e, Float  .class); }
		if (c.matchToken("boolean")) { return parseAssignment(c, e, Boolean.class); }
		return parseAssignment(c, e, null);
	}

	static Statement parseWhile(Cursor c, Environment e) {
		c.expect("(");
		Node condition = checkType(c, e, Boolean.class);
		c.expect(")");
		Statement body = parseBlock(c, e);
		return new While(condition, body);
	}

	static Statement parseIf(Cursor c, Environment e) {
		c.expect("(");
		Node condition = checkType(c, e, Boolean.class);
		c.expect(")");
		Statement a = parseBlock(c, e);
		Statement b = null;
		if (c.matchToken("else")) {
			b = c.matchToken("if") ? parseIf(c, e) : parseBlock(c, e);
		}
		return new If(condition, a, b);
	}

	static Statement parsePrint(Cursor c, Environment e, boolean val) {
		c.expect("(");
		Node source = null;
		if (val) {
			source = parseExpression(c, e);
			source.eval(e);
		}
		c.expect(")");
		return new Print(source);
	}

	static Statement parseAssignment(Cursor c, Environment e, Class type) {
		// assignments to array?
		// <int>[] x = ...
		// <?>  x    = ...

		// assignments to scalar?
		// <int>x    = ...
		// <?>  x[2] = ...
		// <?>  x    = ...

		// determine the name and type of the assignment target
		Mark beforeBrackets = c.mark();
		if (c.match("[]")) {
			if (type == null) {
				throw new ParseError("illegal start of expression, identifier expected", beforeBrackets);
			}
			type = scalarToArray(type);
		}
		if (!c.hasIdentifier()) {
			throw new ParseError("identifier expected", c.mark());
		}
		Mark beforeIdentifier = c.mark();
		String name = c.identifier();
		if (type == null) {
			if (!e.contains(name)) {
				throw new ParseError("cannot find symbol '"+name+"'", beforeIdentifier);
			}
			type = e.get(name).getClass();
		}

		// parse any subscripts and the assignment source
		Assignment ret;
		Mark beforeSubscript = c.mark();
		if (c.match("[")) {
			if (!isArray(type)) {
				throw new ParseError("array required, but "+typeName(type)+" found", beforeSubscript);
			}
			Node index = checkType(c, e, Integer.class);
			c.expect("]");
			c.expect("=");
			Node source = checkType(c, e, arrayToScalar(type));
			ret = new ArrayAssignment(name, index, source, beforeSubscript);
		}
		else {
			c.expect("=");
			Node source = checkType(c, e, type);
			ret = new Assignment(name, source);
		}
		ret.eval(e);
		return ret;
	}

	/**
	* Expression Parsing:
	**/

	public static final int INT       = 0;
	public static final int HAS_INT   = 1;
	public static final int FLOAT     = 2;
	public static final int HAS_FLOAT = 3;
	public static final int BOOL      = 4;
	public static final int HAS_BOOL  = 5;

	static Node parseParens(Cursor c, Environment e) {
		c.expect("(");
		Node ret = parseExpression(c, e);
		c.expect(")");
		return ret;
	}

	static Node parseInit(Cursor c, Environment e, Class type) {
		c.expect("[");
		Node size = parseExpression(c, e);
		c.expect("]");
		return new ArrayInit(size, type);
	}

	static Node parsePrimary(Cursor c, Environment e) {
		if (c.at() == '(') { return parseParens(c, e); }
		if (c.at() == '"') { return new Constant(c.string()); }
		if (c.match("nextInt"       )) { c.expect("("); c.expect(")"); return new Input(Input.INT      ); }
		if (c.match("hasNextInt"    )) { c.expect("("); c.expect(")"); return new Input(Input.HAS_INT  ); }
		if (c.match("nextFloat"     )) { c.expect("("); c.expect(")"); return new Input(Input.FLOAT    ); }
		if (c.match("hasNextFloat"  )) { c.expect("("); c.expect(")"); return new Input(Input.HAS_FLOAT); }
		if (c.match("nextBoolean"   )) { c.expect("("); c.expect(")"); return new Input(Input.BOOL     ); }
		if (c.match("hasNextBoolean")) { c.expect("("); c.expect(")"); return new Input(Input.HAS_BOOL ); }
		if (c.match("true"          )) { return new Constant(true); }
		if (c.match("false"         )) { return new Constant(false); }
		if (c.hasIdentifier()) {
			Mark beforeIdentifier = c.mark();
			String name = c.identifier();
			if (!e.contains(name)) {
				throw new ParseError("cannot find symbol '"+name+"'", beforeIdentifier);
			}
			Mark beforeSubscript = c.mark();
			if (c.match("[")) {
				Class type = e.get(name).getClass();
				if (!isArray(type)) {
					throw new ParseError("array required, but "+typeName(type)+" found", beforeSubscript);
				}
				Node index = parseExpression(c, e);
				c.expect("]");
				return new ArrayRef(name, index, beforeSubscript);
			}
			else if (c.match(".length")) {
				Class type = e.get(name).getClass();
				if (!isArray(type)) {
					throw new ParseError("array required, but "+typeName(type)+" found", beforeSubscript);
				}
				return new ArraySize(new VariableRef(name));
			}
			else {
				return new VariableRef(name);
			}
		}
		return new Constant(c.number());
	}

	static Node parseEx7(Cursor c, Environment e) {
		Mark m = c.mark();
		if (c.match("+")) { return link(new Add(), m, parseEx7(c, e)); }
		if (c.match("-")) { return link(new Sub(), m, parseEx7(c, e)); }
		return parsePrimary(c, e);
	}

	static Node parseEx6(Cursor c, Environment e) {
		Mark m = c.mark();
		if (c.match("(int)"))   { return link(new ToInt(),   m, parseEx6(c, e)); }
		if (c.match("(float)")) { return link(new ToFloat(), m, parseEx6(c, e)); }
		if (c.match("new")) {
			if      (c.match("int"    )) { return parseInit(c, e, Integer.class); }
			else if (c.match("float"  )) { return parseInit(c, e, Float  .class); }
			else if (c.match("boolean")) { return parseInit(c, e, Boolean.class); }
			else { throw new ParseError("type expected", m); }
		}
		return parseEx7(c, e);
	}

	static Node parseEx5(Cursor c, Environment e) {
		Node ret = parseEx6(c, e);
		Mark m = c.mark();
		if (c.match("*")) { return link(new Mul(), m, ret, parseEx5(c, e)); }
		if (c.match("/")) { return link(new Div(), m, ret, parseEx5(c, e)); }
		if (c.match("%")) { return link(new Rem(), m, ret, parseEx5(c, e)); }
		return ret;
	}

	static Node parseEx4(Cursor c, Environment e) {
		Node ret = parseEx5(c, e);
		Mark m = c.mark();
		if (c.match("+")) { return link(new Add(), m, ret, parseEx4(c, e)); }
		if (c.match("-")) { return link(new Sub(), m, ret, parseEx4(c, e)); }
		return ret;
	}

	static Node parseEx3(Cursor c, Environment e) {
		Node ret = parseEx4(c, e);
		Mark m = c.mark();
		if (c.match("<=")) { return link(new LessEqual(),    m, ret, parseEx3(c, e)); }
		if (c.match(">=")) { return link(new GreaterEqual(), m, ret, parseEx3(c, e)); }
		if (c.match("<" )) { return link(new Less(),         m, ret, parseEx3(c, e)); }
		if (c.match(">" )) { return link(new Greater(),      m, ret, parseEx3(c, e)); }
		return ret;
	}

	static Node parseEx2(Cursor c, Environment e) {
		Node ret = parseEx3(c, e);
		Mark m = c.mark();
		if (c.match("==")) { return link(new Equal(),    m, ret, parseEx2(c, e)); }
		if (c.match("!=")) { return link(new NotEqual(), m, ret, parseEx2(c, e)); }
		return ret;
	}

	static Node parseEx1(Cursor c, Environment e) {
		Node ret = parseEx2(c, e);
		Mark m = c.mark();
		if (c.match("&&")) { return link(new And(), m, ret, parseEx1(c, e)); }
		return ret;
	}

	static Node parseExpression(Cursor c, Environment e) {
		Node ret = parseEx1(c, e);
		Mark m = c.mark();
		if (c.match("||")) { return link(new Or(), m, ret, parseExpression(c, e)); }
		return ret;
	}

	/**
	* Helpers:
	**/

	static String typeName(Class type) {
		if (type == Integer  .class) { return "int"; }
		if (type == Float    .class) { return "float"; }
		if (type == Boolean  .class) { return "boolean"; }
		if (type == Integer[].class) { return "int[]"; }
		if (type == Float  [].class) { return "float[]"; }
		if (type == Boolean[].class) { return "boolean[]"; }
		if (type == String   .class) { return "String"; }
		throw new Error("internal error: unknown type '"+type+"'.");
	}

	static Class arrayToScalar(Class type) {
		if (type == Integer[].class) { return Integer.class; }
		if (type == Float  [].class) { return Float  .class; }
		if (type == Boolean[].class) { return Boolean.class; }
		throw new Error("internal error: cannot convert "+type+" to scalar.");
	}

	static Class scalarToArray(Class type) {
		if (type == Integer.class) { return Integer[].class; }
		if (type == Float  .class) { return Float  [].class; }
		if (type == Boolean.class) { return Boolean[].class; }
		throw new Error("internal error: cannot convert "+type+" to array.");
	}

	static boolean isArray(Class type) {
		return type == Integer[].class ||
		       type == Float  [].class ||
		       type == Boolean[].class;
	}

	static Node checkType(Cursor c, Environment e, Class required) {
		Mark mark = c.mark();
		Node ret = parseExpression(c, e);
		Class found = ret.eval(e).getClass();
		if (!required.equals(found)) {
			throw new ParseError(
				String.format("incompatible types. required %s, found %s",
					typeName(required),
					typeName(found)
				),
				mark
			);
		}
		return ret;
	}

	static Node link(Operator o, Mark mark, Node... nodes) {
		for(Node n : nodes) { o.children.add(n); }
		o.mark = mark;
		return o;
	}
}