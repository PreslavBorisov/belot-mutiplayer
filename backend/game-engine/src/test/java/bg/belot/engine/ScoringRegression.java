package bg.belot.engine;
import java.util.*;
import static bg.belot.engine.BelotEngine.Contract;
public final class ScoringRegression {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  var normal=BelotScoring.settle(new int[]{106,106},0,1,0,Contract.HEARTS);
  check(Arrays.equals(normal.awarded(),new int[]{0,10})&&normal.hanging()==11,"106:106 -> 10 defender, 11 hanging");
  var all=BelotScoring.settle(new int[]{154,154},0,1,0,Contract.ALL_TRUMP);
  check(Arrays.equals(all.awarded(),new int[]{0,15})&&all.hanging()==16,"154:154 -> 15 defender, 16 hanging");
  var contra=BelotScoring.settle(new int[]{81,81},0,2,0,Contract.HEARTS);
  check(Arrays.equals(contra.awarded(),new int[]{0,0})&&contra.hanging()==32,"Contra tie: all points hanging");
  var recontra=BelotScoring.settle(new int[]{81,81},0,4,0,Contract.HEARTS);
  check(Arrays.equals(recontra.awarded(),new int[]{0,0})&&recontra.hanging()==65,"Recontra tie: all points hanging");
  var resolved=BelotScoring.settle(new int[]{100,62},0,1,32,Contract.HEARTS);
  check(Arrays.equals(resolved.awarded(),new int[]{42,6})&&resolved.hanging()==0,"Pending awarded to next winner");
  var down=BelotScoring.settle(new int[]{70,92},0,1,0,Contract.HEARTS);
  check(Arrays.equals(down.awarded(),new int[]{0,16}),"Failed contract awards all to defenders");
  var doubled=BelotScoring.settle(new int[]{100,62},0,2,0,Contract.HEARTS);
  check(Arrays.equals(doubled.awarded(),new int[]{32,0}),"Contra winner gets double total");
  check(Arrays.equals(BelotScoring.roundBoth(new int[]{124,134},Contract.ALL_TRUMP),new int[]{13,13}),"All trump lower side rounds up");
  System.out.println("ScoringRegression passed");
 }
}
