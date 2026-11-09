package bg.belot.engine;

import java.util.*;
import static bg.belot.engine.BelotEngine.*;

/** Run with java -ea. Simulates complete deals through the public state machine. */
public final class BotMatchRegression {
 public static void main(String[] args){
  for(int attempt=0;attempt<30;attempt++){
   MatchState game=new MatchState();
   int actions=0;
   while(game.phase==MatchState.Phase.BIDDING&&actions++<100){
    int seat=game.turn;
    game.bid(seat,BelotBot.bid(game,seat));
   }
   if(game.phase!=MatchState.Phase.PLAYING)throw new AssertionError("Bidding never ended");
   while(game.phase==MatchState.Phase.PLAYING&&actions++<150){
    int seat=game.turn;
    if(game.tricks.isEmpty()&&!game.declarationDecisions.contains(seat))game.declare(seat,BelotBot.declarations(game,seat));
    if(hasBelote(game.hands.get(seat),game.contract)&&!game.beloteClaims.contains(seat)&&!game.belotePending.containsKey(seat))game.claimBelote(seat);
    Card card=BelotBot.play(game,seat);
    if(!game.legalFor(seat).contains(card))throw new AssertionError("Bot played illegal card");
    game.play(seat,card);
   }
   if(game.tricks.size()!=8)throw new AssertionError("Expected eight completed tricks");
   if(game.phase!=MatchState.Phase.DEAL_DONE&&game.phase!=MatchState.Phase.FINISHED)throw new AssertionError("Deal did not finish");
   if(game.score[0]<0||game.score[1]<0)throw new AssertionError("Negative score");
  }
  System.out.println("BotMatchRegression passed");
 }
}
