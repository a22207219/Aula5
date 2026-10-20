package pt.ulusofona.cd.product.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/products/{id}/stock")
public class StockController {
 private final JdbcTemplate jdbc;
 public StockController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record StockRequest(int quantity, UUID operationId) {}
 public record StockResponse(int stock) {}
 @GetMapping public StockResponse stock(@PathVariable UUID id){return new StockResponse(current(id));}
 private int current(UUID id){var n=jdbc.query("SELECT stock FROM product WHERE id=?",rs->rs.next()?rs.getInt(1):null,id);if(n==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");return n;}
 @PostMapping("/decrement") @Transactional public StockResponse decrement(@PathVariable UUID id,@RequestBody StockRequest r){return apply(id,r,"DECREMENT");}
 @PostMapping("/increment") @Transactional public StockResponse increment(@PathVariable UUID id,@RequestBody StockRequest r){return apply(id,r,"INCREMENT");}
 private StockResponse apply(UUID id,StockRequest r,String direction){
  if(r==null||r.quantity()<=0||r.operationId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"quantity and operationId required");
  // A PostgreSQL advisory transaction lock serializes duplicate operation IDs even across concurrent requests.
  jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?::text,0))",rs->{rs.next();return 0;},r.operationId().toString());
  var previous=jdbc.query("SELECT product_id,quantity,direction,result_stock FROM stock_operation WHERE operation_id=?",rs->{if(!rs.next())return null;return new Object[]{rs.getObject(1,UUID.class),rs.getInt(2),rs.getString(3),rs.getInt(4)};},r.operationId());
  if(previous!=null){if(!id.equals(previous[0])||r.quantity()!=(Integer)previous[1]||!direction.equals(previous[2]))throw new ResponseStatusException(HttpStatus.CONFLICT,"Operation ID reused with different arguments");return new StockResponse((Integer)previous[3]);}
  int affected="DECREMENT".equals(direction)?jdbc.update("UPDATE product SET stock=stock-?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND stock>=?",r.quantity(),id,r.quantity()):jdbc.update("UPDATE product SET stock=stock+?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",r.quantity(),id);
  if(affected==0){current(id);throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock");}
  int result=current(id);
  jdbc.update("INSERT INTO stock_operation(operation_id,product_id,quantity,direction,result_stock) VALUES(?,?,?,?,?)",r.operationId(),id,r.quantity(),direction,result);
  return new StockResponse(result);
 }
}
