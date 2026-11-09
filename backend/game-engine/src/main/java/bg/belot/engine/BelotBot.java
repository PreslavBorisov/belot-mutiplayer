package bg.belot.engine;

import java.util.*;
import static bg.belot.engine.BelotEngine.*;

/** Deterministic heuristic opponent; never reads another player's hidden hand. */
public final class BelotBot {
 private BelotBot(){}
 public static String bid(MatchState g,int seat){
  if(g.contra!=1 || (g.highest!=null && g.contractTeam==team(seat)))return "PASS";
  List<Card> hand=g.hands.get(seat);
  Contract best=null;double bestStrength=0;
  for(Contract c:Contract.values()){
   if(g.highest!=null && priority(c)<=priority(g.highest))continue;
   double strength;
   if(c==Contract.ALL_TRUMP){
    long jacks=hand.stream().filter(x->x.rank()==Rank.JACK).count();
    long nines=hand.stream().filter(x->x.rank()==Rank.NINE).count();
    strength=jacks>=2 ? (jacks*2.5+nines*1.5)/7.0 : 0;
   }else if(c==Contract.NO_TRUMP){
    long aces=hand.stream().filter(x->x.rank()==Rank.ACE).count();
    long tens=hand.stream().filter(x->x.rank()==Rank.TEN).count();
    strength=aces>=2 ? (aces*3+tens)/8.0 : 0;
   }else{
    List<Card> trumps=hand.stream().filter(x->trump(x,c)).toList();
    double control=trumps.stream().mapToDouble(x->switch(x.rank()){
     case JACK->4;case NINE->3;case ACE->2;case TEN->1;default->0.5;
    }).sum();
    long sideAces=hand.stream().filter(x->!trump(x,c)&&x.rank()==Rank.ACE).count();
    strength=trumps.size()>=3 ? (control+sideAces*1.5)/8.0 : 0;
   }
   if(strength>bestStrength){bestStrength=strength;best=c;}
  }
  return best!=null&&bestStrength>=1 ? best.name():"PASS";
 }

 public static List<Integer> declarations(MatchState g,int seat){
  List<Declaration> choices=g.availableDeclarations(seat);
  List<Integer> selected=new ArrayList<>();
  Set<Card> used=new HashSet<>();
  List<Integer> order=new ArrayList<>();
  for(int i=0;i<choices.size();i++)order.add(i);
  order.sort((a,b)->Integer.compare(choices.get(b).points(),choices.get(a).points()));
  for(int i:order){
   Declaration d=choices.get(i);
   if(d.cards().stream().noneMatch(used::contains)){
    selected.add(i);used.addAll(d.cards());
   }
  }
  return selected;
 }
 public static Card play(MatchState g,int seat){
  List<Card> legal=g.legalFor(seat);
  if(legal.isEmpty())throw new IllegalStateException("No legal move");
  if(g.current.isEmpty()){
   return legal.stream().max(Comparator.comparingInt((Card c)->points(c,g.contract)+power(c,g.contract)*2)).orElseThrow();
  }
  int currentWinner=g.current.get(winningIndex(g.current,g.contract)).seat();
  boolean partnerWinning=team(currentWinner)==team(seat);
  if(partnerWinning)return legal.stream().min(Comparator.comparingInt(c->points(c,g.contract)*10+power(c,g.contract))).orElseThrow();
  List<Card> winning=new ArrayList<>();
  for(Card c:legal){
   List<Play> candidate=new ArrayList<>(g.current);candidate.add(new Play(seat,c));
   if(winningIndex(candidate,g.contract)==candidate.size()-1)winning.add(c);
  }
  if(!winning.isEmpty())return winning.stream().min(Comparator.comparingInt(c->power(c,g.contract)*10+points(c,g.contract))).orElseThrow();
  return legal.stream().min(Comparator.comparingInt(c->points(c,g.contract)*10+power(c,g.contract))).orElseThrow();
 }
}
