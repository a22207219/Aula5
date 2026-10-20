package pt.ulusofona.cd.order.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;import pt.ulusofona.cd.order.repository.OrderRepository;import pt.ulusofona.cd.order.model.Order;import pt.ulusofona.cd.order.model.dto.*;import java.util.UUID;
@Service public class OrderTransitions {
 private final OrderRepository repo; public OrderTransitions(OrderRepository repo){this.repo=repo;}
 private Order locked(UUID id){return repo.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
 @Transactional public OrderResponse beginConfirm(UUID id){Order o=locked(id);if(o.getStatus()==Order.Status.PENDING)o.setStatus(Order.Status.CONFIRMING);else if(o.getStatus()!=Order.Status.CONFIRMING&&o.getStatus()!=Order.Status.CONFIRMED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot confirm in current state");return OrderResponse.from(o);}
 @Transactional public OrderResponse beginCancel(UUID id){Order o=locked(id);if(o.getStatus()==Order.Status.PENDING||o.getStatus()==Order.Status.CONFIRMED){o.setRestoreRequired(o.getStatus()==Order.Status.CONFIRMED);o.setStatus(Order.Status.CANCELLING);}else if(o.getStatus()!=Order.Status.CANCELLING&&o.getStatus()!=Order.Status.CANCELLED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot cancel in current state");return OrderResponse.from(o);}
 // locked() issues SELECT ... FOR UPDATE; keep this transaction read-write because
 // PostgreSQL does not allow row-locking queries inside a read-only transaction.
 @Transactional public boolean restoreRequired(UUID id){return locked(id).isRestoreRequired();}
 @Transactional public OrderResponse finish(UUID id,Order.Status expected,Order.Status next){Order o=locked(id);if(o.getStatus()==expected)o.setStatus(next);else if(o.getStatus()!=next)throw new ResponseStatusException(HttpStatus.CONFLICT,"Concurrent transition");return OrderResponse.from(o);}
}
