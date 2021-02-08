import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilersProject5 {
	static int i = 0;
	static ArrayList<Token> arrList = new ArrayList<Token>();

	// SEMANTICS
	// Initialize Semantic Analyzer values
	static ArrayList<LinkedHashMap<String, String>> variableList = new ArrayList<LinkedHashMap<String, String>>();
	static ArrayList<LinkedHashMap<String, String>> functionList = new ArrayList<LinkedHashMap<String, String>>();
	static ArrayList<LinkedHashMap<String, String>> paramList = new ArrayList<LinkedHashMap<String, String>>();
	static int scopeCount = 0;
	static int returnCount = 0;
	static ArrayList<String> returnType = new ArrayList<String>();
	static boolean returnGood = true;
	static int paramCount = 0;
	static int paramSetNumber = -1;
	static ArrayList<Integer> paramSet = new ArrayList<Integer>();
	static ArrayList<String> functionSet = new ArrayList<String>();
	static int sizeOfParams = 0;
	static int paramCounter = 0;
	static boolean insideParam = false;
	static String currentFunc;

	// codegen
	static int codeLine = 0;
	// static char[] code = new char[100];
	static String op = new String();
	static String opnd1 = new String();
	static String opnd2 = new String();
	static String opre = new String();
	static ArrayList<Generator> codeGen = new ArrayList<Generator>();
	static int operand1 = 0;
	static int funcCounter = 0;
	static int BPW = 0;
	static int valBPW = 0;
	static int BPO = 0;
	static int valBPO = 0;

	public static void main(String[] args) throws Exception {
		// SEMANTICS
		// global symbol table
		variableList.add(new LinkedHashMap<String, String>());
		functionList.add(new LinkedHashMap<String, String>());

		// command line argument for fileName

		// ArrayList to store tokens for Parser in sequential order (token = type of
		// token and name of token)

		///// try-catch block
		///////////
		try {
			File file = new File(args[0]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String input;
			Boolean blockComment = false;

			// PATTERNS
			Pattern commentBlockEnd = Pattern.compile("^\\*/");
			Pattern commentBlockEndSameLine = Pattern.compile("\\*/");
			Pattern commentBlockStart = Pattern.compile("^/\\*");
			Pattern commentLine = Pattern.compile("^//");
			Pattern returnKW = Pattern.compile("^return");
			Pattern whileKW = Pattern.compile("^while");
			Pattern voidKW = Pattern.compile("^void");
			Pattern elseKW = Pattern.compile("^else");
			Pattern intKW = Pattern.compile("^int");
			Pattern ifKW = Pattern.compile("^if");
			Pattern symbol1 = Pattern.compile("^==|^<=|^>=|^!=");
			Pattern symbol2 = Pattern.compile("^\\+|^\\-|^\\*|^/|^=|^<|^>|^;|^,|^\\(|^\\)|^\\[|^\\]|^\\{|^\\}");
			Pattern id = Pattern.compile("(^[a-zA-Z]+)");
			Pattern error = Pattern.compile(
					"^([a-zA-Z]|[\\d]|\\+|^\\-|^\\*|^/|^=|^<|^>|^;|^,|^\\(|^\\)|^\\[|^\\]|^\\{|^\\}|==|^<=|^>=|^!=)");
			Pattern integer = Pattern.compile("^(\\d+)");
			Pattern kwID = Pattern.compile(
					"^return[a-zA-Z]*[\\d]+|^return[\\d]*[a-zA-Z]+|^while[a-zA-Z]*[\\d]+|^while[\\d]*[a-zA-Z]+|^void[a-zA-Z]*[\\d]+|^void[\\d]*[a-zA-Z]+|^else[a-zA-Z]*[\\d]+|^else[\\d]*[a-zA-Z]+|^int[a-zA-Z]*[\\d]+|^int[\\d]*[a-zA-Z]+|^if[a-zA-Z]*[\\d]+|^if[\\d]*[a-zA-Z]+");
			// matcher for checking regex
			Matcher check;
			// index for modifying substrings
			int matchPos;

			while ((input = br.readLine()) != null) {
				// iterates to next line if there is nothing but blank space on a Line.
				if (input.isEmpty()) {
					continue;
				}

				// only prints line input if there is an input on the line besides blank space
				if (!input.trim().isEmpty()) {
					// System.out.println("INPUT: " + input);
				}
				outerLoop: while (input != null) {
					// trims whitespace from front and back of string
					input = input.trim();

					// iterates to next line if there is nothing but blank space on a Line.
					if (input.isEmpty()) {
						break outerLoop;
					}

					// checks for end of comment block */
					if (blockComment == true) {
						check = commentBlockEnd.matcher(input);
						if (check.find()) {
							// upon finding */, toggles blockComment to false and moves string forwards
							blockComment = false;
							matchPos = check.end();
							input = input.substring(matchPos);
							continue;
						}
					}

					// checks for start of comment block /*
					check = commentBlockStart.matcher(input);
					if (check.find()) {
						// upon finding /*, toggles blockComment to true and moves string forwards
						blockComment = true;
						matchPos = check.end();
						input = input.substring(matchPos);
						continue;
					}

					// if inside BlockComment, logic to try to find another */ on the line
					if (blockComment == true) {
						// check if the next term ends the comment block */
						check = commentBlockEnd.matcher(input);
						// while the next term is not the end comment block,
						while (!check.find()) {
							// check if the entire line contains an end of comment block
							check = commentBlockEndSameLine.matcher(input);
							// if no */ exists on the entire line, the entire line is a comment
							if (!check.find()) {
								break outerLoop;
							}
							// if */ does exist on the line, it will ignore the string up to that point
							matchPos = check.start();
							input = input.substring(matchPos);
							// then check again for the end of comment block
							check = commentBlockEnd.matcher(input);
						}
						continue;
					}

					// checks if the string begins with //, then the entire line is a comment
					check = commentLine.matcher(input);
					if (check.find()) {
						break outerLoop;
					}

					// regex for ID starting with a KW
					check = kwID.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("ID: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("id", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = kwID.matcher(input);
						continue;
					}

					// KW regexes
					check = returnKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("return", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = returnKW.matcher(input);
						continue;
					}

					check = whileKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("while", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = whileKW.matcher(input);
						continue;
					}

					check = voidKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("void", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = voidKW.matcher(input);
						continue;
					}

					check = elseKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("else", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = elseKW.matcher(input);
						continue;
					}

					check = intKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("int", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = intKW.matcher(input);
						continue;
					}

					check = ifKW.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("KW: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("if", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = ifKW.matcher(input);
						continue;
					}

					// ID regexes
					check = id.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("ID: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("id", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = id.matcher(input);
						continue;
					}

					// 2-length symbol regexes
					check = symbol1.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println(input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("symbol", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = symbol1.matcher(input);
						continue;
					}

					// 1-length symbol regexes
					check = symbol2.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println(input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("symbol", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = symbol2.matcher(input);
						continue;
					}

					// int regexes
					check = integer.matcher(input);
					if (check.find()) {
						matchPos = check.end();
						// System.out.println("INT: " + input.substring(check.start(), matchPos));
						// add to arraylist
						arrList.add(new Token("num", input.substring(check.start(), matchPos)));
						input = input.substring(matchPos);
						input = input.trim();
						check = integer.matcher(input);
						continue;
					}

					check = error.matcher(input);
					if (!check.find()) {
						// matchPos = check.end();
						// System.out.println("ERROR: " + input.substring(0, 1));
						// add to arraylist
						arrList.add(new Token("error", input.substring(0, 1)));
						input = input.substring(1);
						input = input.trim();
						check = error.matcher(input);
						continue;
					}

					continue;

				} // end of while- line empty
			} ///////////////////// end of while- no more lines to read

			arrList.add(new Token("EOF", "$"));
			// System.out.println(arrList.get(0).tokenValue);
			// if (arrList.get(0).tokenValue.equals("int")) {
			// System.out.println("true");
			// }

			program();
			// SEMANTICS
			// Ensure main function exists
			if (!functionList.get(scopeCount).containsKey("main")) {
				//System.out.println("REJECT");
				//System.exit(0);
			}

			if (arrList.get(i).tokenValue == "$") {
				// System.out.println("ACCEPT");

				// Code Gen
				codeLine = 0;
				int genSize = codeGen.size();

				if (codeGen.get(codeLine).op != null) {
					while (codeGen.get(codeLine).op != null) {
						System.out.print(codeLine + 1);
						System.out.print("        ");
						System.out.print(codeGen.get(codeLine).op);
						System.out.print("             ");
						System.out.print(codeGen.get(codeLine).opnd1);
						System.out.print("             ");
						System.out.print(codeGen.get(codeLine).opnd2);
						System.out.print("             ");
						System.out.println(codeGen.get(codeLine).opre);
						codeLine++;
						if (genSize - 1 == codeLine) {
							break;
						}
					}
				}

				System.out.print(codeLine + 1);
				System.out.print("        ");
				System.out.print(codeGen.get(codeLine).op);
				System.out.print("             ");
				System.out.print(codeGen.get(codeLine).opnd1);
				System.out.print("             ");
				System.out.print(codeGen.get(codeLine).opnd2);
				System.out.print("             ");
				System.out.print(codeGen.get(codeLine).opre);
			} else {
				System.out.println("REJECT");
			}

		} catch (FileNotFoundException e) {
			System.out.println("File does not exist");
			System.exit(0);
		}
	} //////////////////////////// end of main

	// A
	static void program() {
		// B
		declarationList(); // B
	}

	// B
	static void declarationList() {
		// C B'
		// C B'
		declaration(); // C
		declarationListPrime(); // B'
	}

	// B'
	static void declarationListPrime() {
		// C B' | empty
		// C B'
		if (arrList.get(i).tokenValue.equals("int") || arrList.get(i).tokenValue.equals("void")) {
			declaration(); // C
			declarationListPrime(); // B'
		}
	}

	// C
	static void declaration() {
		// int id C' | void id C'
		// int id C'
		if (arrList.get(i).tokenValue.equals("int")) {
			// SEMANTICS
			// FUNCTION DECS
			// if redeclaring same variable in same scope, REJECT
			// (unless the next next token is ( for a variable
			if (arrList.get(i + 2).tokenValue.equals(";") || arrList.get(i + 2).tokenValue.equals(",")
					|| arrList.get(i + 1).tokenValue.equals(")")) {
				if (variableList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add function dec to Hash Scope 0
				variableList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);
			} else if (arrList.get(i + 2).tokenValue.equals("[")) { // type array
				if (variableList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add function dec to Hash Scope 0
				variableList.get(scopeCount).put(arrList.get(i + 1).tokenValue, "array");
				// increment return count
				returnCount++;
				returnType.add("array");
			} else if (arrList.get(i + 2).tokenValue.equals("(")) {
				// SEMANTICS
				// Ensure main is last function
				if (functionList.get(0).containsKey("main")) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// SEMANTICS
				// Scope already contains declaration
				if (functionList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add function dec to Hash Scope 0
				functionList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);
				// function set
				functionSet.add(arrList.get(i + 1).tokenValue);
				paramList.add(new LinkedHashMap<String, String>());
				// increment return count
				returnCount++;
				returnType.add(arrList.get(i).tokenType);
				// SEMANTICS
				returnGood = false;
			}

			// SEMANTICS
			// Ensure main is last function
			if (functionList.get(0).containsKey("main") && scopeCount == 0) {
				//System.out.println("REJECT");
				//System.exit(0);
			}

			i++; // int
			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id
				declarationPrime(); // C'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // void id C'
		else if (arrList.get(i).tokenValue.equals("void")) {
			// SEMANTICS
			// add function dec to Hash Scope 0
			if (arrList.get(i + 2).tokenValue.equals(";") || arrList.get(i + 2).tokenValue.equals(",")
					|| arrList.get(i + 2).tokenValue.equals("[") || arrList.get(i + 1).tokenValue.equals(")")) {
				variableList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);
			} else if (arrList.get(i + 2).tokenValue.equals("(")) {
				// SEMANTICS
				// Ensure main is last function
				if (functionList.get(0).containsKey("main")) {
					//System.out.println("REJECT");
					//System.exit(0);
				} // no duplicates
				if (functionList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				functionList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);
				// function set
				functionSet.add(arrList.get(i + 1).tokenValue);
				paramList.add(new LinkedHashMap<String, String>());
				// increment return count
				returnCount++;
				returnType.add(arrList.get(i).tokenType);
			}

			// SEMANTICS
			// Ensure main is last function
			if (functionList.get(0).containsKey("main") && scopeCount == 0
					&& !arrList.get(i + 2).tokenValue.equals("(")) {
				//System.out.println("REJECT");
				//System.exit(0);
			}

			// code gen
//			codeGen.add(new Generator(op, opnd1, opnd2, opre));
//			codeGen.get(codeLine).op = "func";
//			codeGen.get(codeLine).opnd1 = arrList.get(i + 1).tokenValue;
//			codeGen.get(codeLine).opnd2 = arrList.get(i).tokenValue;
//			if (codeGen.get(codeLine).opnd1.equals("void")) {
//				codeGen.get(codeLine).opre = "0";
//			} else {
//				codeGen.get(codeLine).opre = "1";
//			}
//			codeLine++;

			i++; // int
			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id
				declarationPrime(); // C'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// C'
	static void declarationPrime() {
		// D' | (G) J
		// (G) J
		if (arrList.get(i).tokenValue.equals("(")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "func";
			codeGen.get(codeLine).opnd1 = arrList.get(i - 1).tokenValue;
			codeGen.get(codeLine).opnd2 = arrList.get(i - 2).tokenValue;
			if (codeGen.get(codeLine).opnd1.equals("void")) {
				codeGen.get(codeLine).opre = "0";
			} else {
				codeGen.get(codeLine).opre = "1";
			}
			codeLine++;

			i++; // (
			// SEMANTICS
			// paramsetNumber
			paramSetNumber++;

			params(); // G
			if (arrList.get(i).tokenValue.equals(")")) {
				// SEMANTICS
				// param positioning
				paramSet.add(paramSetNumber, paramCount);
				paramCount = 0;
				i++; // )
				compoundStatement(); // J
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // D'
		else if (arrList.get(i).tokenValue.equals(";") || arrList.get(i).tokenValue.equals("[")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "alloc";
			codeGen.get(codeLine).opnd1 = "4";
			codeGen.get(codeLine).opnd2 = "     ";
			codeGen.get(codeLine).opre = arrList.get(i - 1).tokenValue;
			codeLine++;

			variableDeclarationPrime(); // D'
		}
		// fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
		// SEMANTICS
		if (returnGood == false) {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// D
	static void variableDeclaration() {
		// int id D' | void id D'
		// int id D'
		if (arrList.get(i).tokenValue.equals("int")) {
			// SEMANTICS
			// VARIABLE DECS
			// if redeclaring same variable in same scope, REJECT
			if (arrList.get(i + 2).tokenValue.equals(";") || arrList.get(i + 2).tokenValue.equals(",")
					|| arrList.get(i + 1).tokenValue.equals(")")) {
				if (variableList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add int dec to HashMap with key = ID
				variableList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);

				// code gen
				codeGen.add(new Generator(op, opnd1, opnd2, opre));
				codeGen.get(codeLine).op = "alloc";
				codeGen.get(codeLine).opnd1 = "4";
				codeGen.get(codeLine).opnd2 = "     ";
				codeGen.get(codeLine).opre = arrList.get(i + 1).tokenValue;
				codeLine++;

			} else if (arrList.get(i + 2).tokenValue.equals("(")) {
				if (functionList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add int dec to HashMap with key = array
				functionList.get(scopeCount).put(arrList.get(i + 1).tokenValue, arrList.get(i).tokenType);
			} else if (arrList.get(i + 2).tokenValue.equals("[")) {
				if (variableList.get(scopeCount).containsKey(arrList.get(i + 1).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				variableList.get(scopeCount).put(arrList.get(i + 1).tokenValue, "array");

				// code gen
				codeGen.add(new Generator(op, opnd1, opnd2, opre));
				codeGen.get(codeLine).op = "alloc";
				codeGen.get(codeLine).opnd1 = "4";
				codeGen.get(codeLine).opnd2 = "     ";
				codeGen.get(codeLine).opre = arrList.get(i + 1).tokenValue;
				codeLine++;

			}

			// System.out.println(scopeCount);
			// System.out.println(variableList.get(scopeCount).values());
			// System.out.println(variableList.get(scopeCount).keySet());

			i++; // int
			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id
				variableDeclarationPrime(); // D'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // void id D'
		else if (arrList.get(i).tokenValue.equals("void")) {
			// SEMANTICS
			// can't declare variable void
			if (arrList.get(i + 1).tokenType.equals("id")) {
				//System.out.println("REJECT");
				//System.exit(0);
			}

			i++; // void
			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id
				variableDeclarationPrime(); // D'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// D'
	static void variableDeclarationPrime() {
		// ; | [num] ;
		// ;
		if (arrList.get(i).tokenValue.equals(";")) {
			i++; // ;
		} // [num] ;
		else if (arrList.get(i).tokenValue.equals("[")) {
			i++; // [
			if (arrList.get(i).tokenType.equals("num")) {
				i++; // num
				if (arrList.get(i).tokenValue.equals("]")) {
					i++; // ]
					if (arrList.get(i).tokenValue.equals(";")) {
						i++; // ;
					} // fails
					else {
						//System.out.println("REJECT");
						//System.exit(0);
					}
				} // fails
				else {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// E
	static void typeSpecifier() {
		// int | void not used
	}

	// F
	static void functionDeclaration() {
		// E id (G) J not used
	}

	// G
	static void params() {
		// int id I' H' | void G'
		// int id I' H'
		if (arrList.get(i).tokenValue.equals("int")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "param";
			codeGen.get(codeLine).opnd1 = "    ";
			codeGen.get(codeLine).opnd2 = "    ";
			codeGen.get(codeLine).opre = arrList.get(i + 1).tokenValue;

			codeLine++;

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "alloc";
			codeGen.get(codeLine).opnd1 = "4";
			codeGen.get(codeLine).opnd2 = "      ";
			codeGen.get(codeLine).opre = arrList.get(i + 1).tokenValue;

			codeLine++;

			// SEMANTICS
			// main args should be void
			if (arrList.get(i - 2).tokenValue.equals("main")) {
				//System.out.println("REJECT");
				//System.exit(0);
			}

			i++; // int

			// SEMANTICS
			//
			variableList.add(new LinkedHashMap<String, String>());
			// if redeclaring same variable in same scope, REJECT
			if (arrList.get(i + 1).tokenValue.equals(";") || arrList.get(i + 1).tokenValue.equals(",")
					|| arrList.get(i + 1).tokenValue.equals(")")) {
				if (variableList.get(scopeCount + 1).containsKey(arrList.get(i).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add int token to HashMap with key = ID
				variableList.get(scopeCount + 1).put(arrList.get(i).tokenValue, arrList.get(i - 1).tokenType);
			} else if (arrList.get(i + 1).tokenValue.equals("(")) {
				if (functionList.get(scopeCount + 1).containsKey(arrList.get(i).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				// add int token to HashMap with key = array
				functionList.get(scopeCount + 1).put(arrList.get(i).tokenValue, arrList.get(i - 1).tokenType);
			} else if (arrList.get(i + 1).tokenValue.equals("[")) {
				if (variableList.get(scopeCount + 1).containsKey(arrList.get(i).tokenValue)) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				variableList.get(scopeCount + 1).put(arrList.get(i).tokenValue, "array");
			}

			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id

				// SEMANTICS
				// int param
				if (!arrList.get(i).tokenValue.equals("[")) {
					// paramList.add(new LinkedHashMap<String, String>());
					paramList.get(paramSetNumber).put(arrList.get(i - 1).tokenValue, "int");
					paramCount++;
				}

				paramPrime(); // I'
				paramListPrime(); // H'
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // void G'
		else if (arrList.get(i).tokenValue.equals("void")) {
			// SEMANTICS
			// void parameter shouldn't have a value
			if (!(arrList.get(i + 1).tokenValue.equals(")"))) {
				//System.out.println("REJECT");
				//System.exit(0);
			}
			i++; // void
			paramsPrime(); // G'
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}

	}

	// G'
	static void paramsPrime() {
		// id I' H' | empty
		if (arrList.get(i).tokenType.equals("id")) {
			i++; // id
			paramPrime(); // I'
			paramListPrime(); // H'
		}
	}

	// H
	static void paramList() {
		// I H' not used
		// param(); // I
		// paramListPrime(); // H'
	}

	// H'
	static void paramListPrime() {
		// , I H' | empty
		if (arrList.get(i).tokenValue.equals(",")) {
			i++; // ,

			// SEMANTICS

			param(); // I
			paramListPrime(); // H'
		}
	}

	// I
	static void param() {
		// int id I' | void id I'
		// int id I'
		if (arrList.get(i).tokenValue.equals("int")) {
			i++; // int

			// SEMANTICS
			//
			variableList.add(new LinkedHashMap<String, String>());
			functionList.add(new LinkedHashMap<String, String>());

			// add int token to HashMap with key = ID
			if (arrList.get(i + 1).tokenValue.equals(";") || arrList.get(i + 1).tokenValue.equals(",")
					|| arrList.get(i + 1).tokenValue.equals(")")) {
				variableList.get(scopeCount + 1).put(arrList.get(i).tokenValue, arrList.get(i - 1).tokenType);
			} else if (arrList.get(i + 1).tokenValue.equals("(")) {
				functionList.get(scopeCount + 1).put(arrList.get(i).tokenValue, arrList.get(i - 1).tokenType);
			} else if (arrList.get(i + 1).tokenValue.equals("[")) {
				variableList.get(scopeCount + 1).put(arrList.get(i).tokenValue, "array");
			}

			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id

				// SEMANTICS
				// int param
				if (!arrList.get(i).tokenValue.equals("[")) {
					// paramList.add(new LinkedHashMap<String, String>());
					paramList.get(paramSetNumber).put(arrList.get(i - 1).tokenValue, "int");
					paramCount++;
				}

				paramPrime(); // I'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // void id I'
		else if (arrList.get(i).tokenValue.equals("void")) {
			i++; // void
			if (arrList.get(i).tokenType.equals("id")) {
				i++; // id
				paramPrime(); // I'
			} else { // fails
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// I'
	static void paramPrime() {
		// empty | []
		// []
		if (arrList.get(i).tokenValue.equals("[")) {
			// SEMANTICS
			// array param
			if (arrList.get(i).tokenValue.equals("[")) {
				// paramList.add(new LinkedHashMap<String, String>());
				paramList.get(paramSetNumber).put(arrList.get(i - 1).tokenValue, "array");
				paramCount++;
			}

			i++; // [
			if (arrList.get(i).tokenValue.equals("]")) {
				i++; // ]
			}
		}
	}

	// J
	static void compoundStatement() {
		// { K L }
		if (arrList.get(i).tokenValue.equals("{")) {
			// SEMANTICS
			// add new scope
			scopeCount++;
			variableList.add(new LinkedHashMap<String, String>());
			functionList.add(new LinkedHashMap<String, String>());

			i++; // {
			localDeclaration(); // K
			statementList(); // L
			if (arrList.get(i).tokenValue.equals("}")) {

				// SEMANTICS
				// delete scope for sem-an
				variableList.get(scopeCount).clear();
				functionList.get(scopeCount).clear();
				scopeCount--;

				// code gen
				if (scopeCount != 0) {
					codeGen.add(new Generator(op, opnd1, opnd2, opre));
					codeGen.get(codeLine).op = "   BR";
					codeGen.get(codeLine).opnd1 = "    ";
					codeGen.get(codeLine).opnd2 = "    ";

					valBPW = codeLine;

					codeGen.get(codeLine).opre = Integer.toString(BPW + 1);
					codeGen.get(codeLine).opre += "     val of BPW";

					codeGen.get(BPW).opre = Integer.toString(valBPW + 1);
					codeGen.get(BPW).opre += "      BPW = ";
					codeGen.get(BPW).opre += Integer.toString(BPW + 1);

					codeLine++;
				}

				if (scopeCount == 0) {

					codeGen.add(new Generator(op, opnd1, opnd2, opre));
					codeGen.get(codeLine).op = " end";
					codeGen.get(codeLine).opnd1 = "func";
					codeGen.get(codeLine).opnd2 = functionSet.get(funcCounter);
					// codeGen.get(codeLine).opnd2 =
					// functionList.get(funcCounter).get(functionSet.get(funcCounter));

					codeGen.get(codeLine).opre = " ";

					valBPO = codeLine;

					codeGen.get(codeLine).opre = Integer.toString(BPO + 1);
					codeGen.get(codeLine).opre += "     val of BPO";

					codeGen.get(BPO).opre = Integer.toString(valBPO + 1);
					codeGen.get(BPO).opre += "      BPO = ";
					codeGen.get(BPO).opre += Integer.toString(BPO + 1);

					codeLine++;
					funcCounter++;
				}

				i++; // }
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// K
	static void localDeclaration() {
		// K'
		localDeclarationPrime(); // K'
	}

	// K'
	static void localDeclarationPrime() {
		// D K' | empty
		// D K'
		if (arrList.get(i).tokenValue.equals("int") || arrList.get(i).tokenValue.equals("void")) {
			variableDeclaration(); // D
			localDeclarationPrime(); // K'
		}
	}

	// L
	static void statementList() {
		// L'
		statementListPrime(); // L'
	}

	// L'
	static void statementListPrime() {
		// M L' | empty
		// M L'
		if (arrList.get(i).tokenValue.equals(";") || arrList.get(i).tokenType.equals("id")
				|| arrList.get(i).tokenValue.equals("(") || arrList.get(i).tokenType.equals("num")
				|| arrList.get(i).tokenValue.equals("{") || arrList.get(i).tokenValue.equals("if")
				|| arrList.get(i).tokenValue.equals("while") || arrList.get(i).tokenValue.equals("return")) {
			statement(); // M
			statementListPrime(); // L'
		}
	}

	// M
	static void statement() {
		// N | J | O | P | Q
		// N
		if (arrList.get(i).tokenValue.equals(";") || arrList.get(i).tokenType.equals("id")
				|| arrList.get(i).tokenType.equals("num") || arrList.get(i).tokenValue.equals("(")) {
			expressionStatement(); // N
		} // J
		else if (arrList.get(i).tokenValue.equals("{")) {
			compoundStatement(); // J
		} // O
		else if (arrList.get(i).tokenValue.equals("if")) {
			// code gen
			BPW = codeLine;

			selectionStatement(); // O
		} // P
		else if (arrList.get(i).tokenValue.equals("while")) {
			// code gen
			BPW = codeLine;

			iterationStatement(); // P
		} // Q
		else if (arrList.get(i).tokenValue.equals("return")) {
			// SEMANTICS
			returnGood = true;
			returnStatement(); // Q
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// N
	static void expressionStatement() {
		// R ; | ;
		// R ;
		if (arrList.get(i).tokenType.equals("id") || arrList.get(i).tokenType.equals("num")
				|| arrList.get(i).tokenValue.equals("(")) {

			// code gen
			if (arrList.get(i + 2).tokenType.equals("id") && arrList.get(i + 3).tokenValue.equals("(")) {
				codeGen.add(new Generator(op, opnd1, opnd2, opre));
				codeGen.get(codeLine).op = "arg";
				codeGen.get(codeLine).opnd1 = "    ";
				codeGen.get(codeLine).opnd2 = "    ";
				codeGen.get(codeLine).opre = arrList.get(i + 4).tokenValue;

				codeLine++;

				codeGen.add(new Generator(op, opnd1, opnd2, opre));
				codeGen.get(codeLine).op = "call";
				codeGen.get(codeLine).opnd1 = arrList.get(i + 2).tokenValue;
				codeGen.get(codeLine).opnd2 = "1";

				codeGen.get(codeLine).opre = "t";
				codeGen.get(codeLine).opre += operand1;
				operand1++;

				codeLine++;
			}

			expression(); // R
			if (arrList.get(i).tokenValue.equals(";")) {
				i++; // ;
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // ;
		else if (arrList.get(i).tokenValue.equals(";")) {
			i++; // ;
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// O
	static void selectionStatement() {
		// if (R) M O'
		if (arrList.get(i).tokenValue.equals("if")) {
			i++; // if
			if (arrList.get(i).tokenValue.equals("(")) {
				i++; // (

				// SEMANTICS
				// Scope check - ensures an expression has been properly declared prior to being
				// used. Checks to make sure array or void isn't inside ()
				if (arrList.get(i).tokenType.equals("id")) {
					boolean inScope = false;
					for (int j = scopeCount; j >= 0; j--) {
						if (variableList.get(j).containsKey(arrList.get(i).tokenValue)) {
							inScope = true;
						}
					}
					for (int j = scopeCount; j >= 0; j--) {
						if (functionList.get(j).containsKey(arrList.get(i).tokenValue)) {
							inScope = true;
						}
					}
					if (inScope == false) {
						//System.out.println("REJECT");
						//System.exit(0);
					}
					boolean inScope2 = false;
					for (int k = scopeCount; k >= 0; k--) {
						if (variableList.get(k).containsKey(arrList.get(i).tokenValue)) {
							if (!arrList.get(i + 1).tokenValue.equals("[")) {
								if (variableList.get(k).get(arrList.get(i).tokenValue).equals("array")
										|| variableList.get(k).get(arrList.get(i).tokenValue).equals("void")) {
									inScope2 = true;
								}
							}
						} else if (functionList.get(k).containsKey(arrList.get(i).tokenValue)) {
							if (!arrList.get(i + 1).tokenValue.equals("[")) {
								if (functionList.get(k).get(arrList.get(i).tokenValue).equals("array")
										|| functionList.get(k).get(arrList.get(i).tokenValue).equals("void")) {
									inScope2 = true;
								}
							}
						}

					}
					if (inScope2 == true) {
						//System.out.println("REJECT");
						//System.exit(0);
					}
				}

				expression(); // R
				if (arrList.get(i).tokenValue.equals(")")) {
					i++; // )
					statement(); // M
					selectionStatementPrime(); // O'
				} // fails
				else {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// O'
	static void selectionStatementPrime() {
		// empty | else M
		// else M
		if (arrList.get(i).tokenValue.equals("else")) {
			i++; // else
			statement(); // M
		}
	}

	// P
	static void iterationStatement() {
		// while (R) M
		if (arrList.get(i).tokenValue.equals("while")) {
			i++; // while
			if (arrList.get(i).tokenValue.equals("(")) {
				i++; // (
				expression(); // R
				if (arrList.get(i).tokenValue.equals(")")) {
					i++; // )
					statement(); // M
				} // fails
				else {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// Q
	static void returnStatement() {
		// return Q'
		if (arrList.get(i).tokenValue.equals("return")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "return";
			codeGen.get(codeLine).opnd1 = "    ";
			codeGen.get(codeLine).opnd2 = "    ";

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1;
			operand1++;

			codeLine++;

			// SEMANTICS
			// verify return type matches
			// void return
			if (returnType.get(returnCount - 1).equals("void")) {
				if (!arrList.get(i + 1).tokenValue.equals(";")) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}
			// int return
			if (returnType.get(returnCount - 1).equals("int")) {
				if (arrList.get(i + 1).tokenValue.equals(";")) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				if (arrList.get(i + 2).tokenValue.equals("(")) {
					if (functionList.get(0).containsKey(arrList.get(i + 1).tokenValue)) {
						if (!(functionList.get(0).get(arrList.get(i + 1).tokenValue).equals("int"))) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					}
				}
				for (int z = scopeCount; z >= 0; z--) {
					if (!arrList.get(i + 2).tokenValue.equals("[")) {
						if (variableList.get(z).containsKey(arrList.get(i + 1).tokenValue)) {
							if (!(variableList.get(z).get(arrList.get(i + 1).tokenValue).equals("int"))) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
							break;
						}
					}
				}
			}

			i++; // return
			returnStatementPrime(); // Q'
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// Q'
	static void returnStatementPrime() {
		// ; | R;
		// ;
		if (arrList.get(i).tokenValue.equals(";")) {
			i++; // ;
		} else {
			expression(); // R
			if (arrList.get(i).tokenValue.equals(";")) {
				i++; // ;
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		}
	}

	// R
	static void expression() {
		// id R' | (R) X' V' T' | num X' V' T'
		// id R'
		if (arrList.get(i).tokenType.equals("id")) {
			// SEMANTICS
			// Scope check - ensures an expression has been properly declared prior to being
			// used
			boolean inScope = false;
			for (int j = scopeCount; j >= 0; j--) {
				if (variableList.get(j).containsKey(arrList.get(i).tokenValue)) {
					inScope = true;
				}
			}
			for (int j = scopeCount; j >= 0; j--) {
				if (functionList.get(j).containsKey(arrList.get(i).tokenValue)) {
					inScope = true;
				}
			}
			if (inScope == false) {
				//System.out.println("REJECT");
				//System.exit(0);
			}
			// SEMANTICS
			// make sure if function, formatted properly when called, with ()
			boolean functionScope = false;
			boolean variableScope = false;
			for (int j = scopeCount; j >= 0; j--) {
				if (functionList.get(j).containsKey(arrList.get(i).tokenValue)) {
					functionScope = true;
				}
			}
			for (int j = scopeCount; j >= 0; j--) {
				if (variableList.get(j).containsKey(arrList.get(i).tokenValue)) {
					variableScope = true;
				}
			}
			// make sure if variable, properly formatted when called, without ()
			if (functionScope == true) {
				if (variableScope == false) {
					if (!arrList.get(i + 1).tokenValue.equals("(")) {
						if (!arrList.get(i + 2).tokenValue.equals(")")) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					} else if (variableScope == true) {

					}
				}
			}
			if (functionScope == false) {
				if (variableScope == true) {
					if (arrList.get(i + 1).tokenValue.equals("(")) {
						//System.out.println("REJECT");
						//System.exit(0);
					} else if (variableScope == true) {

					}
				}
			}
			// SEMANTICS
			// don't use int as array && don't use array as int
			for (int j = scopeCount; j >= 0; j--) {
				if (variableList.get(j).containsKey(arrList.get(i).tokenValue)) {
					if (variableList.get(j).get(arrList.get(i).tokenValue).equals("array")) {
						if (!(arrList.get(i + 1).tokenValue.equals("[") || arrList.get(i + 1).tokenValue.equals(";")
								|| arrList.get(i + 1).tokenValue.equals(",")
								|| arrList.get(i + 1).tokenValue.equals(")"))) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					}
					if (variableList.get(j).get(arrList.get(i).tokenValue).equals("int")) {
						if (arrList.get(i + 1).tokenValue.equals("[")) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					}
					break;
				}
			}

			// SEMANTICS
			//
			//
			if (insideParam) {
				if (!arrList.get(i + 1).tokenValue.equals("[")) {
					// array param
					// static ArrayList<LinkedHashMap<String, String>> paramList = new
					// ArrayList<LinkedHashMap<String, String>>();
					List<String> functionKeys = new ArrayList<String>(functionList.get(0).keySet());
					int ind = functionKeys.indexOf(currentFunc);
					List<String> paramValues = new ArrayList<String>(paramList.get(ind).values());
					if (variableList.get(scopeCount).containsKey(arrList.get(i).tokenValue)) {
						if (variableList.get(scopeCount).get(arrList.get(i).tokenValue).equals("array")) {
							Object value = paramValues.get(paramCounter);
							if (!value.equals("array")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}
			}

			i++; // id
			expressionPrime(); // R'
		} // (R) X' V' T'
		else if (arrList.get(i).tokenValue.equals("(")) {
			i++; // (
			expression(); // R
			if (arrList.get(i).tokenValue.equals(")")) {
				i++; // )

				termPrime(); // X'
				additiveExpressionPrime(); // V'
				simpleExpressionPrime(); // T'
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // num X' V' T'
		else if (arrList.get(i).tokenType.equals("num")) {

			// SEMANTICS
			//
			if (insideParam) {
				if (!arrList.get(i - 1).tokenValue.equals("[")) {
					// args match params in number & type
					// param logic
					// static ArrayList<LinkedHashMap<String, String>> paramList = new
					// ArrayList<LinkedHashMap<String, String>>();
					List<String> functionKeys = new ArrayList<String>(functionList.get(0).keySet());
					List<String> paramValues = new ArrayList<String>(
							paramList.get(functionKeys.indexOf(currentFunc)).values());

					// param type matching
					for (int j = scopeCount; j >= 0; j--) {
						if (paramList.get(paramSetNumber - 1).containsKey(arrList.get(i).tokenValue)) {
							Object key = paramList.get(paramSetNumber - 1).keySet().iterator().next();
							Object value = paramList.get(paramSetNumber - 1).get(key);
							String paramCheck = value.toString();
							// String paramCheck = paramList.get(paramSetNumber -
							// 1).get(arrList.get(i).tokenValue);
							if (!variableList.get(j).get(arrList.get(i).tokenValue).equals(paramCheck)) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
							break;
						} // int param
						if (arrList.get(i).tokenType.equals("num")) {
							if (paramCounter + 1 > paramValues.size()) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
							Object value = paramValues.get(paramCounter);
							if (!value.equals("int")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}
			}

			i++; // num
			termPrime(); // X'
			additiveExpressionPrime(); // V'
			simpleExpressionPrime(); // T'
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// R'
	static void expressionPrime() {
		// S' R'' | (theta) X' V' T'
		// (theta) X' V' T'

		// SEMANTICS
		// make sure operands not void & match
		// if i == id
		if (arrList.get(i - 1).tokenType.equals("id")) {
			// if i + 1 (
			if (arrList.get(i).tokenValue.equals("(") && !arrList.get(i + 2).tokenValue.equals(";")
					&& arrList.get(i + 1).tokenValue.equals(")")) {
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i - 1).tokenValue)) {
						if (functionList.get(j).get(arrList.get(i - 1).tokenValue).equals("void")) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					}
				}
			}

		}

		if (arrList.get(i).tokenValue.equals("(")) {
			i++; // (

			// SEMANTICS
			//
			currentFunc = arrList.get(i - 2).tokenValue;
			insideParam = true;
			// if function is calling args
			if (arrList.get(i).tokenValue.equals(")")) {
				// args match params in number & type
				// param logic
				int indexOfParamSet = functionSet.indexOf(arrList.get(i - 2).tokenValue);
				sizeOfParams = paramSet.get(indexOfParamSet);
			}
			if (!arrList.get(i).tokenValue.equals(")")) {
				// args match params in number & type
				// param logic
				int indexOfParamSet = functionSet.indexOf(arrList.get(i - 2).tokenValue);
				sizeOfParams = paramSet.get(indexOfParamSet);

				// param type matching
				for (int j = scopeCount; j >= 0; j--) {
					if (!(paramSetNumber == 0)) {
						if (paramList.get(paramSetNumber - 1).containsKey(arrList.get(i).tokenValue)) {
							Object key = paramList.get(paramSetNumber - 1).keySet().iterator().next();
							Object value = paramList.get(paramSetNumber - 1).get(key);
							String paramCheck = value.toString();
							// String paramCheck = paramList.get(paramSetNumber -
							// 1).get(arrList.get(i).tokenValue);
							if (!variableList.get(j).get(arrList.get(i).tokenValue).equals(paramCheck)) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
							break;
						}
					} // int param
					if (arrList.get(i).tokenType.equals("num")) {
						Object key = paramList.get(paramSetNumber - 1).keySet().iterator().next();
						Object value = paramList.get(paramSetNumber - 1).get(key);
						if (!value.equals("int")) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					} // array param
					if (arrList.get(i).tokenType.equals("array")) {
						Object key = paramList.get(paramSetNumber - 1).keySet().iterator().next();
						Object value = paramList.get(paramSetNumber - 1).get(key);
						if (!value.equals("array")) {
							//System.out.println("REJECT");
							//System.exit(0);
						}
					}
				}
			}

			args(); // theta
			if (arrList.get(i).tokenValue.equals(")")) {

				// SEMANTICS
				// check # of params = # of args
				paramCounter++;
				if (!(sizeOfParams == 0)) {
					if (!(paramCounter == sizeOfParams)) {
						//System.out.println("REJECT");
						//System.exit(0);
					}
				}
				paramCounter = 0;

				i++; // )

				// SEMANTICS
				//
				insideParam = false;

				termPrime(); // X'
				additiveExpressionPrime(); // V'
				simpleExpressionPrime(); // T'
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // S' R''
		else {
			variablePrime(); // S'
			expressionPrimePrime(); // R''
		}
	}

	// R''
	static void expressionPrimePrime() {
		// = R | X' V' T'
		// = R
		if (arrList.get(i).tokenValue.equals("=")) {
			// SEMANTICS
			// cannot assign an array
			if (!arrList.get(i - 1).tokenValue.equals(")") && !arrList.get(i + 1).tokenValue.equals("(")
					&& !arrList.get(i - 1).tokenValue.equals("]") && !arrList.get(i - 1).tokenType.equals("num")
					&& !arrList.get(i + 1).tokenType.equals("num")) {
				// SEMANTICS
				// Scope check - ensures an expression has been properly declared prior to being
				// used
				boolean inScope = false;
				for (int j = scopeCount; j >= 0; j--) {
					if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				if (inScope == false) {
					//System.out.println("REJECT");
					//System.exit(0);
				}

				boolean inScope2 = false;
				for (int k = scopeCount; k >= 0; k--) {
					if (variableList.get(scopeCount).containsKey(arrList.get(i - 1).tokenValue)) {
						if (variableList.get(scopeCount).get(arrList.get(i - 1).tokenValue).equals("array")) {
							inScope2 = true;
						}
					}
				}
				if (inScope2 == true) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}
			// SEMANTICS
			// cannot assign an array to variable
			if (arrList.get(i - 1).tokenValue.equals("]")) {
				if (!arrList.get(i + 2).tokenValue.equals("[") && !arrList.get(i + 1).tokenType.equals("num")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (variableList.get(j).get(arrList.get(i + 1).tokenValue).equals("array")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						} else if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (functionList.get(j).get(arrList.get(i + 1).tokenValue).equals("array")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}
			}

			// SEMANTICS
			// make sure operands not void & match
			String typeCheck;
			// id == i + 1
			if (arrList.get(i + 1).tokenType.equals("id")) {
				// if i + 1 (
				if (arrList.get(i + 2).tokenValue.equals("(")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (functionList.get(j).get(arrList.get(i + 1).tokenValue).equals("void")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}
				// matching variables
				if (arrList.get(i - 1).tokenType.equals("id")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							typeCheck = variableList.get(j).get(arrList.get(i + 1).tokenValue);
							for (int k = scopeCount; k >= 0; k--) {
								if (variableList.get(k).containsKey(arrList.get(i - 1).tokenValue)) {
									if (!(typeCheck.equals(variableList.get(k).get(arrList.get(i - 1).tokenValue)))) {
										if (!arrList.get(i + 2).tokenValue.equals("[")) {
											//System.out.println("REJECT");
											//System.exit(0);
										}
									}
								}
							}
						}
					}
				}
			}
			// if i + 1 [
			// if i + 1

			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "assign";
			codeGen.get(codeLine).opnd1 = arrList.get(i + 1).tokenValue;
			codeGen.get(codeLine).opnd2 = "     ";

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1;
			operand1++;

			codeLine++;

			i++; // =
			expression(); // R
		} else {
			termPrime(); // X'
			additiveExpressionPrime(); // V'
			simpleExpressionPrime(); // T'
		}
	}

	// S
	static void variable() {
		// id S' not used
	}

	// S'
	static void variablePrime() {
		// empty | [R]
		if (arrList.get(i).tokenValue.equals("[")) {
			i++; // [

			// SEMANTICS
			// Scope check - ensures an expression has been properly declared prior to being
			// used. Checks to make sure array or void isn't inside []
			if (arrList.get(i).tokenType.equals("id")) {
				boolean inScope = false;
				for (int j = scopeCount; j >= 0; j--) {
					if (variableList.get(j).containsKey(arrList.get(i).tokenValue)) {
						inScope = true;
					}
				}
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i).tokenValue)) {
						inScope = true;
					}
				}
				if (inScope == false) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
				boolean inScope2 = false;
				for (int k = scopeCount; k >= 0; k--) {
					if (variableList.get(k).containsKey(arrList.get(i).tokenValue)) {
						if (!arrList.get(i + 1).tokenValue.equals("[")) {
							if (variableList.get(k).get(arrList.get(i).tokenValue).equals("array")
									|| variableList.get(k).get(arrList.get(i).tokenValue).equals("void")) {
								inScope2 = true;
							}
						}
					} else if (functionList.get(k).containsKey(arrList.get(i).tokenValue)) {
						if (!arrList.get(i + 1).tokenValue.equals("[")) {
							if (functionList.get(k).get(arrList.get(i).tokenValue).equals("array")
									|| functionList.get(k).get(arrList.get(i).tokenValue).equals("void")) {
								inScope2 = true;
							}
						}
					}

				}
				if (inScope2 == true) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}

			expression(); // R
			if (arrList.get(i).tokenValue.equals("]")) {
				i++; // ]
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		}
	}

	// T
	static void simpleExpression() {
		// V T'
		additiveExpression();
		simpleExpressionPrime();
	}

	// T'
	static void simpleExpressionPrime() {
		// U V | empty
		if (arrList.get(i).tokenValue.equals("<") || arrList.get(i).tokenValue.equals("<=")
				|| arrList.get(i).tokenValue.equals(">") || arrList.get(i).tokenValue.equals(">=")
				|| arrList.get(i).tokenValue.equals("==") || arrList.get(i).tokenValue.equals("!=")) {
			// SEMANTICS
			// cannot comparator an array
			if (!arrList.get(i - 1).tokenValue.equals(")") && !arrList.get(i + 1).tokenValue.equals("(")
					&& !arrList.get(i - 1).tokenValue.equals("]") && !arrList.get(i - 1).tokenType.equals("num")
					&& !arrList.get(i + 1).tokenType.equals("num")) {
				// SEMANTICS
				// Scope check - ensures an expression has been properly declared prior to being
				// used
				boolean inScope = false;
				for (int j = scopeCount; j >= 0; j--) {
					if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				if (inScope == false) {
					//System.out.println("REJECT");
					//System.exit(0);
				}

				boolean inScope2 = false;
				for (int k = scopeCount; k >= 0; k--) {
					if (variableList.get(k).containsKey(arrList.get(i - 1).tokenValue)) {
						if (variableList.get(k).get(arrList.get(i - 1).tokenValue).equals("array")) {
							inScope2 = true;
						}
					}
				}
				if (inScope2 == true) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}

			// SEMANTICS
			// make sure operands not void & match
			// if i == id
			if (arrList.get(i + 1).tokenType.equals("id")) {
				// if i + 1 (
				if (arrList.get(i + 2).tokenValue.equals("(")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (functionList.get(j).get(arrList.get(i + 1).tokenValue).equals("void")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}

			}

			relationalOperator(); // U
			additiveExpression(); // V
		}
	}

	// U
	static void relationalOperator() {
		// <= | < | > | >= | == | !=
		if (arrList.get(i).tokenValue.equals("<") || arrList.get(i).tokenValue.equals("<=")
				|| arrList.get(i).tokenValue.equals(">") || arrList.get(i).tokenValue.equals(">=")
				|| arrList.get(i).tokenValue.equals("==") || arrList.get(i).tokenValue.equals("!=")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			codeGen.get(codeLine).op = "comp";
			codeGen.get(codeLine).opnd1 = arrList.get(i - 1).tokenValue;
			codeGen.get(codeLine).opnd2 = arrList.get(i + 1).tokenValue;

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1;
			operand1++;

			codeLine++;

			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			if (arrList.get(i).tokenValue.equals("<")) {
				codeGen.get(codeLine).op = "BRGEQ";
			} else if (arrList.get(i).tokenValue.equals("<=")) {
				codeGen.get(codeLine).op = " BRGT";
			} else if (arrList.get(i).tokenValue.equals(">")) {
				codeGen.get(codeLine).op = "BRLEQ";
			} else if (arrList.get(i).tokenValue.equals(">=")) {
				codeGen.get(codeLine).op = " BRLT";
			} else if (arrList.get(i).tokenValue.equals("==")) {
				codeGen.get(codeLine).op = " BREQ";
			} else if (arrList.get(i).tokenValue.equals("==")) {
				codeGen.get(codeLine).op = "BRNEQ";
			}

			codeGen.get(codeLine).opnd1 = "t";
			codeGen.get(codeLine).opnd1 += operand1 - 1;
			operand1++;

			codeGen.get(codeLine).opnd2 = "     ";

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1 - 1;
			operand1++;

			BPO = codeLine;

			codeLine++;

			i++; // <= | < | > | >= | == | !=
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// V
	static void additiveExpression() {
		// X V'
		term(); // X
		additiveExpressionPrime(); // V'
	}

	// V'
	static void additiveExpressionPrime() {
		// W X V' | empty
		// W X V'
		if (arrList.get(i).tokenValue.equals("+") || arrList.get(i).tokenValue.equals("-")) {
			// SEMANTICS
			// cannot add/sub an array
			if (!arrList.get(i - 1).tokenValue.equals(")") && !arrList.get(i + 1).tokenValue.equals("(")
					&& !arrList.get(i - 1).tokenValue.equals("]") && !arrList.get(i - 1).tokenType.equals("num")
					&& !arrList.get(i + 1).tokenType.equals("num")) {
				// SEMANTICS
				// Scope check - ensures an expression has been properly declared prior to being
				// used
				boolean inScope = false;
				for (int j = scopeCount; j >= 0; j--) {
					if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				if (inScope == false) {
					//System.out.println("REJECT");
					//System.exit(0);
				}

				boolean arrCheck = false;
				for (int k = scopeCount; k >= 0; k--) {
					if (variableList.get(k).containsKey(arrList.get(i - 1).tokenValue)) {
						if (variableList.get(k).get(arrList.get(i - 1).tokenValue).equals("array")) {
							arrCheck = true;
						}
					}
				}
				if (arrCheck == true) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}

			// SEMANTICS
			// make sure operands not void & match
			// if i == id
			if (arrList.get(i + 1).tokenType.equals("id")) {
				// if i + 1 (
				if (arrList.get(i + 2).tokenValue.equals("(")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (functionList.get(j).get(arrList.get(i + 1).tokenValue).equals("void")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}

			}

			additiveOperator(); // W
			term(); // X
			additiveExpressionPrime(); // V'
		}
	}

	// W
	static void additiveOperator() {
		// + | -
		if (arrList.get(i).tokenValue.equals("+") || arrList.get(i).tokenValue.equals("-")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			if (arrList.get(i).tokenValue.equals("+")) {
				codeGen.get(codeLine).op = "   add";
			} else {
				codeGen.get(codeLine).op = "   sub";
			}
			codeGen.get(codeLine).opnd1 = arrList.get(i - 1).tokenValue;
			codeGen.get(codeLine).opnd2 = arrList.get(i + 1).tokenValue;

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1;
			operand1++;

			codeLine++;

			i++; // + | -
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// X
	static void term() {
		// Z X'
		factor(); // Z
		termPrime(); // X'
	}

	// X'
	static void termPrime() {
		// Y Z X' | empty
		// Y Z X'
		if (arrList.get(i).tokenValue.equals("*") || arrList.get(i).tokenValue.equals("/")) {
			// SEMANTICS
			// cannot mult/divide an array
			if (!arrList.get(i - 1).tokenValue.equals(")") && !arrList.get(i + 1).tokenValue.equals("(")
					&& !arrList.get(i - 1).tokenValue.equals("]") && !arrList.get(i - 1).tokenType.equals("num")
					&& !arrList.get(i + 1).tokenType.equals("num")) {
				// SEMANTICS
				// Scope check - ensures an expression has been properly declared prior to being
				// used
				boolean inScope = false;
				for (int j = scopeCount; j >= 0; j--) {
					if (variableList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				for (int j = scopeCount; j >= 0; j--) {
					if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
						inScope = true;
					}
				}
				if (inScope == false) {
					//System.out.println("REJECT");
					//System.exit(0);
				}

				boolean inScope2 = false;
				for (int k = scopeCount; k >= 0; k--) {
					if (variableList.get(k).containsKey(arrList.get(i - 1).tokenValue)) {
						if (variableList.get(k).get(arrList.get(i - 1).tokenValue).equals("array")) {
							inScope2 = true;
						}
					}
				}
				if (inScope2 == true) {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			}

			// SEMANTICS
			// make sure operands not void & match
			// if i == id
			if (arrList.get(i + 1).tokenType.equals("id")) {
				// if i + 1 (
				if (arrList.get(i + 2).tokenValue.equals("(")) {
					for (int j = scopeCount; j >= 0; j--) {
						if (functionList.get(j).containsKey(arrList.get(i + 1).tokenValue)) {
							if (functionList.get(j).get(arrList.get(i + 1).tokenValue).equals("void")) {
								//System.out.println("REJECT");
								//System.exit(0);
							}
						}
					}
				}

			}

			multiplicativeOperator(); // W
			factor(); // X
			termPrime(); // V'
		}
	}

	// Y
	static void multiplicativeOperator() {
		// * | /
		if (arrList.get(i).tokenValue.equals("*") || arrList.get(i).tokenValue.equals("/")) {

			// code gen
			codeGen.add(new Generator(op, opnd1, opnd2, opre));
			if (arrList.get(i).tokenValue.equals("*")) {
				codeGen.get(codeLine).op = "  mult";
			} else {
				codeGen.get(codeLine).op = "   div";
			}
			codeGen.get(codeLine).opnd1 = arrList.get(i - 1).tokenValue;
			codeGen.get(codeLine).opnd2 = arrList.get(i + 1).tokenValue;

			codeGen.get(codeLine).opre = "t";
			codeGen.get(codeLine).opre += operand1;
			operand1++;

			codeLine++;

			i++; // + | -
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// Z
	static void factor() {
		// (R) | num | id Z'
		// (R)
		if (arrList.get(i).tokenValue.equals("(")) {
			i++; // (
			expression(); // R
			if (arrList.get(i).tokenValue.equals(")")) {
				i++; // )
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // num
		else if (arrList.get(i).tokenType.equals("num")) {
			i++; // num
		} // id Z'
		else if (arrList.get(i).tokenType.equals("id")) {
			i++; // id
			factorPrime(); // Z'
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// Z'
	static void factorPrime() {
		// S' | (theta)
		if (arrList.get(i).tokenValue.equals("(")) {
			i++; // (
			args(); // thera
			if (arrList.get(i).tokenValue.equals(")")) {
				i++; // )
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // S'
		else {
			variablePrime(); // S'
		}

	}

	// PI
	static void call() {
		// id (theta) // not used
		if (arrList.get(i).tokenType.equals("id")) {
			i++; // id
			if (arrList.get(i).tokenValue.equals("(")) {
				i++; // (
				args(); // theta
				if (arrList.get(i).tokenValue.equals(")")) {
					i++; // )
				} // fails
				else {
					//System.out.println("REJECT");
					//System.exit(0);
				}
			} // fails
			else {
				//System.out.println("REJECT");
				//System.exit(0);
			}
		} // fails
		else {
			//System.out.println("REJECT");
			//System.exit(0);
		}
	}

	// THETA
	static void args() {
		// psi | empty
		if (arrList.get(i).tokenType.equals("id") || arrList.get(i).tokenValue.equals("(")
				|| arrList.get(i).tokenType.equals("num")) {
			argList(); // psi
		}
	}

	// PSI
	static void argList() {
		// R psi'
		expression(); // R
		argListPrime(); // psi'
	}

	// PSI'
	static void argListPrime() {
		// , R psi' | empty
		// , R psi'
		if (arrList.get(i).tokenValue.equals(",")) {
			// SEMANTICS
			paramCounter++;

			i++; // ,
			expression(); // R
			argListPrime(); // psi'
		}
	}

} /// main class