parser grammar StackTracesParser;

options { tokenVocab = StackTracesLexer; }

stackTraces: ( content )* EOF;

content: stackTrace # RootStackTrace
        | .         # MiscContent
        ;

stackTrace: messageLine atLine+ ellipsisLine? causedByLine?;

atLine: AT qualifiedMethod LPAR classFile RPAR;

ellipsisLine: ELLIPSIS NUMBER MORE_;

causedByLine: CAUSED_BY stackTrace;

messageLine: qualifiedClass (COLON message)?;

qualifiedClass: packagePath? className innerClassName*;

innerClassName: DOLLAR (NUMBER | className);

classFile: fileLocation
        | isNative
        | isUnknown
        ;

fileLocation: fileName COLON NUMBER;

isNative: NATIVE_METHOD;

isUnknown: UNKNOWN_SOURCE;

fileName: className FILEEXTENSION;

qualifiedMethod: qualifiedClass DOT (methodName | constructor);

constructor: INIT;

methodName: ID;

packagePath: (ID DOT) +;

className: ID;

message: .*?;
