package bg.belot.server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class RoomPageController {
 @GetMapping("/room/{code}")
 public String roomPage(@PathVariable String code) {
  return "forward:/index.html";
 }
}
