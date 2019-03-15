lexer grammar StackTracesLexer;

// Symbols

AT: 'at';

FILEEXTENSION: '.java';

CAUSED_BY: 'Caused by:';

MORE_: 'more';

NATIVE_METHOD: 'Native Method';

UNKNOWN_SOURCE: 'Unknown Source';

INIT: '<init>';

DOLLAR: '$';

LPAR: '(';

RPAR: ')';

DOT: '.';

ELLIPSIS: '...';

COLON: ':';

// Numbers

NUMBER: (DIGIT)+;

fragment DIGIT: '0' .. '9' ;

// Identifiers

ID: (UPPERCASE | LOWERCASE | UNDERSCORE | DIGIT)+;

fragment LOWERCASE: 'a' .. 'z';

fragment UPPERCASE: 'A' .. 'Z';

UNDERSCORE: '_';

// Skip Whitespaces

WS: (' ' | '\r' | '\t' | '\u000C' | '\n') -> skip;

// Skip everything else

OTHER : . -> skip ;