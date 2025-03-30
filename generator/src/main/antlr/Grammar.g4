grammar Grammar;

file : (directive | rule)* EOF;

directive : '!' command=Ident directiveArgs? directiveBody ;

directiveArgs : '(' Ident (',' Ident)* ')' ;

directiveBody : Ident (','? Ident)* ';' | Code ';'? ;

rule
    : Ident inAttrs? (COLON outAttr=type)? BYDEF variant ('|' variant)* ';' # ParseRule
    | Ident EQUAL (Regex | Literal) ';' # LexRule
    ;

inAttrs : '[' param (',' param)* ']';

param : Ident COLON type;

type
    : Ident ('<' types '>')? '?'? # concrete
    | '*' # starProjection
    | '(' types ')' '->' type # function
    | '(' type ')' '?' # nullable
    ;

types : type (',' type)* ;

variant: element* ;

element
    : Ident args? # regular
    | Code # codeInsert
    | Regex # surrogateRegex
    | Literal # surrogateLiteral
    ;

args: '(' arg (',' arg)* ')' ;

arg : Ident | Code ;

Ident : [A-Za-z_][_a-zA-Z0-9.]*;

Regex : '/' (~[/\\] | '\\' .)* '/';
Literal : '`' (~[`\\] | '\\`' | '\\\\' | '\\n' | '\\r' | '\\u' [A-Fa-f0-9][A-Fa-f0-9][A-Fa-f0-9][A-Fa-f0-9] )* '`';

BYDEF : ':=';
EQUAL : '=';
COLON : ':';

Code : '{' (~[{}] | Code)* '}' ;

WS : [ \t\r\n]+ -> skip ;

LineComment  : '//' (~[\r\n])* -> skip ;
