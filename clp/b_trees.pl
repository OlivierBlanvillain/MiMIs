module(b_trees,
       [b_tree_main/2],
       [b_tree]).

use_module('inclusive.pl',
           [inInclusiveRange/3, inInclusiveRangeList/3],
           []).

datadef(pair, [A, B], [pair(A, B)]).
datadef(b_tree, [], [b_tree(list(int), list(b_tree))]).

clausedef(yolo_UNSAFE_minChildSize, [], [int, int]).
yolo_UNSAFE_minChildSize(H, Output) :-
    HMinusOne is H - 1,
    powInt(2, HMinusOne, PowResult),
    Output is 2 * PowResult - 1.

clausedef(yolo_UNSAFE_maxChildSize, [], [int, int]).
yolo_UNSAFE_maxChildSize(H, Output) :-
    powInt(4, H, Temp),
    Output is Temp - 1.

clausedef(getAdditions, [], [int,         % input size
                             int,         % input amount
                             int,         % input max
                             list(int)]). % output additions
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

clausedef(sublist, [A], [list(A), list(A)]).
sublist([], []).
sublist([H|T], [H|Rest]) :-
    sublist(T, Rest).
sublist([_|T], Rest) :-
    sublist(T, Rest).

clausedef(sublistOfSize, [A], [list(A), int, list(A)]).
sublistOfSize(Input, Size, Output) :-
    length(Output, Size),
    sublist(Input, Output).

clausedef(yolo_UNSAFE_rootBTreeHeight, [], [int, int]).
yolo_UNSAFE_rootBTreeHeight(Size, Height) :-
    SizePlusOne is Size + 1,
    logInt(SizePlusOne, LogResult),
    LogResultPlusOne is LogResult + 1,
    inInclusiveRange(1, LogResultPlusOne, Height).

clausedef(makeRootBTree, [], [int,      % input size
                              b_tree]). % output b tree
makeRootBTree(Size, Result) :-
    yolo_UNSAFE_rootBTreeHeight(Size, Height),
    makeRootBTree(Size, 1, Size, Height, Result).

clausedef(makeRootBTree, [], [int,      % input size
                              int,      % input key range start
                              int,      % input key range end
                              int,      % input h (height)
                              b_tree]). % output tree
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

clausedef(makeNonRootBTree, [], [int,      % input size
                                 int,      % input key range start
                                 int,      % input key range end
                                 int,      % input h (height)
                                 b_tree]). % output tree
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

clausedef(map, [A, B], [list(A),
                        relation([A, B]),
                        list(B)]).
map([], _, []).
map([A|As], Relation, [B|Bs]) :-
    call(Relation, A, B),
    map(As, Relation, Bs).

clausedef(zip, [A, B], [list(A),
                        list(B),
                        list(pair(A, B))]).
zip([], [], []).
zip([A|As], [B|Bs], [pair(A, B)|Rest]) :-
    zip(As, Bs, Rest).

clausedef(scanLeft, [A, B], [list(A),
                             B,
                             relation([B, A, B]),
                             list(B)]).
scanLeft([], B, _, [B]).
scanLeft([A|As], B, Relation, [B|Bs]) :-
    call(Relation, B, A, NewB),
    scanLeft(As, NewB, Relation, Bs).

clausedef(tail, [A], [list(A), list(A)]).
tail([_|Rest], Rest).

clausedef(init, [A], [list(A), list(A)]).
init([_], []).
init([A1, A2|As], [A1|Rest]) :-
    init([A2|As], Rest).

clausedef(keysHelper, [], [list(int),   % input childSizes
                           list(int),   % input addKeys
                           int,         % input start key range
                           list(int)]). % output keys
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

clausedef(childRangesHelper, [], [int,                    % input key range start
                                  list(int),              % input keys
                                  int,                    % input key range end
                                  list(pair(int, int))]). % child ranges
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

clausedef(childrenHelper, [], [list(int),            % input child sizes
                               list(pair(int, int)), % input child ranges
                               int,                  % input h minus one(height)
                               list(b_tree)]).       % children
childrenHelper(ChildSizes, ChildRanges, HMinusOne, Children) :-
    zip(ChildSizes, ChildRanges, ZipResult),
    map(ZipResult,
        lambda([pair(Size, pair(RangeStart, RangeEnd)),
                Child],
               makeNonRootBTree(Size, RangeStart, RangeEnd, HMinusOne, Child)),
        Children).

clausedef(makeBTree, [], [int,      % input number of children
                          int,      % input size
                          int,      % input key range start
                          int,      % input key range end
                          int,      % input h (height)
                          b_tree]). % output tree
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
