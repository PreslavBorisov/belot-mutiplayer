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
 static class Room {final MatchState game=new MatchState();final String[] seats=new String[4];boolean started=false;}
 public record Join(String player,int seat){}
 public record Command(String player,String action,String card){}
 public static void main(String[] args){SpringApplication.run(BelotServer.class,args);}
 @GetMapping("/health")public Map<String,String> health(){return Map.of("status","UP");}
 @PostMapping("/rooms")public Map<String,String> create(){String id=UUID.randomUUID().toString().substring(0,6).toUpperCase();rooms.put(id,new Room());return Map.of("code",id);}
 @PostMapping("/rooms/{code}/join")public Map<String,Object> join(@PathVariable String code,@RequestBody Join j){Room r=room(code);synchronized(r){if(j.player()==null||j.player().isBlank()||j.seat()<0||j.seat()>3)throw new IllegalArgumentException("Invalid seat/player");for(int i=0;i<4;i++)if(j.player().equals(r.seats[i])&&i!=j.seat())throw new IllegalArgumentException("Already seated");if(r.seats[j.seat()]!=null&&!r.seats[j.seat()].equals(j.player()))throw new IllegalArgumentException("Seat occupied");r.seats[j.seat()]=j.player();return view(r,j.player());}}
 @PostMapping("/rooms/{code}/start")public Map<String,Object> start(@PathVariable String code,@RequestBody Command c){Room r=room(code);synchronized(r){if(Arrays.stream(r.seats).anyMatch(Objects::isNull))throw new IllegalArgumentException("Four seats required");if(!Objects.equals(r.seats[0],c.player()))throw new IllegalArgumentException("Host only");r.started=true;return view(r,c.player());}}
 @GetMapping("/rooms/{code}")public Map<String,Object> snapshot(@PathVariable String code,@RequestParam String player){Room r=room(code);synchronized(r){return view(r,player);}}
 @PostMapping("/rooms/{code}/command")public Map<String,Object> command(@PathVariable String code,@RequestBody Command c){Room r=room(code);synchronized(r){int seat=Arrays.asList(r.seats).indexOf(c.player());if(seat<0||!r.started)throw new IllegalArgumentException("Not authorized or not started");if(c.action().equals("BID"))r.game.bid(seat,c.card());else if(c.action().equals("PLAY")){String[] parts=c.card().split("_");r.game.play(seat,new Card(Suit.valueOf(parts[0]),Rank.valueOf(parts[1])));}else if(c.action().equals("NEXT")){r.game.nextDeal();}else throw new IllegalArgumentException("Unknown action");return view(r,c.player());}}
 private Room room(String code){Room r=rooms.get(code);if(r==null)throw new IllegalArgumentException("Room not found");return r;}
 private Map<String,Object> view(Room r,String player){int seat=Arrays.asList(r.seats).indexOf(player);if(seat<0)throw new IllegalArgumentException("Not a member");MatchState g=r.game;Map<String,Object> m=new LinkedHashMap<>();m.put("seats",r.seats.clone());m.put("seat",seat);m.put("started",r.started);m.put("phase",g.phase);m.put("turn",g.turn);m.put("dealer",g.dealer);m.put("contract",g.contract);m.put("highest",g.highest);m.put("contra",g.contra);m.put("score",g.score.clone());m.put("pending",g.pending);m.put("version",g.version);m.put("hand",r.started?List.copyOf(g.hands.get(seat)):List.of());m.put("handCounts",g.hands.stream().map(List::size).toList());m.put("legal",g.legalFor(seat));m.put("trick",List.copyOf(g.current));m.put("tricksCompleted",g.tricks.size());m.put("history",List.copyOf(g.history));return m;}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})public ResponseEntity<Map<String,String>> error(RuntimeException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}
}
