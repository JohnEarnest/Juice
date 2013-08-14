import java.io.*;
import java.util.*;

/**
* This is a simple automated test framework which drives Juice
* with an optional input file and validates the contents of
* (optional) output and error output.
*
* John Earnest
**/

public class Test {

	public static void main(String[] args) {
		assertFiles(new File("tests/"));
	}

	public static void assertFiles(File directory) {
		int count = 0;
		for(File f : directory.listFiles()) {
			if (f.getName().endsWith(".juice")) {
				assertFile(f);
				count++;
			}
		}
		System.out.println("All tests passed! ("+count+")");
	}

	private static void compareFiles(String name, File ref, ByteArrayOutputStream data) throws IOException {
		String expected;
		if (ref.exists()) {
			InputStream in = new FileInputStream(ref);
			StringBuilder ins = new StringBuilder();
			while(true) {
				int c = in.read();
				if (c == -1) { break; }
				ins.append((char)c);
			}
			expected = ins.toString().replaceAll("\n", "\n\t");
		}
		else {
			expected = "";
		}
		String observed = data.toString().replaceAll("\n", "\n\t");
		if (!expected.equals(observed)) {
			System.out.println("Test '"+name+"' failed!");
			System.out.println("Expected output:");
			System.out.println("\t"+expected);
			System.out.println("Observed output:");
			System.out.println("\t"+observed);
			System.exit(1);
		}
	}

	public static void assertFile(File source) {
		String basename = source.getName().substring(0, source.getName().indexOf(".juice"));
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			File input = new File(source.getParentFile(), basename + ".in");
			Juice.run(
				new FileInputStream(source),
				input.exists() ? new Scanner(input) : new Scanner(""),
				new PrintStream(out),
				new PrintStream(err)
			);
			compareFiles(basename, new File(source.getParentFile(), basename + ".out"), out);
			compareFiles(basename, new File(source.getParentFile(), basename + ".err"), err);
		}
		catch(IOException e) {
			System.out.println("Test '"+basename+"' failed!");
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
}