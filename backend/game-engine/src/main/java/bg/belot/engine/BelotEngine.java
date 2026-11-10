package bg.belot.engine;

import java.security.SecureRandom;
import java.util.*;

/** Dependency-free authoritative rules core. Seats run counterclockwise: 0 -> 1 -> 2 -> 3. */
public final class BelotEngine {
  public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
  public enum Rank { SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE }
  public enum Contract { CLUBS, DIAMONDS, HEARTS, SPADES, NO_TRUMP, ALL_TRUMP }
  public record Card(Suit suit, Rank rank) implements java.io.Serializable { public String id(){return suit.name()+"_"+rank.name();} }
  public record Play(int seat, Card card) implements java.io.Serializable {}
  public record Declaration(int seat, String kind, int points, List<Card> cards) implements java.io.Serializable {}
  public record DealScore(int[] raw, int[] rounded, int pending, boolean capot, int winnerTeam) {}
  private static final List<Rank> NORMAL = List.of(Rank.SEVEN,Rank.EIGHT,Rank.NINE,Rank.JACK,Rank.QUEEN,Rank.KING,Rank.TEN,Rank.ACE);
  private static final List<Rank> TRUMP = List.of(Rank.SEVEN,Rank.EIGHT,Rank.QUEEN,Rank.KING,Rank.TEN,Rank.ACE,Rank.NINE,Rank.JACK);
  private static final SecureRandom RNG = new SecureRandom();
  public static List<Card> deck(){List<Card> d=new ArrayList<>();for(Suit s:Suit.values())for(Rank r:Rank.values())d.add(new Card(s,r));return d;}
  public static List<Card> shuffle(){List<Card>d=deck();for(int i=d.size()-1;i>0;i--){int j=RNG.nextInt(i+1);Collections.swap(d,i,j);}return d;}
  public static int team(int seat){return seat%2;}
  public static int priority(Contract c){return c.ordinal();}
  public static boolean trump(Card c,Contract contract){return contract==Contract.ALL_TRUMP || (contract.ordinal()<4 && c.suit().ordinal()==contract.ordinal());}
  public static int power(Card c,Contract contract){return (trump(c,contract)?TRUMP:NORMAL).indexOf(c.rank());}
  public static int points(Card c,Contract contract){return switch(c.rank()){
    case SEVEN,EIGHT -> 0;case NINE -> trump(c,contract)?14:0;case JACK -> trump(c,contract)?20:2;
    case QUEEN -> 3;case KING -> 4;case TEN -> 10;case ACE -> 11;};}
  public static int winningIndex(List<Play> trick,Contract contract){if(trick.isEmpty())throw new IllegalArgumentException("Empty trick");int best=0;for(int i=1;i<trick.size();i++){Card a=trick.get(best).card(),b=trick.get(i).card();boolean bt=trump(b,contract),at=trump(a,contract);if(contract==Contract.ALL_TRUMP || contract==Contract.NO_TRUMP){if(b.suit()==a.suit()&&power(b,contract)>power(a,contract))best=i;}else if((bt&&!at)||(bt==at&&b.suit()==a.suit()&&power(b,contract)>power(a,contract)))best=i;}return best;}
  public static List<Card> legal(List<Card> hand,List<Play> trick,Contract contract,int seat){if(trick.isEmpty())return List.copyOf(hand);Suit led=trick.get(0).card().suit();List<Card>follow=hand.stream().filter(c->c.suit()==led).toList();Card winning=trick.get(winningIndex(trick,contract)).card();if(contract==Contract.NO_TRUMP)return follow.isEmpty()?List.copyOf(hand):follow;
    if(contract==Contract.ALL_TRUMP){if(follow.isEmpty())return List.copyOf(hand);int max=trick.stream().filter(p->p.card().suit()==led).mapToInt(p->power(p.card(),contract)).max().orElse(-1);List<Card>higher=follow.stream().filter(c->power(c,contract)>max).toList();return higher.isEmpty()?follow:higher;}
    if(!follow.isEmpty()){if(led.ordinal()==contract.ordinal()){int max=trick.stream().filter(p->trump(p.card(),contract)).mapToInt(p->power(p.card(),contract)).max().orElse(-1);List<Card>higher=follow.stream().filter(c->power(c,contract)>max).toList();return higher.isEmpty()?follow:higher;}return follow;}
    if(team(trick.get(winningIndex(trick,contract)).seat())==team(seat))return List.copyOf(hand);
    List<Card>trumps=hand.stream().filter(c->trump(c,contract)).toList();if(trumps.isEmpty())return List.copyOf(hand);
    if(trump(winning,contract)){int max=trick.stream().filter(p->trump(p.card(),contract)).mapToInt(p->power(p.card(),contract)).max().orElse(-1);List<Card>higher=trumps.stream().filter(c->power(c,contract)>max).toList();return higher.isEmpty()?trumps:higher;}return trumps;
  }
  public static List<Declaration> detect(List<Card> hand,int seat,Contract contract){if(contract==Contract.NO_TRUMP)return List.of();List<Declaration>out=new ArrayList<>();for(Suit s:Suit.values()){List<Card>sorted=hand.stream().filter(c->c.suit()==s).sorted(Comparator.comparingInt(c->c.rank().ordinal())).toList();for(int i=0;i<sorted.size();){int j=i+1;while(j<sorted.size()&&sorted.get(j).rank().ordinal()==sorted.get(j-1).rank().ordinal()+1)j++;if(j-i>=3){int len=j-i;out.add(new Declaration(seat,"SEQUENCE",len==3?20:len==4?50:100,List.copyOf(sorted.subList(i,j))));}i=j;}}
    for(Rank r:Rank.values()){if(r.ordinal()<Rank.NINE.ordinal())continue;List<Card>cards=hand.stream().filter(c->c.rank()==r).toList();if(cards.size()==4)out.add(new Declaration(seat,"FOUR",r==Rank.JACK?200:r==Rank.NINE?150:100,cards));}Set<Card> fourCards=new HashSet<>();for(Declaration d:out)if(d.kind().equals("FOUR"))fourCards.addAll(d.cards());return out.stream().filter(d->!d.kind().equals("SEQUENCE")||Collections.disjoint(d.cards(),fourCards)).toList();}
  public static boolean hasBelote(List<Card> hand,Contract contract){if(contract==Contract.NO_TRUMP)return false;for(Suit s:Suit.values()){if(contract!=Contract.ALL_TRUMP&&s.ordinal()!=contract.ordinal())continue;if(hand.contains(new Card(s,Rank.KING))&&hand.contains(new Card(s,Rank.QUEEN)))return true;}return false;}
  /** Scores without announcements. Adds last ten and capot; raw totals are 162/130/258 before capot. */
  public static int[] trickPoints(List<List<Play>> tricks,Contract contract){int[] p=new int[2];for(int i=0;i<tricks.size();i++){List<Play>t=tricks.get(i);if(t.size()!=4)throw new IllegalArgumentException("Incomplete trick");int winner=team(t.get(winningIndex(t,contract)).seat());for(Play play:t)p[winner]+=points(play.card(),contract);if(i==7)p[winner]+=10;}if(tricks.size()==8){int first=team(tricks.get(0).get(winningIndex(tricks.get(0),contract)).seat());boolean capot=tricks.stream().allMatch(t->team(t.get(winningIndex(t,contract)).seat())==first);if(capot)p[first]+=90;}if(contract==Contract.NO_TRUMP){for(int i=0;i<2;i++){final int target=i;boolean capot=tricks.size()==8&&tricks.stream().allMatch(t->team(t.get(winningIndex(t,contract)).seat())==target);p[i]=p[i]*2-(capot?90:0);}}return p;}
  private BelotEngine(){}
}
