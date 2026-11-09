package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.*;

public final class DeclarationRegression {
 public static void main(String[] args){
  List<Card> hand=List.of(
   new Card(Suit.HEARTS,Rank.SEVEN),
   new Card(Suit.HEARTS,Rank.EIGHT),
   new Card(Suit.HEARTS,Rank.NINE),
   new Card(Suit.HEARTS,Rank.TEN),
   new Card(Suit.CLUBS,Rank.JACK));
  List<Declaration> found=detect(hand,0,Contract.HEARTS);
  if(found.size()!=1||found.get(0).points()!=50)throw new AssertionError("Four-card sequence must score 50");
  if(!detect(hand,0,Contract.NO_TRUMP).isEmpty())throw new AssertionError("No-trump declarations are disabled");
  if(!hasBelote(List.of(new Card(Suit.HEARTS,Rank.KING),new Card(Suit.HEARTS,Rank.QUEEN)),Contract.HEARTS))throw new AssertionError("Trump belote missing");
  if(hasBelote(List.of(new Card(Suit.HEARTS,Rank.KING),new Card(Suit.HEARTS,Rank.QUEEN)),Contract.CLUBS))throw new AssertionError("Wrong suit belote");
  System.out.println("DeclarationRegression passed");
 }
}
