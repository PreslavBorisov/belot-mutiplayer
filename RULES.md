# Rules implementation status
Authoritative source: https://belot.bg/belot/rules/ (retrieved 2026-11-09).

## Verified source rules
32 cards, counterclockwise 3+2 then 3 deal, bidding clubs < diamonds < hearts < spades < no trump < all trump, three consecutive passes after a bid, four passes redeal. Following suit mandatory. All-trump overtake mandatory when possible. No-trump follow suit only. In suit contracts, trump when void and opposing team winning; overtrump if possible. Last trick +10; capot +90. Declarations: sequences 20/50/100, four 10/Q/K/A 100, four nines 150, four jacks 200; belote 20. No trump disallows ordinary announcements. Opposing sequence and four-of-kind categories are compared independently. Same card cannot simultaneously score in a sequence and four. Contra x2, recontra x4. Match 151, capot cannot finish match.

## Implemented and incomplete
Pure engine: deck, shuffle, preliminary/final dealing, bidding, card legality, trick resolution, basic point totals, sequence/four detection helpers, belote ownership detection. Engine smoke test passes.
NOT YET IMPLEMENTED: declaration submission/timing, cross-team declaration resolution, belote announcement, official score rounding and tie carry, correct pending contra/recontra, match completion edge cases, correct contra turn restrictions, session security, persistence, websocket synchronization, bots. Current MatchState scoring is a deliberately incomplete approximation and MUST NOT be considered Belot.BG compliant. No-trump card points are doubled correctly, but declaration and rounding pipeline is not finished.

## Important ambiguity / implementation work
The official source says 154-154 all trump rounds to 15 for non-bidder and 16 pending, and 106-106 suit rounds to 10 for non-bidder and 11 pending. General rounding needs exact test vectors before release. The source specifies belote must be announced when playing the first king/queen in certain legal situations; ownership alone must not award points.
