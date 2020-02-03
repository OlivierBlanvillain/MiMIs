inInclusiveRange(Start, End, Start) :-
    Start =< End.
inInclusiveRange(Start, End, Result) :-
    Start < End,
    NewStart is Start + 1,
    inInclusiveRange(NewStart, End, Result).
until(Start, End, Result) :-
    NewEnd is End - 1,
    inInclusiveRange(Start, NewEnd, Result).
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
bst_main(Size, BST) :-
    makeBSTNodeBound(Size, 1, Size, BST).
