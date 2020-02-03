module(binary_search_trees,
       [bst_main/2],
       [bst]).

use_module('inclusive.pl',
           [until/3, inInclusiveRange/3],
           []).

datadef(bst, [], [bst_leaf,
                  bst_node(bst, int, bst)]).

clausedef(makeBSTNodeBound, [], [int, % size
                                 int, % range start
                                 int, % range end
                                 bst]).
makeBSTNodeBound(Size, _, _, bst_leaf) :-
    Size =< 0,
    !.
makeBSTNodeBound(1, RangeStart, RangeEnd, bst_node(bst_leaf, I, bst_leaf)) :-
    !,
    inInclusiveRange(RangeStart, RangeEnd, I).
makeBSTNodeBound(Size, RangeStart, RangeEnd, bst_node(Left, Median, Right)) :-
    until(0, Size, LeftSize),
    RightSize is Size - LeftSize - 1,
    StartBetween is RangeStart + LeftSize,
    EndBetween is RangeEnd - RightSize,
    inInclusiveRange(StartBetween, EndBetween, Median),
    MedianMinusOne is Median - 1,
    MedianPlusOne is Median + 1,
    makeBSTNodeBound(LeftSize, RangeStart, MedianMinusOne, Left),
    makeBSTNodeBound(RightSize, MedianPlusOne, RangeEnd, Right).

clausedef(bst_main, [], [int, bst]).
bst_main(Size, BST) :-
    makeBSTNodeBound(Size, 1, Size, BST).
