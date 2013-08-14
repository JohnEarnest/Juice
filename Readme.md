Juice
=====

Juice is an interpreter for a highy-restricted subset of the Java programming language, appropriately written in Java.

What Does Juice Look Like?
--------------------------

It looks like Java:

	int x = 1;
	while(x < 10) {
		print(x);
		println();
		x = x + 1;
	}

Why Juice?
----------

Programming languages are bloody complicated. Java is a popular choice for university-level introductory programming courses, but even a simple "Hello, World" program will require touching on concepts like access modifiers and classes. Juice strips away boilerplate and sophisticated features, allowing beginners to focus on core programming concepts like variables and simple loops. Since Juice programs are restricted to terminal I/O, they are very easy to test in an automated fashion.

If you like the idea of easing students into Java but you want a richer language with more graphical capabilities, have a look at [Processing](http://processing.org).

Features
--------

Assuming you're already familiar with Java it will probably be faster to list some of the things Juice doesn't have:

- Objects
- Classes
- Methods
- Interfaces
- Types other than int, float, boolean, int[], float[] or boolean[]
- Threads
- Generics
- Access modifiers
- Serialization
- Compound assignment operators
- Static initializer blocks
- And more!

Syntax
------

The following is a brief and incomplete BNF description of Juice's syntax:

	PROGRAM     := (STATEMENT)*
	BLOCK       := '{' (STATEMENT)* '}'
	STATEMENT   := BLOCK | WHILE | IF | PRIMITIVE | ASSIGNMENT
	WHILE       := 'while' '(' EXPRESSION ')' BLOCK
	IF          := 'if' '(' EXPRESSION ')' BLOCK ('else' IF)? ('else' BLOCK)?
	PRIMITIVE   := ('print' | 'println') '(' (EXPRESSION)? ')' ';'
	ASSIGNMENT  := (DECLARATION | TARGET) '=' 'EXPRESSION' ';'
	DECLARATION := SCALAR ('[]')? IDENTIFIER
	SCALAR      := ('int' | 'float' | 'boolean')
	TARGET      := IDENTIFIER ('[' EXPRESSION ']')?

EXPRESSIONs are your standard-issue Java expressions with normal precedence and the following operators:

	+ - * / % ! > < >= <= == != (float) (int) && || ()

Additionally, `[]` subscripts or `.length` may immediately follow the name of an array. The keyword 'new' is treated as a unary operator which must be followed by an array declaration as in `new int[9+a]`.

IDENTIFIERS are, again, totally normal Java syntax and may not be any reserved Java keyword.

Strings literals are supported, but the only place they make sense is as arguments to a `print` function. Strings may contain the escape sequences `\n`, `\t`, `\\` and `\"`.

Juice recognizes both single-line `//` comments and multiline `/* ... */` comments.

Built-In Functions
------------------

Juice provides a small collection of intrinsic functions for doing terminal I/O. All of these are meant to resemble using a `java.io.PrintStream` for printing to stdout and a `java.util.Scanner` for reading input from stdin:

- void print(int)
- void print(float)
- void print(boolean)
- void println()
- int nextInt()
- float nextFloat()
- boolean nextBoolean()
- boolean hasNextInt()
- boolean hasNextFloat()
- boolean hasNextBoolean()

Running Programs
----------------

Compile Juice via Ant, put the resulting JAR somewhere safe and then make a nice little bash script for yourself:

	#!/bin/bash
	java -jar /path/to/Juice.jar $@

Pop that in your PATH and running a program is as simple as:

	juice yourFantasticProgram.juice
