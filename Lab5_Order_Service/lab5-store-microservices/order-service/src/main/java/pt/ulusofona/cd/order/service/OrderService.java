package pt.ulusofona.cd.order.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.client.*;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;import pt.ulusofona.cd.order.client.*;import pt.ulusofona.cd.order.model.Order;import pt.ulusofona.cd.order.model.dto.*;import pt.ulusofona.cd.order.repository.OrderRepository;import java.nio.charset.StandardCharsets;import java.util.*;
@Service public class OrderService {
 private final OrderRepository repo;private final ProductClient products;private final SupplierClient suppliers;private final OrderTransitions transitions;
 public OrderService(OrderRepository repo,ProductClient products,SupplierClient suppliers,OrderTransitions transitions){this.repo=repo;this.products=products;this.suppliers=suppliers;this.transitions=transitions;}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
 private void validate(UUID productId){try{var product=products.get(productId);if(product==null||product.supplierId()==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Invalid Product response");var supplier=suppliers.get(product.supplierId());if(supplier==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Invalid Supplier response");if(!supplier.active())throw bad("Supplier inactive");}catch(HttpClientErrorException.NotFound ex){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Product or supplier not found",ex);}catch(ResourceAccessException|HttpServerErrorException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Remote service unavailable",ex);}}
 @Transactional public OrderResponse create(OrderRequest request){validate(request.productId());return OrderResponse.from(repo.save(new Order(request.productId(),request.quantity())));}
 @Transactional(readOnly=true) public List<OrderResponse> all(){return repo.findAll().stream().map(OrderResponse::from).toList();}
 @Transactional(readOnly=true) public OrderResponse one(UUID id){return OrderResponse.from(repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found")));}
 private Order locked(UUID id){return repo.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
 @Transactional public OrderResponse update(UUID id,OrderRequest request){Order o=locked(id);if(o.getStatus()!=Order.Status.PENDING)throw bad("Only pending orders can be edited");validate(request.productId());o.change(request.productId(),request.quantity());return OrderResponse.from(o);}
 @Transactional public void delete(UUID id){Order o=locked(id);if(o.getStatus()!=Order.Status.PENDING&&o.getStatus()!=Order.Status.CANCELLED)throw bad("Cancel the order before deletion");repo.delete(o);}
 private UUID key(UUID id,String action){return UUID.nameUUIDFromBytes((id+":"+action).getBytes(StandardCharsets.UTF_8));}
 public OrderResponse confirm(UUID id){var o=transitions.beginConfirm(id);if(o.status()==Order.Status.CONFIRMED)return o;
  try{products.decrement(o.productId(),new ProductClient.StockRequest(o.quantity(),key(id,"confirm")));}
  catch(HttpClientErrorException.Conflict ex){transitions.finish(id,Order.Status.CONFIRMING,Order.Status.PENDING);throw bad("Insufficient stock");}
  catch(RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Outcome unknown; repeat confirm with same operation ID (safe)",ex);}
  return transitions.finish(id,Order.Status.CONFIRMING,Order.Status.CONFIRMED);
 }
 public OrderResponse cancel(UUID id){var o=transitions.beginCancel(id);if(o.status()==Order.Status.CANCELLED)return o;
  // A previously confirmed order must restore stock; pending orders never changed stock.
  // The starting status is persisted as a stable property before entering CANCELLING.
  if(o.status()==Order.Status.CANCELLING&&shouldRestore(id)){
   try{products.increment(o.productId(),new ProductClient.StockRequest(o.quantity(),key(id,"cancel")));}
   catch(RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Outcome unknown; repeat cancel safely",ex);}
  }
  return transitions.finish(id,Order.Status.CANCELLING,Order.Status.CANCELLED);
 }
 private boolean shouldRestore(UUID id){return transitions.restoreRequired(id);}
}
