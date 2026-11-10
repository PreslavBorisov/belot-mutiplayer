package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
public final class AnnouncementRegression {
 static Card c(Suit s,Rank r){return new Card(s,r);}
 public static void main(String[] args)throws Exception{
  List<Card> hand=new ArrayList<>();for(Suit suit:Suit.values())hand.add(c(suit,Rank.JACK));
  hand.addAll(List.of(c(Suit.HEARTS,Rank.TEN),c(Suit.HEARTS,Rank.QUEEN),c(Suit.HEARTS,Rank.KING),c(Suit.CLUBS,Rank.ACE)));
  List<Declaration> choices=detect(hand,0,Contract.ALL_TRUMP);
  if(choices.size()!=1||!choices.get(0).kind().equals("FOUR"))throw new AssertionError("Sequence reused four-of-a-kind card");
  MatchState g=new MatchState();g.bid(g.turn,"ALL_TRUMP");for(int i=0;i<3;i++)g.bid(g.turn,"PASS");
  int seat=g.turn;g.hands.get(seat).clear();g.hands.get(seat).addAll(hand);
  var field=MatchState.class.getDeclaredField("declarationOptions");field.setAccessible(true);
  @SuppressWarnings("unchecked") List<List<Declaration>> options=(List<List<Declaration>>)field.get(g);
  options.set(seat,detect(hand,seat,g.contract));
  try{g.declare(seat,List.of(0,999));throw new AssertionError("Invalid selection accepted");}catch(IllegalArgumentException expected){}
  if(!g.declarations.isEmpty()||g.declarationDecisions.contains(seat))throw new AssertionError("Rejected selection partially saved");
  g.declare(seat,List.of(0));
  try{g.declare(seat,List.of(0));throw new AssertionError("Duplicate accepted");}catch(IllegalArgumentException expected){}
  if(g.declarations.size()!=1)throw new AssertionError("Declaration counted twice");
  g.current.add(new Play((seat+3)%4,c(Suit.CLUBS,Rank.SEVEN)));
  g.hands.get(seat).clear();Card queen=c(Suit.HEARTS,Rank.QUEEN);g.hands.get(seat).addAll(List.of(queen,c(Suit.HEARTS,Rank.KING)));
  if(!g.legalFor(seat).contains(queen))throw new AssertionError("Discard should be legal");
  if(g.canAnnounceBelote(seat,queen))throw new AssertionError("Off-suit all-trump belote accepted");
  try{g.play(seat,queen,true);throw new AssertionError("Server accepted off-suit belote");}catch(IllegalArgumentException expected){}
  if(!g.belotePending.isEmpty())throw new AssertionError("Invalid belote changed state");
  g.current.clear();g.current.add(new Play((seat+3)%4,c(Suit.HEARTS,Rank.SEVEN)));
  if(!g.canAnnounceBelote(seat,queen))throw new AssertionError("Following suit belote rejected");
  g.current.clear();if(!g.canAnnounceBelote(seat,queen))throw new AssertionError("Leading belote rejected");
  MatchState b=new MatchState();int bidder=b.turn;b.bid(bidder,"CLUBS");for(int i=0;i<3;i++)b.bid(b.turn,"PASS");
  if(!Objects.equals(b.highestBidder,bidder))throw new AssertionError("Contract bidder lost after passes");
  b.newDeal();if(b.highestBidder!=null)throw new AssertionError("Old bidder survived new deal");
  System.out.println("AnnouncementRegression passed");
 }
}
