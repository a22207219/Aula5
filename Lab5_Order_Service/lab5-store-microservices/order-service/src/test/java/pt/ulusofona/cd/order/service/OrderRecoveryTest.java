package pt.ulusofona.cd.order.service;

import org.junit.jupiter.api.Test;
import pt.ulusofona.cd.order.model.Order;
import pt.ulusofona.cd.order.repository.OrderRepository;
import java.util.List;
import static org.mockito.Mockito.*;

class OrderRecoveryTest {
 @Test void schedulerReplaysInterruptedTransitions() {
  var repo=mock(OrderRepository.class);var service=mock(OrderService.class);
  var confirm=new Order(java.util.UUID.randomUUID(),2);confirm.setStatus(Order.Status.CONFIRMING);
  var cancel=new Order(java.util.UUID.randomUUID(),1);cancel.setStatus(Order.Status.CANCELLING);
  // Zero minimum age allows deterministic tests without timeouts or sleeps.
  when(repo.findTop100ByStatusOrderByUpdatedAtAsc(Order.Status.CONFIRMING)).thenReturn(List.of(confirm));
  when(repo.findTop100ByStatusOrderByUpdatedAtAsc(Order.Status.CANCELLING)).thenReturn(List.of(cancel));
  new OrderRecovery(repo,service,0).recover();
  verify(service).confirm(confirm.getId());verify(service).cancel(cancel.getId());
 }
 @Test void recoveryContinuesWhenOneRemoteCallFails() {
  var repo=mock(OrderRepository.class);var service=mock(OrderService.class);
  var first=new Order(java.util.UUID.randomUUID(),1);first.setStatus(Order.Status.CONFIRMING);
  var second=new Order(java.util.UUID.randomUUID(),1);second.setStatus(Order.Status.CONFIRMING);
  when(repo.findTop100ByStatusOrderByUpdatedAtAsc(Order.Status.CONFIRMING)).thenReturn(List.of(first,second));
  when(service.confirm(first.getId())).thenThrow(new IllegalStateException("remote down"));
  new OrderRecovery(repo,service,0).recover();
  verify(service).confirm(second.getId());
 }
}
