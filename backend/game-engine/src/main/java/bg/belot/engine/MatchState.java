package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
/** Single-room state machine. All mutating methods synchronized; caller persists snapshots. */
public final class MatchState implements java.io.Serializable {
 private static final long serialVersionUID=1L;
 public enum Phase { BIDDING, PLAYING, DEAL_DONE, FINISHED }
 public final List<List<Card>> hands=new ArrayList<>();public final List<List<Play>> tricks=new ArrayList<>();public final List<Play> current=new ArrayList<>();public final int[] score={0,0};
 public int dealer=0,turn=1,contractTeam=-1,passCount=0,contra=1,pending=0,dealNumber=0,capotLock=0;public int contraTeam=-1;public long version=0;public Phase phase;public Contract contract;public Contract highest;public final List<String> history=new ArrayList<>();public final List<Declaration> declarations=new ArrayList<>();public final Set<Integer> declarationDecisions=new HashSet<>();public final Set<Integer> beloteClaims=new HashSet<>();public final Map<Integer,Set<Suit>> belotePending=new HashMap<>();private List<Card> stock;private final List<List<Declaration>> declarationOptions=new ArrayList<>();
 public Integer highestBidder=null;
 public long dealStartedAt=0;
 public record BidNotice(int seat,String action,long at) implements java.io.Serializable {}
 public List<BidNotice> bidNotices=new ArrayList<>();
 public MatchState(){newDeal();}
 public synchronized void newDeal(){dealStartedAt=System.currentTimeMillis();if(bidNotices==null)bidNotices=new ArrayList<>();bidNotices.clear();stock=shuffle();hands.clear();for(int i=0;i<4;i++)hands.add(new ArrayList<>());int at=0;for(int packet:List.of(3,2)){for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<packet;j++)hands.get(seat).add(stock.get(at++));}}stock=new ArrayList<>(stock.subList(at,32));tricks.clear();current.clear();declarations.clear();declarationDecisions.clear();beloteClaims.clear();belotePending.clear();declarationOptions.clear();highest=null;highestBidder=null;contract=null;contractTeam=-1;passCount=0;contra=1;contraTeam=-1;turn=(dealer+1)%4;phase=Phase.BIDDING;dealNumber++;version++;}
 public synchronized void bid(int seat,String action){require(Phase.BIDDING,seat);int biddingDeal=dealNumber;if(action.equals("PASS")){passCount++;if(highest==null&&passCount==4){dealer=(dealer+1)%4;newDeal();return;}if(highest!=null&&passCount==3){recordBid(seat,action);finalizeBid();return;}}else if(action.equals("CONTRA")){if(highest==null||contractTeam==team(seat)||contra!=1)throw new IllegalArgumentException("Contra not allowed");contra=2;contraTeam=team(seat);passCount=0;}else if(action.equals("RECONTRA")){if(highest==null||contractTeam!=team(seat)||contra!=2||contraTeam==team(seat))throw new IllegalArgumentException("Recontra not allowed");contra=4;passCount=0;}else{Contract c=Contract.valueOf(action);if(highest!=null&&priority(c)<=priority(highest))throw new IllegalArgumentException("Bid must be higher");if(contra!=1)throw new IllegalArgumentException("Contract locked by contra");highest=c;highestBidder=seat;contractTeam=team(seat);passCount=0;}if(dealNumber==biddingDeal)recordBid(seat,action);turn=(turn+1)%4;version++;}
 private void recordBid(int seat,String action){if(bidNotices==null)bidNotices=new ArrayList<>();bidNotices.removeIf(n->n.seat()==seat);bidNotices.add(new BidNotice(seat,action,System.currentTimeMillis()));}
 private void finalizeBid(){contract=highest;int k=0;for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<3;j++)hands.get(seat).add(stock.get(k++));}stock=List.of();for(int i=0;i<4;i++)declarationOptions.add(detect(List.copyOf(hands.get(i)),i,contract));phase=Phase.PLAYING;turn=(dealer+1)%4;version++;}
 public synchronized List<Declaration> availableDeclarations(int seat){
  if(seat<0||seat>=4||phase!=Phase.PLAYING||!tricks.isEmpty())return List.of();
  List<Declaration> options=declarationOptions.get(seat);
  Set<Card> fourCards=new HashSet<>();for(Declaration d:options)if(d.kind().equals("FOUR"))fourCards.addAll(d.cards());
  return options.stream().filter(d->!d.kind().equals("SEQUENCE")||Collections.disjoint(d.cards(),fourCards)).toList();
 }
 public synchronized void declare(int seat,List<Integer> selected){
  if(phase!=Phase.PLAYING||!tricks.isEmpty())throw new IllegalArgumentException("Declarations only during first trick");
  if(seat!=turn||declarationDecisions.contains(seat))throw new IllegalArgumentException("Already declared");
  List<Declaration> options=availableDeclarations(seat);
  if(selected==null)throw new IllegalArgumentException("Missing selection");
  Set<Integer> unique=new HashSet<>(selected);
  if(unique.size()!=selected.size())throw new IllegalArgumentException("Duplicate declaration");
  Set<Card> used=new HashSet<>();List<Declaration> validated=new ArrayList<>();for(int index:selected){
   if(index<0||index>=options.size())throw new IllegalArgumentException("Invalid declaration");
   Declaration d=options.get(index);
   for(Card card:d.cards())if(!used.add(card))throw new IllegalArgumentException("A card cannot be in multiple declarations");
   validated.add(d);
  }
  declarations.addAll(validated);declarationDecisions.add(seat);version++;
 }
 public synchronized List<Card> legalFor(int seat){return phase==Phase.PLAYING&&seat==turn?legal(hands.get(seat),current,contract,seat):List.of();}
 public synchronized void play(int seat,Card card){play(seat,card,false);}
 public synchronized boolean canAnnounceBelote(int seat,Card card){return phase==Phase.PLAYING&&seat==turn&&legalFor(seat).contains(card)&&(card.rank()==Rank.KING||card.rank()==Rank.QUEEN)&&trump(card,contract)&&(contract!=Contract.ALL_TRUMP||current.isEmpty()||current.get(0).card().suit()==card.suit())&&!belotePending.containsKey(seat)&&!beloteClaims.contains(seat)&&hands.get(seat).contains(new Card(card.suit(),card.rank()==Rank.KING?Rank.QUEEN:Rank.KING));}
 public synchronized void play(int seat,Card card,boolean announceBelote){require(Phase.PLAYING,seat);if(!legalFor(seat).contains(card))throw new IllegalArgumentException("Illegal card");if(tricks.isEmpty()&&!declarationDecisions.contains(seat))throw new IllegalArgumentException("Choose declarations before playing your first card");if(announceBelote){if(!canAnnounceBelote(seat,card))throw new IllegalArgumentException("Belote must accompany a legal trump king or queen");belotePending.put(seat,new HashSet<>(Set.of(card.suit())));}hands.get(seat).remove(card);current.add(new Play(seat,card));if(belotePending.containsKey(seat)&& (card.rank()==Rank.KING||card.rank()==Rank.QUEEN) && belotePending.get(seat).contains(card.suit())){long played=tricks.stream().flatMap(List::stream).filter(p->p.seat()==seat&&p.card().suit()==card.suit()&&(p.card().rank()==Rank.KING||p.card().rank()==Rank.QUEEN)).count()+current.stream().filter(p->p.seat()==seat&&p.card().suit()==card.suit()&&(p.card().rank()==Rank.KING||p.card().rank()==Rank.QUEEN)).count();boolean bothPlayed=played==2;if(bothPlayed)beloteClaims.add(seat);}if(current.size()==4){List<Play>complete=List.copyOf(current);tricks.add(complete);turn=complete.get(winningIndex(complete,contract)).seat();current.clear();if(tricks.size()==8){finishDeal();return;}}else turn=(turn+1)%4;version++;}
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
 public synchronized void claimBelote(int seat){throw new IllegalArgumentException("Announce belote when playing the trump king or queen");}
 private List<Card> originalHandFor(int seat){
  List<Card> cards=new ArrayList<>(hands.get(seat));
  for(List<Play> trick:tricks)for(Play p:trick)if(p.seat()==seat)cards.add(p.card());
  for(Play p:current)if(p.seat()==seat)cards.add(p.card());
  return cards;
 }
 private void finishDeal(){
  int[] raw=trickPoints(tricks,contract);int[] declarationPoints=resolveDeclarationPoints();raw[0]+=declarationPoints[0];raw[1]+=declarationPoints[1];
  BelotScoring.Settlement settlement=BelotScoring.settle(raw,contractTeam,contra,pending,contract);
  int[] added=settlement.awarded();
  pending=settlement.hanging();
  score[0]+=added[0];score[1]+=added[1];
  boolean capot=tricks.stream().allMatch(t->team(t.get(winningIndex(t,contract)).seat())==team(tricks.get(0).get(winningIndex(tricks.get(0),contract)).seat()));
  history.add("Deal "+dealNumber+" "+contract+" raw="+Arrays.toString(raw)+" score="+Arrays.toString(score)+" pending="+pending+" made="+(settlement.winningTeam()==contractTeam));
  if(capot)capotLock=1;else if(capotLock>0)capotLock=0;
  if(!capot&&capotLock==0&&Math.max(score[0],score[1])>=151&&score[0]!=score[1])phase=Phase.FINISHED;
  else phase=Phase.DEAL_DONE;
  version++;
 }
 public synchronized void nextDeal(){if(phase!=Phase.DEAL_DONE)throw new IllegalStateException("Not ready");dealer=(dealer+1)%4;newDeal();}
 private void require(Phase expected,int seat){if(phase!=expected||turn!=seat)throw new IllegalArgumentException("Wrong phase or turn");}
}
