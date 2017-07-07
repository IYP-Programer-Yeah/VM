/*todo:
*1- delimeter
*/

package scanner;

import vm.*;

%%
%class Scanner

%unicode
%line
%column
%public
%type Instruction
%{
	private Instruction instr;
	private StringBuffer stringConstant = new StringBuffer();
	private float floatConstant;
	private int integerConstant;
	private int currentOperand;
	
    public int getLine() {
        return yyline + 1;
    }
	
	public int getColumn() {
		return yycolumn + 1;
	}
	
	public double getIntegerConstant() {
		return integerConstant;
	}
	
	public double getFloatConstant() {
		return floatConstant;
	}
%}

WhiteSpace = \r|\n|\r\n|\t|" "
EOL = \r|\n|\r\n
HexadecimalDigit = [0-9a-fA-F]
OctalDigit = [0-7]
DecimalDigit = [0-9]

/***************************************************************************************/
/***************************************************************************************/
/*****************************String/Character Recognition******************************/
/***************************************************************************************/
/***************************************************************************************/
StringStartIndicator = \"
StringEndIndicator = \"
StringEscapeSequence = \\
StringConstant = !([^]*({StringEndIndicator}|{StringEscapeSequence}|{EOL})[^]*|"")


OctalCharacter = {OctalDigit}{1,3}
InvalidOctalCharacter = {DecimalDigit}
HexadecimalCharacter = [x]{HexadecimalDigit}{1,8}
UniversalUnicodeCharacterValue = ([u]{HexadecimalDigit}{4} | [U]{HexadecimalDigit}{8})
/***************************************************************************************/
/***************************************************************************************/
/*****************************String/Character Recognition******************************/
/***************************************************************************************/
/***************************************************************************************/

/***************************************************************************************/
/***************************************************************************************/
/*********************************Comment Recognition***********************************/
/***************************************************************************************/
/***************************************************************************************/
SingleLineCommentString = !([^]*{EOL}[^]*|"")
SingleLineCommentStartIndicator = "//"

CommentBlockIndicatorEndCharacter = [/]
CommentBlockIndicatorMidharacter = [*]
CommentBlockIndicatorCharacters = {CommentBlockIndicatorMidharacter}|{CommentBlockIndicatorEndCharacter}
CommentBlockStartIndicator = {CommentBlockIndicatorEndCharacter}{CommentBlockIndicatorMidharacter}
CommnetBlockEndIndicator = {CommentBlockIndicatorMidharacter}{CommentBlockIndicatorEndCharacter}
CommentBlockString = !([^]*({CommentBlockIndicatorMidharacter}|{CommentBlockIndicatorEndCharacter})[^]*|"")
/***************************************************************************************/
/***************************************************************************************/
/*********************************Comment Recognition***********************************/
/***************************************************************************************/
/***************************************************************************************/

DecimalIntegerConstant = [+-]?{DecimalDigit}+
HexadecimalIntegerConstant = [+-]?"0x"{HexadecimalDigit}+
InvalidHexadecimalIntegerConstant = [+-]?"0x"

RealNumber = ({DecimalDigit}*[.]{DecimalDigit}+)|({DecimalDigit}+[.]{DecimalDigit}*)
ScientificRepresentation = ({RealNumber}|{DecimalIntegerConstant})[eE][-+]?{DecimalIntegerConstant}
FloatConstant = [+-]?({RealNumber}|{ScientificRepresentation})

IdentifierDigitAndLetter = [a-zA-Z0-9]
IdentifierRepeatingBlock = [_]+{IdentifierDigitAndLetter}+
Identifier = ([a-zA-Z]+{IdentifierDigitAndLetter}*){IdentifierRepeatingBlock}*| {IdentifierRepeatingBlock}+

Register = R{DecimalDigit}+

%state STRING
%state STRING_ESCAPE_SEQUENCE

%state OPERAND

%state SINGLE_LINE_COMMENT
%state COMMENT_BLOCK

%%

<YYINITIAL> {
	{WhiteSpace} {}
	{Identifier} {
		instr = new Instruction();
		instr.name = yytext();
		yybegin(OPERAND);
		currentOperand = 0;
		instr.operands[currentOperand] = new Operand();
	}
	
	{SingleLineCommentStartIndicator} {yybegin(SINGLE_LINE_COMMENT);}
	{CommentBlockStartIndicator} {yybegin(COMMENT_BLOCK);}
	
	[^] {return null;}
	<<EOF>> {return null;}
}

/***************************************************************************************/
/***************************************************************************************/
/*************************************String States*************************************/
/***************************************************************************************/
/***************************************************************************************/
<STRING> {
	{StringConstant} {stringConstant.append(yytext());}
	{StringEscapeSequence} {yybegin(STRING_ESCAPE_SEQUENCE);}
	{StringEndIndicator}{StringEndIndicator} {}
	{StringEndIndicator} {
		instr.operands[currentOperand].value = stringConstant.toString();
		instr.operands[currentOperand].operandType = Operand.OperandType.String;
		yybegin(OPERAND);
	}
	[^] {yybegin(YYINITIAL); return null;}
	<<EOF>> {return null;}
}

<STRING_ESCAPE_SEQUENCE> {
	{StringEndIndicator} {stringConstant.append(yytext()); yybegin(STRING);}
	{StringEscapeSequence} {stringConstant.append(yytext()); yybegin(STRING);}
	['?] {stringConstant.append(yytext()); yybegin(STRING);}
	[n] {stringConstant.append("\n"); yybegin(STRING);}
	[t] {stringConstant.append("\t"); yybegin(STRING);}
	[v] {stringConstant.append((char)11); yybegin(STRING);}
	[b] {stringConstant.append("\b"); yybegin(STRING);}
	[r] {stringConstant.append("\r"); yybegin(STRING);}
	[f] {stringConstant.append("\f"); yybegin(STRING);}
	[a] {stringConstant.append((char)7); yybegin(STRING);}
	{EOL} {yybegin(STRING);}
	{OctalCharacter} {
		int tempCharacter = Integer.parseInt(yytext(),8);
		yybegin(STRING); 
		stringConstant.append((char)tempCharacter);
	}
	{HexadecimalCharacter} {stringConstant.append((char)Long.parseLong(yytext().substring(1),16)); yybegin(STRING);}
	{UniversalUnicodeCharacterValue} {
		long num = Long.parseLong(yytext().substring(1),16);
		yybegin(STRING);
		stringConstant.append((char)num);
	}
	{InvalidOctalCharacter} {return null;}
	[x] {return null;}
	[uU] {return null;}
	[^] {return null;}
	<<EOF>> {return null;}
}

/***************************************************************************************/
/***************************************************************************************/
/*************************************String States*************************************/
/***************************************************************************************/
/***************************************************************************************/


<SINGLE_LINE_COMMENT> {
	{SingleLineCommentString} {}
	{EOL} {yybegin(YYINITIAL);}
	<<EOF>> {yybegin(YYINITIAL);}
}

<COMMENT_BLOCK> {
	{CommentBlockString} {}
	{CommnetBlockEndIndicator} {yybegin(YYINITIAL);}
	{CommentBlockIndicatorCharacters} {}
	<<EOF>> {yybegin(YYINITIAL);}
}


<OPERAND> {
	{StringStartIndicator} {stringConstant.setLength(0); yybegin(STRING);}
	
	{SingleLineCommentStartIndicator} {yybegin(SINGLE_LINE_COMMENT); return instr;}
	{CommentBlockStartIndicator} {yybegin(COMMENT_BLOCK); return instr;}
	
	{FloatConstant} {
		instr.operands[currentOperand].value = Float.floatToRawIntBits(Float.parseFloat(yytext()));
		instr.operands[currentOperand].operandType = Operand.OperandType.Number;
	}
	
	{HexadecimalIntegerConstant} {
		instr.operands[currentOperand].value = Integer.decode(yytext());
		instr.operands[currentOperand].operandType = Operand.OperandType.Number;
	}
	{InvalidHexadecimalIntegerConstant} {
		return null;
	}
	{DecimalIntegerConstant} {
		instr.operands[currentOperand].value = Integer.decode(yytext());
		instr.operands[currentOperand].operandType = Operand.OperandType.Number;
	}
	{Register} {
		instr.operands[currentOperand].value = yytext();
		instr.operands[currentOperand].operandType = Operand.OperandType.Register;
	}
	{Identifier} {
		instr.operands[currentOperand].value = yytext();
		instr.operands[currentOperand].operandType = Operand.OperandType.Id;
	}
	[,] {
		currentOperand++;
		instr.operands[currentOperand] = new Operand();
		yybegin(OPERAND);
	}
	[#] {
	}
	[@] {
		instr.operands[currentOperand].isAddress = true;
	}
	[\$] {
		instr.operands[currentOperand].isTopStack = true;
	}
	[\^] {
		instr.operands[currentOperand].isRelative = true;
	}
	{EOL} {yybegin(YYINITIAL); return instr;}
	
	{WhiteSpace} {}
	
	<<EOF>> {yybegin(YYINITIAL); return instr;}
	[^] {return null;}
}