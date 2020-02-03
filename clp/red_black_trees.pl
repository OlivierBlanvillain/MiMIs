module(red_black_trees,
       [rb_main/2],
       [bool, rb_tree]).

use_module('inclusive.pl',
           [until/3, inInclusiveRange/3],
           []).

datadef(bool, [], [yes, no]).
datadef(rb_tree, [], [rb_leaf,
                      rb_node(rb_tree, int, rb_tree, bool)]).

clausedef(calcChildColors, [], [int,   % my color
                                int,   % child color start
                                int]). % child color end
calcChildColors(0, 1, 1) :- !.
calcChildColors(_, 0, 1).

clausedef(calcChildBlackHeight, [], [int,   % my color
                                     int,   % black height
                                     int]). % output
calcChildBlackHeight(1, BlackHeight, Result) :-
    !,
    Result is BlackHeight - 1.
calcChildBlackHeight(_, BlackHeight, BlackHeight).

clausedef(makeRBTree, [], [int,       % size
                           int,       % range start
                           int,       % range end
                           int,       % colors start
                           int,       % colors end
                           int,       % black height
                           rb_tree]). % generated tree
makeRBTree(Size, RangeStart, RangeEnd, _, _, _, _) :-
    RangeSize is RangeEnd - RangeStart + 1,
    RangeSize >= Size,
    RangeSize < 0,
    !,
    fail.
makeRBTree(_, _, _, _, _, BlackHeight, _) :-
    BlackHeight < 0,
    !,
    fail.
makeRBTree(0, _, _, _, ColorsEnd, 1, rb_leaf) :-
    ColorsEnd >= 1,
    !.
makeRBTree(Size, RangeStart, RangeEnd, ColorsStart, ColorsEnd, BlackHeight, rb_node(Left, Median, Right, MyColorComparison)) :-
    Size > 0,
    BlackHeight >= 1,
    !,
    until(0, Size, LeftSize),
    RightSize is Size - LeftSize - 1,
    StartBetween is RangeStart + LeftSize,
    EndBetween is RangeEnd - RightSize,
    inInclusiveRange(StartBetween, EndBetween, Median),
    inInclusiveRange(ColorsStart, ColorsEnd, MyColor),
    calcChildColors(MyColor, ChildColorStart, ChildColorEnd),
    calcChildBlackHeight(MyColor, BlackHeight, ChildBlackHeight),
    MedianMinusOne is Median - 1,
    MedianPlusOne is Median + 1,
    makeRBTree(LeftSize, RangeStart, MedianMinusOne, ChildColorStart, ChildColorEnd, ChildBlackHeight, Left),
    makeRBTree(RightSize, MedianPlusOne, RangeEnd, ChildColorStart, ChildColorEnd, ChildBlackHeight, Right),
    (MyColor == 1 ->
         MyColorComparison = yes;
         MyColorComparison = no).

clausedef(yolo_UNSAFE_blackHeightRange, [], [int,   % input size
                                             int,   % output start range
                                             int]). % output end range
yolo_UNSAFE_blackHeightRange(Size, Start, End) :-
    log2Int(Size, Start),
    SizePlusOne is Size + 1,
    log2Int(SizePlusOne, Temp),
    End is Temp + 1.

clausedef(makeRBTreeMeasure, [], [int, rb_tree]).
makeRBTreeMeasure(Size, Tree) :-
    yolo_UNSAFE_blackHeightRange(Size, Start, End),
    inInclusiveRange(Start, End, BlackHeight),
    makeRBTree(Size, 1, Size, 0, 1, BlackHeight, Tree).

clausedef(rb_main, [], [int, rb_tree]).
rb_main(Size, Tree) :-
    makeRBTreeMeasure(Size, Tree).
