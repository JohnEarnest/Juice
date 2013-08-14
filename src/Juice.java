import java.io.*;
import java.util.*;

/**
* Juice is an interpreter for a highy-restricted subset of
* the Java programming language, appropriately written in Java.
*
* @author John Earnest
**/

public class Juice {

	private static void usage() {
		System.out.println("Usage: juice [-options] sourceFile");
		System.out.println("\t-t <seconds> set execution timeout");
		System.exit(0);
	}

	public static void main(String[] a) {
		List<String> args = new ArrayList<String>(Arrays.asList(a));
		int timeout = 0;
		if (args.size() == 0) { usage(); }
		if ("-t".equals(args.get(0))) {
			args.remove(0);
			try { timeout = Integer.parseInt(args.remove(0)); }
			catch(NumberFormatException e) { System.err.println("bad argument for timeout."); System.exit(1); }
			catch(IndexOutOfBoundsException e) { usage(); }
			
		}
		if (args.size() == 0) { usage(); }
		if (timeout == 0) {
			run(args.get(0));
		}
		else {
			Runner r = new Runner(args.get(0));
			r.start();
			try { r.join(timeout * 1000); }
			catch(InterruptedException e) {}
			System.err.println("program timed out after "+timeout+" seconds. possible infinite loop?");
			System.exit(1);
		}
	}

	public static void run(String filename) {
		try {
			run(
				new FileInputStream(filename),
				new Scanner(System.in),
				System.out,
				System.err
			);
		}
		catch(FileNotFoundException e) {
			System.err.println("unable to open file '"+filename+"'");
		}
		System.exit(0);
	}

	public static void run(InputStream program, Scanner in, PrintStream out, PrintStream err) {
		Cursor cursor = new Cursor(program);
		Environment env = new Environment(in, out, err);
		try {
			Statement root = Parser.parse(cursor);
			root.eval(env);
		}
		catch(ParseError e) {
			err.format("%d: error: %s%n", e.mark.lineNo, e.message);
			e.mark.print(err);
		}
		catch(Error e) {
			cursor.mark().print(err);
			e.printStackTrace();
			System.exit(1);
		}
	}
}

class Runner extends Thread {
	final String filename;
	Runner(String filename) {
		this.filename = filename;
	}
	public void run() {
		Juice.run(filename);
	}
}

class ParseError extends Error {
	public final Mark mark;
	public final String message;

	ParseError(String message, Mark mark) {
		this.message = message;
		this.mark = mark;
	}
}