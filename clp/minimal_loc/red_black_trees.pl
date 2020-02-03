inInclusiveRange(Start, End, Start) :-
    Start =< End.
inInclusiveRange(Start, End, Result) :-
    Start < End,
    NewStart is Start + 1,
    inInclusiveRange(NewStart, End, Result).
until(Start, End, Result) :-
    NewEnd is End - 1,
    inInclusiveRange(Start, NewEnd, Result).
calcChildColors(0, 1, 1) :- !.
calcChildColors(_, 0, 1).
calcChildBlackHeight(1, BlackHeight, Result) :-
    !,
    Result is BlackHeight - 1.
calcChildBlackHeight(_, BlackHeight, BlackHeight).
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
yolo_UNSAFE_blackHeightRange(Size, Start, End) :-
    log2Int(Size, Start),
    SizePlusOne is Size + 1,
    log2Int(SizePlusOne, Temp),
    End is Temp + 1.
makeRBTreeMeasure(Size, Tree) :-
    yolo_UNSAFE_blackHeightRange(Size, Start, End),
    inInclusiveRange(Start, End, BlackHeight),
    makeRBTree(Size, 1, Size, 0, 1, BlackHeight, Tree).
rb_main(Size, Tree) :-
    makeRBTreeMeasure(Size, Tree).
