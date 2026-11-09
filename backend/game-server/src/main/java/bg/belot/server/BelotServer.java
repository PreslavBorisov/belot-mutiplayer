package bg.belot.server;
import bg.belot.engine.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.util.concurrent.*;
import static bg.belot.engine.BelotEngine.*;
@SpringBootApplication
@RestController
@RequestMapping("/api")
public class BelotServer {
 private final Map<String,Room> rooms=new ConcurrentHashMap<>();
 static class Room {final MatchState game=new MatchState();final String[] seats=new String[4];final String[] tokens=new String[4];boolean started=false;}
 public record Join(String player,int seat){}
 public record Command(String token,String action,String card){}
 public record DeclareCommand(String token,List<Integer> selected){}
 public static void main(String[] args){SpringApplication.run(BelotServer.class,args);}
 @GetMapping("/health")public Map<String,String> health(){return Map.of("status","UP");}
 @PostMapping("/rooms")public Map<String,String> create(){String id=UUID.randomUUID().toString().substring(0,6).toUpperCase();rooms.put(id,new Room());return Map.of("code",id);}

 @PostMapping("/rooms/{code}/join")public Map<String,Object> join(@PathVariable String code,@RequestBody Join j){
  Room r=room(code);synchronized(r){
   if(r.started)throw new IllegalArgumentException("Game already started");
   if(j.player()==null||j.player().isBlank()||j.player().length()>40||j.seat()<0||j.seat()>3)throw new IllegalArgumentException("Invalid seat/player");
   if(Arrays.asList(r.seats).contains(j.player()))throw new IllegalArgumentException("Name already taken");
   if(r.seats[j.seat()]!=null)throw new IllegalArgumentException("Seat occupied");
   String token=UUID.randomUUID().toString()+UUID.randomUUID();
   r.seats[j.seat()]=j.player();r.tokens[j.seat()]=token;
   Map<String,Object> result=view(r,j.seat());result.put("token",token);return result;
  }
 }
 @PostMapping("/rooms/{code}/bots")public Map<String,Object> fillBots(@PathVariable String code,@RequestBody Command c){
  Room r=room(code);synchronized(r){
   int seat=authenticate(r,c.token());
   if(seat!=0||r.started)throw new IllegalArgumentException("Only host can add bots before start");
   for(int i=0;i<4;i++)if(r.seats[i]==null){r.seats[i]="Bot "+(i+1);r.tokens[i]=null;}
   return view(r,seat);
  }
 }
 private void advanceBots(Room r){
  int guard=0;
  while(r.started&&guard++<150){
   int seat=r.game.turn;
   if(seat<0||seat>=4||r.tokens[seat]!=null)break;
   if(r.game.phase==MatchState.Phase.BIDDING){
    r.game.bid(seat,BelotBot.bid(r.game,seat));
   }else if(r.game.phase==MatchState.Phase.PLAYING){
    List<Card> choices=r.game.legalFor(seat);
    if(choices.isEmpty())break;
    if(r.game.tricks.isEmpty()&&!r.game.declarationDecisions.contains(seat))r.game.declare(seat,BelotBot.declarations(r.game,seat));
    if(BelotEngine.hasBelote(r.game.hands.get(seat),r.game.contract)&&!r.game.beloteClaims.contains(seat)&&!r.game.belotePending.containsKey(seat))r.game.claimBelote(seat);
    r.game.play(seat,BelotBot.play(r.game,seat));
   }else break;
  }
 }
 @PostMapping("/rooms/{code}/start")public Map<String,Object> start(@PathVariable String code,@RequestBody Command c){
  Room r=room(code);synchronized(r){
   int seat=authenticate(r,c.token());
   if(Arrays.stream(r.seats).anyMatch(Objects::isNull))throw new IllegalArgumentException("Four seats required");
   if(seat!=0)throw new IllegalArgumentException("Host only");
   if(r.started)throw new IllegalArgumentException("Already started");
   r.started=true;advanceBots(r);return view(r,seat);
  }
 }
 @GetMapping("/rooms/{code}")public Map<String,Object> snapshot(@PathVariable String code,@RequestParam String token){
  Room r=room(code);synchronized(r){return view(r,authenticate(r,token));}
 }
 @PostMapping("/rooms/{code}/declare")public Map<String,Object> declare(@PathVariable String code,@RequestBody DeclareCommand c){
  Room r=room(code);synchronized(r){
   int seat=authenticate(r,c.token());
   if(!r.started)throw new IllegalArgumentException("Not started");
   r.game.declare(seat,c.selected());
   advanceBots(r);
   return view(r,seat);
  }
 }
 @PostMapping("/rooms/{code}/belote")public Map<String,Object> belote(@PathVariable String code,@RequestBody Command c){
  Room r=room(code);synchronized(r){
   int seat=authenticate(r,c.token());
   if(!r.started)throw new IllegalArgumentException("Not started");
   r.game.claimBelote(seat);
   return view(r,seat);
  }
 }
 @PostMapping("/rooms/{code}/command")public Map<String,Object> command(@PathVariable String code,@RequestBody Command c){
  Room r=room(code);synchronized(r){
   int seat=authenticate(r,c.token());
   if(!r.started)throw new IllegalArgumentException("Not started");
   if("BID".equals(c.action()))r.game.bid(seat,c.card());
   else if("PLAY".equals(c.action())){
    String[] parts=c.card().split("_");
    if(parts.length!=2)throw new IllegalArgumentException("Invalid card");
    r.game.play(seat,new Card(Suit.valueOf(parts[0]),Rank.valueOf(parts[1])));
   }else if("NEXT".equals(c.action())){if(seat!=0)throw new IllegalArgumentException("Host only");r.game.nextDeal();}
   else throw new IllegalArgumentException("Unknown action");
   advanceBots(r);return view(r,seat);
  }
 }
 private int authenticate(Room r,String token){
  if(token==null||token.isBlank())throw new IllegalArgumentException("Missing session token");
  int seat=Arrays.asList(r.tokens).indexOf(token);
  if(seat<0)throw new IllegalArgumentException("Invalid session token");
  return seat;
 }
 private Room room(String code){Room r=rooms.get(code);if(r==null)throw new IllegalArgumentException("Room not found");return r;}
 private Map<String,Object> view(Room r,int seat){
  MatchState g=r.game;Map<String,Object> m=new LinkedHashMap<>();
  m.put("seats",r.seats.clone());m.put("seat",seat);m.put("started",r.started);
  m.put("phase",g.phase);m.put("turn",g.turn);m.put("dealer",g.dealer);
  m.put("contract",g.contract);m.put("highest",g.highest);m.put("contra",g.contra);
  m.put("score",g.score.clone());m.put("pending",g.pending);m.put("version",g.version);
  m.put("hand",r.started?List.copyOf(g.hands.get(seat)):List.of());
  m.put("handCounts",g.hands.stream().map(List::size).toList());
  m.put("legal",g.legalFor(seat));m.put("declarations",g.availableDeclarations(seat));m.put("declarationDecided",g.declarationDecisions.contains(seat));m.put("canClaimBelote",g.phase==MatchState.Phase.PLAYING&&g.turn==seat&&!g.beloteClaims.contains(seat)&&!g.belotePending.containsKey(seat)&&BelotEngine.hasBelote(g.hands.get(seat),g.contract));m.put("trick",List.copyOf(g.current));
  m.put("tricksCompleted",g.tricks.size());m.put("history",List.copyOf(g.history));
  return m;
 }
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})public ResponseEntity<Map<String,String>> error(RuntimeException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}
}
