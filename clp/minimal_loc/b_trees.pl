inInclusiveRange(Start, End, Start) :-
    Start =< End.
inInclusiveRange(Start, End, Result) :-
    Start < End,
    NewStart is Start + 1,
    inInclusiveRange(NewStart, End, Result).
inInclusiveRangeList(Start, End, []) :-
    Start > End.
inInclusiveRangeList(Start, End, [Start|Rest]) :-
    Start =< End,
    NewStart is Start + 1,
    inInclusiveRangeList(NewStart, End, Rest).
yolo_UNSAFE_minChildSize(H, Output) :-
    HMinusOne is H - 1,
    powInt(2, HMinusOne, PowResult),
    Output is 2 * PowResult - 1.
yolo_UNSAFE_maxChildSize(H, Output) :-
    powInt(4, H, Temp),
    Output is Temp - 1.
getAdditions(0, Amount, _, _) :-
    Amount > 0,
    !,
    fail.
getAdditions(0, _, _, []) :- !.
getAdditions(1, Amount, Max, _) :-
    Amount > Max,
    !,
    fail.
getAdditions(Size, Amount, Max, [Added|Rest]) :-
    RangeEnd is min(Amount, Max),
    SizeMinusOne is Size - 1,
    inInclusiveRange(0, RangeEnd, Added),
    AmountMinusAdded is Amount - Added,
    getAdditions(SizeMinusOne, AmountMinusAdded, Max, Rest).
sublist([], []).
sublist([H|T], [H|Rest]) :-
    sublist(T, Rest).
sublist([_|T], Rest) :-
    sublist(T, Rest).
sublistOfSize(Input, Size, Output) :-
    length(Output, Size),
    sublist(Input, Output).
yolo_UNSAFE_rootBTreeHeight(Size, Height) :-
    SizePlusOne is Size + 1,
    logInt(SizePlusOne, LogResult),
    LogResultPlusOne is LogResult + 1,
    inInclusiveRange(1, LogResultPlusOne, Height).
makeRootBTree(Size, Result) :-
    yolo_UNSAFE_rootBTreeHeight(Size, Height),
    makeRootBTree(Size, 1, Size, Height, Result).
makeRootBTree(Size, KeyRangeStart, KeyRangeEnd, 1, b_tree(X, [])) :-
    Size < 4,
    !,
    inInclusiveRangeList(KeyRangeStart, KeyRangeEnd, KeyRangeList),
    sublistOfSize(KeyRangeList, Size, X).
makeRootBTree(Size, KeyRangeStart, KeyRangeEnd, H, Result) :-
    H > 1,
    !,
    inInclusiveRange(2, 4, X),
    makeBTree(X, Size, KeyRangeStart, KeyRangeEnd, H, Result).
makeNonRootBTree(Size, KeyRangeStart, KeyRangeEnd, 1, b_tree(X, [])) :-
    Size < 4,
    Size >= 1,
    !,
    inInclusiveRangeList(KeyRangeStart, KeyRangeEnd, KeyRangeList),
    sublistOfSize(KeyRangeList, Size, X).
makeNonRootBTree(Size, KeyRangeStart, KeyRangeEnd, H, Result) :-
    H > 1,
    Size > 0,
    !,
    inInclusiveRange(2, 4, X),
    makeBTree(X, Size, KeyRangeStart, KeyRangeEnd, H, Result).
map([], _, []).
map([A|As], Relation, [B|Bs]) :-
    call(Relation, A, B),
    map(As, Relation, Bs).
zip([], [], []).
zip([A|As], [B|Bs], [pair(A, B)|Rest]) :-
    zip(As, Bs, Rest).
scanLeft([], B, _, [B]).
scanLeft([A|As], B, Relation, [B|Bs]) :-
    call(Relation, B, A, NewB),
    scanLeft(As, NewB, Relation, Bs).
tail([_|Rest], Rest).
init([_], []).
init([A1, A2|As], [A1|Rest]) :-
    init([A2|As], Rest).
keysHelper(ChildSizes, AddKeys, KeyRangeStart, Keys) :-
    zip(ChildSizes, AddKeys, ZipResult),
    InitialScan is KeyRangeStart - 1,
    scanLeft(ZipResult,
             InitialScan,
             lambda([SoFar,
                     pair(ChildSize, Add),
                     LambdaResult],
                    LambdaResult is SoFar + ChildSize + Add + 1),
             ScanResult),
    tail(ScanResult, TailResult),
    init(TailResult, Keys).
childRangesHelper(KeyRangeStart, Keys, KeyRangeEnd, ChildRanges) :-
    StartMinusOne is KeyRangeStart - 1,
    EndPlusOne is KeyRangeEnd + 1,
    append(Keys, [EndPlusOne], ZipRight),
    zip([StartMinusOne|Keys], ZipRight, ZipResult),
    map(ZipResult,
        lambda([pair(A, B),
                pair(InnerStart, InnerEnd)],
               (InnerStart is A + 1,
                InnerEnd is B - 1)),
        ChildRanges).
childrenHelper(ChildSizes, ChildRanges, HMinusOne, Children) :-
    zip(ChildSizes, ChildRanges, ZipResult),
    map(ZipResult,
        lambda([pair(Size, pair(RangeStart, RangeEnd)),
                Child],
               makeNonRootBTree(Size, RangeStart, RangeEnd, HMinusOne, Child)),
        Children).
makeBTree(NChildren, Size, KeyRangeStart, KeyRangeEnd, H, b_tree(Keys, Children)) :-
    HMinusOne is H - 1,
    yolo_UNSAFE_minChildSize(HMinusOne, MinChildSizeBelow),
    yolo_UNSAFE_maxChildSize(HMinusOne, MaxChildSizeBelow),
    NKeys is NChildren - 1,
    RestOfNodes is Size - NKeys - NChildren * MinChildSizeBelow,
    RestOfNodes >= 0,
    KeyRangeSize is KeyRangeEnd - KeyRangeStart + 1,
    AddKeysAmount is KeyRangeSize - Size,
    AddListMax is MaxChildSizeBelow - MinChildSizeBelow,
    getAdditions(NChildren, RestOfNodes, AddListMax, AddList),
    map(AddList, lambda([Current, CurrentAdded], CurrentAdded is Current + MinChildSizeBelow), ChildSizes),
    getAdditions(NChildren, AddKeysAmount, KeyRangeSize, AddKeys),
    keysHelper(ChildSizes, AddKeys, KeyRangeStart, Keys),
    childRangesHelper(KeyRangeStart, Keys, KeyRangeEnd, ChildRanges),
    childrenHelper(ChildSizes, ChildRanges, HMinusOne, Children).
clausedef(b_tree_main, [], [int, b_tree]).
b_tree_main(Size, Tree) :-
    makeRootBTree(Size, Tree).
