package bg.belot.engine;

/** Scores in game units (tens of card points). */
public final class BelotScoring {
 private BelotScoring(){}
 public record Settlement(int[] awarded,int hanging,int winningTeam,boolean tied){}
 public static int round(int raw){return (raw+5)/10;}
 /** Normal all-trump 258-point rounding: when both remainders are >=4, lower raw side rounds up. */
 public static int[] roundBoth(int[] raw, BelotEngine.Contract contract){
  int[] result={round(raw[0]),round(raw[1])};
  if(contract==BelotEngine.Contract.ALL_TRUMP && raw[0]!=raw[1]
      && raw[0]%10>=4 && raw[1]%10>=4){
   int lower=raw[0]<raw[1]?0:1;
   result[lower]=(raw[lower]+9)/10;
   result[1-lower]=raw[1-lower]/10;
  }
  return result;
 }
 public static Settlement settle(int[] raw,int bidder,int multiplier,int oldPending,BelotEngine.Contract contract){
  if(raw.length!=2||bidder<0||bidder>1||(multiplier!=1&&multiplier!=2&&multiplier!=4)||oldPending<0)
   throw new IllegalArgumentException("Invalid settlement");
  int defender=1-bidder,total=raw[0]+raw[1];
  int[] points={0,0};
  if(raw[bidder]==raw[defender]){
   if(multiplier>1)return new Settlement(points,oldPending+round(total*multiplier),-1,true);
   int[] rounded=roundBoth(raw,contract);
   // On equal scores, the bidder's part hangs, rounded upwards.
   points[defender]=raw[defender]/10;
   return new Settlement(points,oldPending+(raw[bidder]+9)/10,-1,true);
  }
  int winner=raw[bidder]>raw[defender]?bidder:defender;
  if(multiplier>1||winner==defender)points[winner]=round(total*multiplier);
  else {
   int[] rounded=roundBoth(raw,contract);
   points[0]=rounded[0];points[1]=rounded[1];
  }
  points[winner]+=oldPending;
  return new Settlement(points,0,winner,false);
 }
}
