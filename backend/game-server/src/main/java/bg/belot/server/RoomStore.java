package bg.belot.server;

import java.io.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomStore {
 private final JdbcTemplate jdbc;
 public RoomStore(ObjectProvider<JdbcTemplate> provider){
  this.jdbc=provider.getIfAvailable();
  if(jdbc!=null)jdbc.execute("CREATE TABLE IF NOT EXISTS belot_rooms (code VARCHAR(12) PRIMARY KEY, snapshot BYTEA NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
 }
 public boolean enabled(){return jdbc!=null;}
 public synchronized void save(String code,Serializable room){
  if(jdbc==null)return;
  try{
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(room);}
   jdbc.update("INSERT INTO belot_rooms(code,snapshot,updated_at) VALUES (?, ?, NOW()) ON CONFLICT (code) DO UPDATE SET snapshot=EXCLUDED.snapshot, updated_at=NOW()",code,bytes.toByteArray());
  }catch(IOException e){throw new IllegalStateException("Could not serialize room",e);}
 }
 public synchronized Map<String,Object> load(){
  if(jdbc==null)return Map.of();
  Map<String,Object> rooms=new HashMap<>();
  jdbc.query("SELECT code,snapshot FROM belot_rooms",rs->{
   try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(rs.getBytes("snapshot")))){
    rooms.put(rs.getString("code"),in.readObject());
   }catch(IOException|ClassNotFoundException e){throw new IllegalStateException("Could not restore room "+rs.getString("code"),e);}
  });
  return rooms;
 }
}
