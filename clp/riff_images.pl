module(riff_images,
       [riff_main/2],
       [riff_chunk]).

use_module('inclusive.pl',
           [inInclusiveRange/3, until/3],
           []).

datadef(riff_chunk, [], [riff_leaf,
                         riff_payload(int, int, int),
                         riff_node(int, riff_chunk, riff_chunk)]).

clausedef(makeRiff, [], [int,          % input size
                         int,          % input data size
                         int,          % input jiff loss
                         int,          % input av chunks
                         riff_chunk]). % output riff
makeRiff(0, 0, _, _, riff_leaf) :- !.
makeRiff(1, DataSize, JiffLoss, AvChunks,
         riff_payload(DataSize, OutputDataSize, AvChunks)) :-
    DataSize > 0,
    AvChunks =< 1,
    JiffLossTimesFour is JiffLoss * 4,
    0 is mod(JiffLossTimesFour, DataSize),
    !,
    OutputDataSize is truncate(JiffLossTimesFour / DataSize).
makeRiff(Size, DataSize, JiffLoss, AvChunks,
         riff_node(DataSize, LeftTree, RightTree)) :-
    SizeMinusOne is Size - 1,
    until(0, SizeMinusOne, LeftSize),
    RightTreeSize is Size - LeftSize - 1,
    LeftAudioStart is max(0, AvChunks - (Size - LeftSize - 1)),
    LeftAudioEnd is min(LeftSize, AvChunks),
    inInclusiveRange(LeftAudioStart, LeftAudioEnd, LeftAudio),
    RightAvChunks is AvChunks - LeftAudio,
    DataSizeDivTwo is truncate(DataSize / 2),
    inInclusiveRange(0, DataSizeDivTwo, LeftDataSize),
    RightDataSize is DataSize - LeftDataSize,
    LeftJiffStart is max(0, JiffLoss - (DataSize - LeftDataSize)),
    LeftJiffEnd is min(LeftDataSize, JiffLoss),
    inInclusiveRange(LeftJiffStart, LeftJiffEnd, LeftJiff),
    RightJiffLoss is JiffLoss - LeftJiff,
    makeRiff(LeftSize, LeftDataSize, LeftJiff, LeftAudio, LeftTree),
    makeRiff(RightTreeSize, RightDataSize, RightJiffLoss, RightAvChunks, RightTree).

clausedef(makeRiff, [], [int, riff_chunk]).
makeRiff(Size, Riff) :-
    JiffLoss is truncate((Size + 1) / 2),
    AvChunks is truncate(Size / 2),
    makeRiff(Size, Size, JiffLoss, AvChunks, Riff).

clausedef(riff_main, [], [int, riff_chunk]).
riff_main(Size, Riff) :-
    makeRiff(Size, Riff).
