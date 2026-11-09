package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
/** Single-room state machine. All mutating methods synchronized; caller persists snapshots. */
public final class MatchState {
 public enum Phase { BIDDING, PLAYING, DEAL_DONE, FINISHED }
 public final List<List<Card>> hands=new ArrayList<>();public final List<List<Play>> tricks=new ArrayList<>();public final List<Play> current=new ArrayList<>();public final int[] score={0,0};
 public int dealer=0,turn=1,contractTeam=-1,passCount=0,contra=1,pending=0,dealNumber=0,capotLock=0;public long version=0;public Phase phase;public Contract contract;public Contract highest;public final List<String> history=new ArrayList<>();public final List<Declaration> declarations=new ArrayList<>();public final Set<Integer> declarationDecisions=new HashSet<>();public final Set<Integer> beloteClaims=new HashSet<>();private List<Card> stock;
 public MatchState(){newDeal();}
 public synchronized void newDeal(){stock=shuffle();hands.clear();for(int i=0;i<4;i++)hands.add(new ArrayList<>());int at=0;for(int packet:List.of(3,2)){for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<packet;j++)hands.get(seat).add(stock.get(at++));}}stock=new ArrayList<>(stock.subList(at,32));tricks.clear();current.clear();declarations.clear();declarationDecisions.clear();beloteClaims.clear();highest=null;contract=null;contractTeam=-1;passCount=0;contra=1;turn=(dealer+1)%4;phase=Phase.BIDDING;dealNumber++;version++;}
 public synchronized void bid(int seat,String action){require(Phase.BIDDING,seat);if(action.equals("PASS")){passCount++;if(highest==null&&passCount==4){dealer=(dealer+1)%4;newDeal();return;}if(highest!=null&&passCount==3){finalizeBid();return;}}else if(action.equals("CONTRA")){if(highest==null||contractTeam==team(seat)||contra!=1)throw new IllegalArgumentException("Contra not allowed");contra=2;passCount=0;}else if(action.equals("RECONTRA")){if(highest==null||contractTeam!=team(seat)||contra!=2)throw new IllegalArgumentException("Recontra not allowed");contra=4;passCount=0;}else{Contract c=Contract.valueOf(action);if(highest!=null&&priority(c)<=priority(highest))throw new IllegalArgumentException("Bid must be higher");if(contra!=1)throw new IllegalArgumentException("Contract locked by contra");highest=c;contractTeam=team(seat);passCount=0;}turn=(turn+1)%4;version++;}
 private void finalizeBid(){contract=highest;int k=0;for(int offset=1;offset<=4;offset++){int seat=(dealer+offset)%4;for(int j=0;j<3;j++)hands.get(seat).add(stock.get(k++));}stock=List.of();phase=Phase.PLAYING;turn=(dealer+1)%4;version++;}
 public synchronized List<Declaration> availableDeclarations(int seat){
  if(seat<0||seat>=4||phase!=Phase.PLAYING||!tricks.isEmpty())return List.of();
  return detect(hands.get(seat),seat,contract);
 }
 public synchronized void declare(int seat,List<Integer> selected){
  if(phase!=Phase.PLAYING||!tricks.isEmpty())throw new IllegalArgumentException("Declarations only during first trick");
  if(seat<0||seat>=4||declarationDecisions.contains(seat))throw new IllegalArgumentException("Already declared");
  List<Declaration> options=availableDeclarations(seat);
  if(selected==null)throw new IllegalArgumentException("Missing selection");
  Set<Integer> unique=new HashSet<>(selected);
  if(unique.size()!=selected.size())throw new IllegalArgumentException("Duplicate declaration");
  for(int index:selected){
   if(index<0||index>=options.size())throw new IllegalArgumentException("Invalid declaration");
   declarations.add(options.get(index));
  }
  declarationDecisions.add(seat);version++;
 }
 public synchronized List<Card> legalFor(int seat){return phase==Phase.PLAYING&&seat==turn?legal(hands.get(seat),current,contract,seat):List.of();}
 public synchronized void play(int seat,Card card){require(Phase.PLAYING,seat);if(!legalFor(seat).contains(card))throw new IllegalArgumentException("Illegal card");if(tricks.isEmpty()&&!declarationDecisions.contains(seat))throw new IllegalArgumentException("Choose declarations before playing your first card");hands.get(seat).remove(card);current.add(new Play(seat,card));if(current.size()==4){List<Play>complete=List.copyOf(current);tricks.add(complete);turn=complete.get(winningIndex(complete,contract)).seat();current.clear();if(tricks.size()==8){finishDeal();return;}}else turn=(turn+1)%4;version++;}
 private int[] resolveDeclarationPoints(){
  int[] result={0,0};
  if(declarations.isEmpty())return result;
  // Strongest declaration wins all sequence/four-of-a-kind premiums for its team.
  Declaration best=declarations.stream().max(Comparator.comparingInt(Declaration::points)).orElseThrow();
  int winningTeam=team(best.seat());
  for(Declaration d:declarations)if(team(d.seat())==winningTeam)result[winningTeam]+=d.points();
  return result;
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
   pending+=(total*contra+5)/10;
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
