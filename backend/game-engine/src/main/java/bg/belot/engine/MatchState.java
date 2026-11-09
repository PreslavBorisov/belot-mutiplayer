package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
/** Single-room state machine. All mutating methods synchronized; caller persists snapshots. */
public final class MatchState {
 public enum Phase { BIDDING, PLAYING, DEAL_DONE, FINISHED }
 public final List<List<Card>> hands=new ArrayList<>();public final List<List<Play>> tricks=new ArrayList<>();public final List<Play> current=new ArrayList<>();public final int[] score={0,0};
 public int dealer=0,turn=1,contractTeam=-1,passCount=0,contra=1,pending=0,dealNumber=0,capotLock=0;public int contraTeam=-1;public long version=0;public Phase phase;public Contract contract;public Contract highest;public final List<String> history=new ArrayList<>();public final List<Declaration> declarations=new ArrayList<>();public final Set<Integer> declarationDecisions=new HashSet<>();public final Set<Integer> beloteClaims=new HashSet<>();public final Map<Integer,Set<Suit>> belotePending=new HashMap<>();private List<Card> stock;private final List<List<Declaration>> declarationOptions=new ArrayList<>();
 public MatchState(){newDeal();}
 public synchronized void newDeal(){stock=shuffle();hands.clear();for(int i=0;i<4;i++)hands.add(new ArrayList<>());int at=0;for(int packet:List.of(3,2)){for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<packet;j++)hands.get(seat).add(stock.get(at++));}}stock=new ArrayList<>(stock.subList(at,32));tricks.clear();current.clear();declarations.clear();declarationDecisions.clear();beloteClaims.clear();belotePending.clear();declarationOptions.clear();highest=null;contract=null;contractTeam=-1;passCount=0;contra=1;contraTeam=-1;turn=(dealer+1)%4;phase=Phase.BIDDING;dealNumber++;version++;}
 public synchronized void bid(int seat,String action){require(Phase.BIDDING,seat);if(action.equals("PASS")){passCount++;if(highest==null&&passCount==4){dealer=(dealer+1)%4;newDeal();return;}if(highest!=null&&passCount==3){finalizeBid();return;}}else if(action.equals("CONTRA")){if(highest==null||contractTeam==team(seat)||contra!=1)throw new IllegalArgumentException("Contra not allowed");contra=2;contraTeam=team(seat);passCount=0;}else if(action.equals("RECONTRA")){if(highest==null||contractTeam!=team(seat)||contra!=2||contraTeam==team(seat))throw new IllegalArgumentException("Recontra not allowed");contra=4;passCount=0;}else{Contract c=Contract.valueOf(action);if(highest!=null&&priority(c)<=priority(highest))throw new IllegalArgumentException("Bid must be higher");if(contra!=1)throw new IllegalArgumentException("Contract locked by contra");highest=c;contractTeam=team(seat);passCount=0;}turn=(turn+1)%4;version++;}
 private void finalizeBid(){contract=highest;int k=0;for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<3;j++)hands.get(seat).add(stock.get(k++));}stock=List.of();for(int i=0;i<4;i++)declarationOptions.add(detect(List.copyOf(hands.get(i)),i,contract));phase=Phase.PLAYING;turn=(dealer+1)%4;version++;}
 public synchronized List<Declaration> availableDeclarations(int seat){
  if(seat<0||seat>=4||phase!=Phase.PLAYING||!tricks.isEmpty())return List.of();
  return declarationOptions.get(seat);
 }
 public synchronized void declare(int seat,List<Integer> selected){
  if(phase!=Phase.PLAYING||!tricks.isEmpty())throw new IllegalArgumentException("Declarations only during first trick");
  if(seat!=turn||declarationDecisions.contains(seat))throw new IllegalArgumentException("Already declared");
  List<Declaration> options=availableDeclarations(seat);
  if(selected==null)throw new IllegalArgumentException("Missing selection");
  Set<Integer> unique=new HashSet<>(selected);
  if(unique.size()!=selected.size())throw new IllegalArgumentException("Duplicate declaration");
  Set<Card> used=new HashSet<>();for(int index:selected){
   if(index<0||index>=options.size())throw new IllegalArgumentException("Invalid declaration");
   Declaration d=options.get(index);
   for(Card card:d.cards())if(!used.add(card))throw new IllegalArgumentException("A card cannot be in multiple declarations");
   declarations.add(d);
  }
  declarationDecisions.add(seat);version++;
 }
 public synchronized List<Card> legalFor(int seat){return phase==Phase.PLAYING&&seat==turn?legal(hands.get(seat),current,contract,seat):List.of();}
 public synchronized void play(int seat,Card card){require(Phase.PLAYING,seat);if(!legalFor(seat).contains(card))throw new IllegalArgumentException("Illegal card");if(tricks.isEmpty()&&!declarationDecisions.contains(seat))throw new IllegalArgumentException("Choose declarations before playing your first card");hands.get(seat).remove(card);current.add(new Play(seat,card));if(belotePending.containsKey(seat)&& (card.rank()==Rank.KING||card.rank()==Rank.QUEEN) && belotePending.get(seat).contains(card.suit())){long played=tricks.stream().flatMap(List::stream).filter(p->p.seat()==seat&&p.card().suit()==card.suit()&&(p.card().rank()==Rank.KING||p.card().rank()==Rank.QUEEN)).count()+current.stream().filter(p->p.seat()==seat&&p.card().suit()==card.suit()&&(p.card().rank()==Rank.KING||p.card().rank()==Rank.QUEEN)).count();boolean bothPlayed=played==2;if(bothPlayed)beloteClaims.add(seat);}if(current.size()==4){List<Play>complete=List.copyOf(current);tricks.add(complete);turn=complete.get(winningIndex(complete,contract)).seat();current.clear();if(tricks.size()==8){finishDeal();return;}}else turn=(turn+1)%4;version++;}
 private int[] resolveDeclarationPoints(){
  int[] result={0,0};
  // Four-of-a-kind and sequences compete in separate categories.
  for(String kind:List.of("FOUR","SEQUENCE")){
   List<Declaration> candidates=declarations.stream().filter(d->kind.equals(d.kind())).toList();
   if(candidates.isEmpty())continue;
   Comparator<Declaration> strength=Comparator.comparingInt(Declaration::points)
     .thenComparingInt(d->d.cards().stream().mapToInt(c->c.rank().ordinal()).max().orElse(-1));
   Declaration strongest=candidates.stream().max(strength).orElseThrow();
   int bestTeam=team(strongest.seat());
   boolean tiedOpposition=candidates.stream().anyMatch(d->team(d.seat())!=bestTeam&&strength.compare(d,strongest)==0);
   if(tiedOpposition)continue;
   for(Declaration d:candidates)if(team(d.seat())==bestTeam)result[bestTeam]+=d.points();
  }
  for(int seat:beloteClaims)result[team(seat)]+=20;
  return result;
 }
 public synchronized void claimBelote(int seat){
  if(phase!=Phase.PLAYING||seat!=turn)throw new IllegalArgumentException("Belote only on your turn");
  if(beloteClaims.contains(seat)||belotePending.containsKey(seat))throw new IllegalArgumentException("Already announced");
  if(!hasBelote(hands.get(seat),contract))throw new IllegalArgumentException("No belote");
  Set<Suit> eligible=new HashSet<>();
  for(Suit suit:Suit.values())if((contract==Contract.ALL_TRUMP||contract.ordinal()==suit.ordinal())&&hands.get(seat).contains(new Card(suit,Rank.KING))&&hands.get(seat).contains(new Card(suit,Rank.QUEEN)))eligible.add(suit);
  belotePending.put(seat,eligible);version++;
 }
 private List<Card> originalHandFor(int seat){
  List<Card> cards=new ArrayList<>(hands.get(seat));
  for(List<Play> trick:tricks)for(Play p:trick)if(p.seat()==seat)cards.add(p.card());
  for(Play p:current)if(p.seat()==seat)cards.add(p.card());
  return cards;
 }
 private void finishDeal(){
  int[] raw=trickPoints(tricks,contract);int[] declarationPoints=resolveDeclarationPoints();raw[0]+=declarationPoints[0];raw[1]+=declarationPoints[1];
  int defenders=1-contractTeam;
  int total=raw[0]+raw[1];
  boolean contractMade=raw[contractTeam]>raw[defenders];
  int[] added=new int[2];
  // A failed contract awards the entire deal to the defenders.
  // Equal trick points are held as pending; defenders receive their own points.
  if(raw[contractTeam]==raw[defenders]){
   added[defenders]=(raw[defenders]*contra)/10;
   pending+=(raw[contractTeam]*contra+9)/10;
  }else if(!contractMade){
   added[defenders]=(total*contra+5)/10;
  }else if(contra>1){
   added[contractTeam]=(total*contra+5)/10;
  }else{
   added[0]=(raw[0]+5)/10;
   added[1]=(raw[1]+5)/10;
  }
  if(raw[contractTeam]!=raw[defenders]){
   added[contractMade?contractTeam:defenders]+=pending;
   pending=0;
  }
  score[0]+=added[0];score[1]+=added[1];
  boolean capot=tricks.stream().allMatch(t->team(t.get(winningIndex(t,contract)).seat())==team(tricks.get(0).get(winningIndex(tricks.get(0),contract)).seat()));
  history.add("Deal "+dealNumber+" "+contract+" raw="+Arrays.toString(raw)+" score="+Arrays.toString(score)+" pending="+pending+" made="+contractMade);
  if(capot)capotLock=1;else if(capotLock>0)capotLock=0;
  if(capotLock==0&&Math.max(score[0],score[1])>=151&&score[0]!=score[1])phase=Phase.FINISHED;
  else phase=Phase.DEAL_DONE;
  version++;
 }
 public synchronized void nextDeal(){if(phase!=Phase.DEAL_DONE)throw new IllegalStateException("Not ready");dealer=(dealer+1)%4;newDeal();}
 private void require(Phase expected,int seat){if(phase!=expected||turn!=seat)throw new IllegalArgumentException("Wrong phase or turn");}
}
