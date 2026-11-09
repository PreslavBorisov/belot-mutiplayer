package bg.belot.engine;

import java.util.*;
import static bg.belot.engine.BelotEngine.*;

/** Deterministic heuristic opponent; never reads another player's hidden hand. */
public final class BelotBot {
 private BelotBot(){}
 public static String bid(MatchState g,int seat){
  if(g.highest!=null)return "PASS";
  List<Card> hand=g.hands.get(seat);
  Contract best=Contract.CLUBS;int max=-1;
  for(Contract c:Contract.values()){
   int value=hand.stream().mapToInt(card->points(card,c)+(trump(card,c)?power(card,c)*2:0)).sum();
   if(value>max){max=value;best=c;}
  }
  return max>=30?best.name():"PASS";
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
