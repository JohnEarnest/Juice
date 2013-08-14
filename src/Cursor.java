import java.io.*;
import java.util.*;

/**
* The Cursor maintains the state of the parser and provides
* useful routines for examining the head of the input stream,
* obtaining debug markers and skipping Java comments.
*
* @author John Earnest
**/

public class Cursor {

	private InputStream in;
	private List<Integer> queue = new ArrayList<Integer>();

	private String currentLine = "";
	private int lineNo = 1;
	private int index  = 0;

	Cursor(InputStream in) {
		this.in = in;
		readLine();
		trim();
	}
	Cursor(String source) {
		this(new ByteArrayInputStream(source.getBytes()));
	}
	Cursor(File file) throws FileNotFoundException {
		this(new FileInputStream(file));
	}

	private int read() {
		try {
			return in.read();
		}
		catch(IOException ioe) {
			return -1;
		}
	}

	private int at(int offset) {
		while(queue.size() <= offset) {
			queue.add(read());
		}
		return queue.get(offset);
	}

	public int at() {
		return at(0);
	}

	public boolean eof() {
		return at(0) == -1;
	}


	private void readLine() {
		int index = 0;
		while(at(index) != '\n' && at(index) != -1) {
			currentLine += (char)at(index);
			index++;
		}
	}

	private void skip() {
		if (queue.size() < 1) {
			queue.add(read());
		}
		int c = queue.remove(0);
		if (c == '\n') {
			currentLine = "";
			lineNo++;
			readLine();
			index = 0;
		}
		else {
			index++;
		}
	}

	private void trim() {
		while(true) {
			if (Character.isWhitespace(at())) {
				skip();
				continue;
			}
			if (at(0) == '/' && at(1) == '/') {
				while(at(0) != '\n' && !eof()) {
					skip();
				}
				continue;
			}
			if (at(0) == '/' && at(1) == '*') {
				Mark start = mark();
				skip();
				skip();
				while(at(0) != '*' || at(1) != '/') {
					if (eof()) {
						throw new ParseError("unclosed comment", start);
					}
					skip();
				}
				skip();
				skip();
				continue;
			}
			break;
		}
	}

	private boolean match(String token, boolean separated) {
		for(int z = 0; z < token.length(); z++) {
			if(token.charAt(z) != at(z)) { return false; }
		}
		if (separated && Character.isJavaIdentifierPart(at(token.length()))) { return false; }
		for(int z = 0; z < token.length(); z++) {
			skip();
		}
		trim();
		return true;
	}

	public boolean matchToken(String token) {
		return match(token, true);
	}

	public boolean match(String token) {
		return match(token, false);
	}

	public void expect(String token) {
		if (!match(token)) {
			throw new ParseError("'"+token+"' expected", mark());
		}
	}

	public boolean hasIdentifier() {
		return Character.isJavaIdentifierStart(at());
	}

	private static final List<String> keywords = Arrays.asList(
		"abstract", "assert", "boolean", "break", "byte", "case",
		"catch", "char", "class", "const", "continue", "default",
		"do", "double", "else", "enum", "extends", "final",
		"finally", "float", "for", "goto", "if", "implements",
		"import", "instanceof", "int", "interface", "long",
		"native", "new", "package", "private", "protected",
		"public", "return", "short", "static", "strictfp",
		"super", "switch", "synchronized", "this", "throw",
		"throws", "transient", "try", "void", "volatile",
		"while", "false", "true", "null"
	);

	public String identifier() {
		Mark beforeIdentifier = mark();
		StringBuilder ret = new StringBuilder();
		while(Character.isJavaIdentifierPart(at())) {
			ret.append((char)at());
			skip();
		}
		trim();
		if (keywords.contains(ret.toString())) {
			throw new ParseError("'"+ret.toString()+"' is a reserved keyword, identifier expected.", beforeIdentifier);
		}
		return ret.toString();
	}

	public Object number() {
		if (eof()) {
			throw new ParseError("reached end of file while parsing, number expected", mark());
		}
		String iPart = "";
		if (Character.isDigit(at())) {
			iPart = "";
			while(Character.isDigit(at())) {
				iPart += (char)at();
				skip();
			}
		}
		if (at() == '.') {
			skip();
			String fPart = "";
			while(Character.isDigit(at())) {
				fPart += (char)at();
				skip();
			}
			trim();
			try { return new Float(Float.parseFloat(iPart + "." + fPart)); }
			catch(NumberFormatException e) {
				throw new ParseError("float expected", mark());
			}
		}
		else if (iPart.length() > 0) {
			trim();
			try { return new Integer(Integer.parseInt(iPart)); }
			catch(NumberFormatException e) {
				throw new ParseError("integer expected", mark());
			}
		}
		else {
			throw new ParseError("number expected", mark());
		}
	}

	public String string() {
		skip();
		StringBuilder ret = new StringBuilder();
		while(at() != '"') {
			if (eof()) {
				throw new ParseError("unclosed string literal", mark());
			}
			if (at() == '\\') {
				skip();
				if      (at() == 't' ) { ret.append('\t'); }
				else if (at() == 'n' ) { ret.append('\n'); }
				else if (at() == '"' ) { ret.append('"' ); }
				else if (at() == '\\') { ret.append('\\'); }
				else {
					throw new ParseError("illegal escape character", mark());
				}
			}
			else {
				ret.append((char)at());
			}
			skip();
		}
		skip();
		trim();
		return ret.toString();
	}

	public Mark mark() {
		return new Mark(currentLine, lineNo, index);
	}
}

class Mark {
	public final String line;
	public final int lineNo;
	public final int index;

	Mark(String line, int lineNo, int index) {
		this.line   = line;
		this.lineNo = lineNo;
		this.index  = index;
	}

	public void print(PrintStream out) {
		out.print("\t" + line + "\n\t");
		for(int z = 0; z < index; z++) {
			out.print(line.charAt(z) == '\t' ? '\t' : ' ');
		}
		out.println("^");
	}
}