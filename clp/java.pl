module(java,
       [java_main/2],
       [java_test]).

datadef(pair, [A, B], [pair(A, B)]).

datadef(map, [K, V], [map(list(pair(K, V)),
                          relation([K, K]),
                          relation([K, K]))]).

clausedef(mapMember, [K, V], [K,           % key to look for
                              V,           % value bound for that key
                              map(K, V)]). % the map to look in
mapMember(K, V, map(Pairs, _, _)) :-
    member(pair(K, V), Pairs).

clausedef(mapAdd, [K, V], [map(K, V),   % input map
                           K,           % key to add
                           V,           % value to add for the key
                           map(K, V)]). % output map
mapAdd(map(InPairs, R1, R2), K, V, map(OutPairs, R1, R2)) :-
    mapAddInternal(InPairs, R1, R2, K, V, OutPairs).

clausedef(mapAddInternal, [K, V], [list(pair(K, V)),   % input map
                                   relation([K, K]),   % used to determine if two keys are the same
                                   relation([K, K]),   % used to determine if two keys are different
                                   K,                  % key to add
                                   V,                  % value to add for the key
                                   list(pair(K, V))]). % output map
mapAddInternal([], _, _, K, V, [pair(K, V)]).
mapAddInternal([pair(K1, _)|Rest], KeysSame, _, K2, V, [pair(K2, V)|Rest]) :-
    call(KeysSame, K1, K2).
mapAddInternal([Pair|InRest], KeysSame, KeysDifferent, K1, V, [Pair|OutRest]) :-
    Pair = pair(K2, _),
    call(KeysDifferent, K1, K2),
    mapAddInternal(InRest, KeysSame, KeysDifferent, K1, V, OutRest).

clausedef(makeEqualsMap, [K, V], [map(K, V)]).
makeEqualsMap(map([],
                  lambda([K1, K2], K1 == K2),
                  lambda([K1, K2], K1 \== K2))).


datadef(env, [], [env(map(variable, type))]).

datadef(variable, [], [variable(atom)]).
datadef(method_name, [], [method_name(atom)]).
datadef(class_name, [], [class_name(atom)]).

datadef(type,
        [],
        [int_type,
         boolean_type,
         double_type,
         class_type(class_name)]).

datadef(call,
        [],
        [call(expression,
              method_name,
              list(expression))]).

datadef(boolean,
        [],
        [yes, no]).

datadef(expression,
        [],
        [variable_expression(variable),
         int_literal(int),
         boolean_literal(boolean),
         double_literal(int),
         string_literal(atom),
         new_expression(class_name, list(expression)),
         call_expression(call)]).

datadef(statement,
        [],
        [variable_declaration(type, variable, expression),
         call_statement(call)]).

datadef(java_test,
        [],
        [java_test(list(statement))]).

datadef(class_spec,
        [],
        [class_spec(class_name,
                    list(class_name),
                    list(constructor_spec),
                    list(method_spec))]).

datadef(constructor_spec,
        [],
        [constructor_spec(list(type))]).

datadef(option,
        [A],
        [some(A),
         none]).

datadef(method_spec,
        [],
        [method_spec(method_name,
                     list(type),
                     option(type))]).

clausedef(defaultSpec, [], [list(class_spec)]).
defaultSpec([class_spec(class_name('Object'),
                        [],
                        [constructor_spec([])],
                        []),
             class_spec(class_name('CharSequence'),
                        [class_name('Object')],
                        [],
                        [method_spec(method_name('length'),
                                     [],
                                     some(int_type))]),
             class_spec(class_name('String'),
                        [class_name('Object'), class_name('CharSequence')],
                        [constructor_spec([])],
                        [method_spec(method_name('concat'),
                                     [class_type(class_name('String'))],
                                     some(class_type(class_name('String'))))]),
             class_spec(class_name('java.util.ArrayList'),
                        [class_name('Object')],
                        [constructor_spec([]),
                         constructor_spec([int_type])],
                        [method_spec(method_name('size'),
                                     [],
                                     some(int_type)),
                         method_spec(method_name('add'),
                                     [class_type(class_name('Object'))],
                                     some(boolean_type)),
                         method_spec(method_name('remove'),
                                     [class_type(class_name('Object'))],
                                     some(boolean_type))])]).

% gets all the immediate parents of the given class
clausedef(hierarchyGet, [], [list(class_spec),
                             class_name,
                             list(class_name)]).
hierarchyGet(Spec, ClassName, DirectParents) :-
    once(lambda([], member(class_spec(ClassName, DirectParents, _, _), Spec))).

clausedef(allParents, [], [list(class_spec),
                           class_name,
                           class_name]).
allParents(Spec, Of, Result) :-
    hierarchyGet(Spec, Of, DirectParents),
    member(DirectParent, DirectParents),
    (Result = DirectParent;
     allParents(Spec, DirectParent, Result)).

clausedef(once, [], [relation([])]).
once(Call) :-
    call(Call),
    !.
    
% succeeds if this is a parent/child relationship
clausedef(parentChild, [], [list(class_spec),
                            class_name,
                            class_name]).
parentChild(Spec, Parent, Child) :-
    hierarchyGet(Spec, Child, DirectParents),
    member(Parent, DirectParents),
    !.
parentChild(Spec, Parent, Child) :-
    hierarchyGet(Spec, Child, DirectParents),
    member(DirectParent, DirectParents),
    parentChild(Spec, Parent, DirectParent),
    !.

% succeeds if true
clausedef(typesCompatibleParentChild, [], [list(class_spec),
                                           type,
                                           type]).
typesCompatibleParentChild(_, Type, Type) :- !.
typesCompatibleParentChild(Spec, class_type(ParentName), class_type(ChildName)) :-
    parentChild(Spec, ParentName, ChildName),
    !.

clausedef(makeExpressionsOfTypes, [], [list(class_spec),
                                       int,
                                       env,
                                       list(type),
                                       list(expression)]).
makeExpressionsOfTypes(_, _, _, [], []).
makeExpressionsOfTypes(Spec, Size, Env, [Type|Types], [Expression|Expressions]) :-
    makeExpression(Spec, Size, Env, Expression, ActualType),
    typesCompatibleParentChild(Spec, Type, ActualType),
    makeExpressionsOfTypes(Spec, Size, Env, Types, Expressions).

clausedef(findCompatibleMethod, [], [list(class_spec),
                                     class_name,
                                     method_spec]).
findCompatibleMethod(Spec, ClassName, MethodSpec) :-
    member(class_spec(ClassName, _, _, MethodSpecs), Spec),
    member(MethodSpec, MethodSpecs).

clausedef(returnTypeOk, [], [option(type), boolean]).
returnTypeOk(some(_), _) :- !.
returnTypeOk(_, yes).

clausedef(makeCall, [], [list(class_spec),
                         int,
                         env,
                         boolean,
                         call,
                         option(type)]).
makeCall(Spec, Size, Env, VoidOk, call(Base, MethodName, Params), ReturnType) :-
    makeExpression(Spec, Size, Env, Base, ExpressionType),
    ExpressionType = class_type(MyClassName), % intentionally mirroring Scala
    (MethodClassName = MyClassName;
     allParents(Spec, MyClassName, MethodClassName)),
    findCompatibleMethod(Spec, MethodClassName, method_spec(MethodName, ParamTypes, ReturnType)),
    returnTypeOk(ReturnType, VoidOk),
    makeExpressionsOfTypes(Spec, Size, Env, ParamTypes, Params).

clausedef(decBound, [], [int, int]).
decBound(In, Out) :-
    In > 0,
    Out is In - 1.

clausedef(makeExpression, [], [list(class_spec),
                               int,
                               env,
                               expression,
                               type]).
makeExpression(_, _, _, int_literal(0), int_type).
makeExpression(_, _, _, boolean_literal(no), boolean_type).
makeExpression(_, _, _, double_literal(0), double_type).
makeExpression(_, _, _, string_literal('foo'), class_type(class_name('String'))).
makeExpression(_, _, env(Map), variable_expression(Variable), Type) :-
    mapMember(Variable, Type, Map).
makeExpression(Spec, Size1, Env, new_expression(ClassName, Params), class_type(ClassName)) :-
    decBound(Size1, Size2),
    member(class_spec(ClassName, _, Constructors, _), Spec),
    member(constructor_spec(ParamTypes), Constructors),
    makeExpressionsOfTypes(Spec, Size2, Env, ParamTypes, Params).
makeExpression(Spec, Size1, Env, call_expression(Call), ReturnType) :-
    decBound(Size1, Size2),
    makeCall(Spec, Size2, Env, no, Call, RawReturnType),
    RawReturnType = some(ReturnType).

clausedef(yolo_UNSAFE_atom_concat_number, [], [atom, int, atom]).
yolo_UNSAFE_atom_concat_number(Atom, Int, Result) :-
    number_atom(Int, IntAtom),
    atom_concat(Atom, IntAtom, Result).

clausedef(makeStatement, [], [list(class_spec),
                              int,
                              env,
                              int,
                              statement,
                              env]).
makeStatement(Spec, Size, Env, StatementNum,
              variable_declaration(InitializerType, Variable, Initializer),
              env(NewMap)) :-
    makeExpression(Spec, Size, Env, Initializer, InitializerType),
    yolo_UNSAFE_atom_concat_number('x', StatementNum, VariableName),
    Variable = variable(VariableName),
    Env = env(Map),
    mapAdd(Map, Variable, InitializerType, NewMap).
makeStatement(Spec, Size, Env, _,
              call_statement(Call),
              Env) :-
    makeCall(Spec, Size, Env, yes, Call, _).

clausedef(makeStatements, [], [list(class_spec),
                               int, % size
                               env,
                               int, % numStatements
                               list(statement)]).
makeStatements(_, _, _, 0, []).
makeStatements(Spec, Size, Env, NumStatements, [Statement|Statements]) :-
    NumStatements > 0,
    makeStatement(Spec, Size, Env, NumStatements, Statement, NewEnv),
    NewNumStatements is NumStatements - 1,
    makeStatements(Spec, Size, NewEnv, NewNumStatements, Statements).

clausedef(makeTest, [], [int, java_test]).
makeTest(Bound, java_test(Statements)) :-
    defaultSpec(Spec),
    makeEqualsMap(EmptyMap),
    makeStatements(Spec, Bound, env(EmptyMap), 1, Statements).

clausedef(java_main, [], [int, java_test]).
java_main(Size, Test) :-
    makeTest(Size, Test).
