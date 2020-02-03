mapMember(K, V, map(Pairs, _, _)) :-
    member(pair(K, V), Pairs).
mapAdd(map(InPairs, R1, R2), K, V, map(OutPairs, R1, R2)) :-
    mapAddInternal(InPairs, R1, R2, K, V, OutPairs).
mapAddInternal([], _, _, K, V, [pair(K, V)]).
mapAddInternal([pair(K1, _)|Rest], KeysSame, _, K2, V, [pair(K2, V)|Rest]) :-
    call(KeysSame, K1, K2).
mapAddInternal([Pair|InRest], KeysSame, KeysDifferent, K1, V, [Pair|OutRest]) :-
    Pair = pair(K2, _),
    call(KeysDifferent, K1, K2),
    mapAddInternal(InRest, KeysSame, KeysDifferent, K1, V, OutRest).
makeEqualsMap(map([],
                  lambda([K1, K2], K1 == K2),
                  lambda([K1, K2], K1 \== K2))).
hierarchyGet(Spec, ClassName, DirectParents) :-
    once(lambda([], member(class_spec(ClassName, DirectParents, _, _), Spec))).
allParents(Spec, Of, Result) :-
    hierarchyGet(Spec, Of, DirectParents),
    member(DirectParent, DirectParents),
    (Result = DirectParent;
     allParents(Spec, DirectParent, Result)).
once(Call) :-
    call(Call),
    !.
parentChild(Spec, Parent, Child) :-
    hierarchyGet(Spec, Child, DirectParents),
    member(Parent, DirectParents),
    !.
parentChild(Spec, Parent, Child) :-
    hierarchyGet(Spec, Child, DirectParents),
    member(DirectParent, DirectParents),
    parentChild(Spec, Parent, DirectParent),
    !.
typesCompatibleParentChild(_, Type, Type) :- !.
typesCompatibleParentChild(Spec, class_type(ParentName), class_type(ChildName)) :-
    parentChild(Spec, ParentName, ChildName),
    !.
makeExpressionsOfTypes(_, _, _, [], []).
makeExpressionsOfTypes(Spec, Size, Env, [Type|Types], [Expression|Expressions]) :-
    makeExpression(Spec, Size, Env, Expression, ActualType),
    typesCompatibleParentChild(Spec, Type, ActualType),
    makeExpressionsOfTypes(Spec, Size, Env, Types, Expressions).
findCompatibleMethod(Spec, ClassName, MethodSpec) :-
    member(class_spec(ClassName, _, _, MethodSpecs), Spec),
    member(MethodSpec, MethodSpecs).
returnTypeOk(some(_), _) :- !.
returnTypeOk(_, yes).
makeCall(Spec, Size, Env, VoidOk, call(Base, MethodName, Params), ReturnType) :-
    makeExpression(Spec, Size, Env, Base, ExpressionType),
    ExpressionType = class_type(MyClassName), % intentionally mirroring Scala
    (MethodClassName = MyClassName;
     allParents(Spec, MyClassName, MethodClassName)),
    findCompatibleMethod(Spec, MethodClassName, method_spec(MethodName, ParamTypes, ReturnType)),
    returnTypeOk(ReturnType, VoidOk),
    makeExpressionsOfTypes(Spec, Size, Env, ParamTypes, Params).
decBound(In, Out) :-
    In > 0,
    Out is In - 1.
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
yolo_UNSAFE_atom_concat_number(Atom, Int, Result) :-
    number_atom(Int, IntAtom),
    atom_concat(Atom, IntAtom, Result).
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
makeStatements(_, _, _, 0, []).
makeStatements(Spec, Size, Env, NumStatements, [Statement|Statements]) :-
    NumStatements > 0,
    makeStatement(Spec, Size, Env, NumStatements, Statement, NewEnv),
    NewNumStatements is NumStatements - 1,
    makeStatements(Spec, Size, NewEnv, NewNumStatements, Statements).
makeTest(Bound, java_test(Statements)) :-
    defaultSpec(Spec),
    makeEqualsMap(EmptyMap),
    makeStatements(Spec, Bound, env(EmptyMap), 1, Statements).
java_main(Size, Test) :-
    makeTest(Size, Test).
