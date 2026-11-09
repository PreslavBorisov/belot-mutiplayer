package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
public class BeloteTimingRegression {
 public static void main(String[] args){
  MatchState g=new MatchState();g.bid(g.turn,"HEARTS");for(int i=0;i<3;i++)g.bid(g.turn,"PASS");
  int seat=g.turn;Card king=new Card(Suit.HEARTS,Rank.KING),queen=new Card(Suit.HEARTS,Rank.QUEEN),ace=new Card(Suit.HEARTS,Rank.ACE);
  g.hands.get(seat).clear();g.hands.get(seat).addAll(List.of(king,queen,ace));g.declarationDecisions.add(seat);
  try{g.claimBelote(seat);throw new AssertionError("Early belote accepted");}catch(IllegalArgumentException expected){}
  if(!g.belotePending.isEmpty())throw new AssertionError("Early announcement leaked");
  try{g.play(seat,ace,true);throw new AssertionError("Ace belote accepted");}catch(IllegalArgumentException expected){}
  if(!g.hands.get(seat).contains(ace)||!g.current.isEmpty())throw new AssertionError("Rejected move changed state");
  g.play(seat,king,true);
  if(!g.belotePending.get(seat).equals(Set.of(Suit.HEARTS)))throw new AssertionError("Belote not announced with king");
  MatchState n=new MatchState();n.phase=MatchState.Phase.PLAYING;n.contract=Contract.NO_TRUMP;n.hands.get(n.turn).clear();n.hands.get(n.turn).addAll(List.of(king,queen));
  if(n.canAnnounceBelote(n.turn,king))throw new AssertionError("No trump belote allowed");
  System.out.println("BeloteTimingRegression passed");
 }
}
