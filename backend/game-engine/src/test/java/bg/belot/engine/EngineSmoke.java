package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
public class EngineSmoke {
 public static void main(String[] args){
  if(deck().size()!=32||new HashSet<>(shuffle()).size()!=32)throw new AssertionError("deck");
  MatchState state=new MatchState();for(List<Card> h:state.hands)if(h.size()!=5)throw new AssertionError("initial deal");
  state.bid(1,"CLUBS");state.bid(2,"PASS");state.bid(3,"PASS");state.bid(0,"PASS");
  for(List<Card> h:state.hands)if(h.size()!=8)throw new AssertionError("final deal");
  while(state.phase==MatchState.Phase.PLAYING){int seat=state.turn;if(state.tricks.isEmpty()&&!state.declarationDecisions.contains(seat))state.declare(seat,List.of());List<Card> legal=state.legalFor(seat);if(legal.isEmpty())throw new AssertionError("no legal cards");state.play(seat,legal.get(0));}
  if(state.tricks.size()!=8||state.score[0]+state.score[1]==0)throw new AssertionError("deal scoring");
  System.out.println("PASS: deck, shuffle, initial/final deal, legal play, eight tricks, basic scoring: "+Arrays.toString(state.score));
 }
}
