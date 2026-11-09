package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;
/** Standalone deterministic regression checks; run with java -ea. */
public class RulesRegression {
 private static Card c(Suit s,Rank r){return new Card(s,r);}
 private static void eq(Object expected,Object actual,String label){if(!Objects.equals(expected,actual))throw new AssertionError(label+": expected "+expected+" got "+actual);}
 public static void main(String[] args){
  eq(32,deck().size(),"deck size");
  eq(32,new HashSet<>(deck()).size(),"unique deck");
  List<Play> trick=List.of(new Play(0,c(Suit.HEARTS,Rank.ACE)));
  List<Card> hand=List.of(c(Suit.CLUBS,Rank.EIGHT),c(Suit.SPADES,Rank.ACE));
  eq(List.of(c(Suit.CLUBS,Rank.EIGHT)),legal(hand,trick,Contract.CLUBS,1),"must trump when opponent winning and void");
  List<Play> over=List.of(new Play(0,c(Suit.HEARTS,Rank.ACE)),new Play(1,c(Suit.CLUBS,Rank.JACK)));
  eq(List.of(c(Suit.CLUBS,Rank.EIGHT)),legal(hand,over,Contract.CLUBS,2),"must undertrump if unable to overtrump");
  eq(List.of(c(Suit.HEARTS,Rank.SEVEN)),legal(List.of(c(Suit.HEARTS,Rank.SEVEN),c(Suit.CLUBS,Rank.JACK)),trick,Contract.CLUBS,1),"must follow suit");
  List<Card> all=List.of(c(Suit.HEARTS,Rank.JACK),c(Suit.HEARTS,Rank.NINE));
  eq(List.of(c(Suit.HEARTS,Rank.JACK)),legal(all,List.of(new Play(0,c(Suit.HEARTS,Rank.NINE))),Contract.ALL_TRUMP,1),"must raise all trump");
  eq(20,points(c(Suit.CLUBS,Rank.JACK),Contract.CLUBS),"trump jack points");
  eq(2,points(c(Suit.CLUBS,Rank.JACK),Contract.NO_TRUMP),"non trump jack points");
  System.out.println("PASS: deterministic rules regression checks");
 }
}
