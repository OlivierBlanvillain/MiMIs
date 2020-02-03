% typed-prolog intentionally lacks log, as this would require a floating-point type
% This is a non-trivial change to make, since we'd then need to do reasoning
% like "what happens if I add a float and an int"?
% As such, we write this in regular Prolog
% We need to append this stub to the end of the compiled file.

logInt(Input, Output) :-
    Output is truncate(log(Input)).

log2Int(Input, Output) :-
    Output is truncate(log(Input) / log(2)).

powInt(Base, Exponent, Output) :-
    % Base ^ Exponent makes more sense, bug some versions
    % of GNU Prolog treat this as XOR, despite what the
    % manual says.
    Output is truncate(Base ** Exponent).

