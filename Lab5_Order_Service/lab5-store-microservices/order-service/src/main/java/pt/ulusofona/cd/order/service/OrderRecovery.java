package pt.ulusofona.cd.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulusofona.cd.order.model.Order;
import pt.ulusofona.cd.order.repository.OrderRepository;
import java.time.Duration;
import java.time.Instant;

/** Replays interrupted transitions with the SAME deterministic stock operation ID. */
@Component
public class OrderRecovery {
 private static final Logger log=LoggerFactory.getLogger(OrderRecovery.class);
 private final OrderRepository orders;
 private final OrderService service;
 private final Duration delay;
 public OrderRecovery(OrderRepository orders, OrderService service,
    @Value("${orders.recovery-min-age-seconds:30}") long minAgeSeconds) {
  this.orders=orders;this.service=service;this.delay=Duration.ofSeconds(minAgeSeconds);
 }
 @Scheduled(fixedDelayString="${orders.recovery-interval-ms:15000}", initialDelayString="${orders.recovery-initial-delay-ms:15000}")
 public void recover() {
  replay(Order.Status.CONFIRMING);
  replay(Order.Status.CANCELLING);
 }
 private void replay(Order.Status status) {
  for (Order order:orders.findTop100ByStatusOrderByUpdatedAtAsc(status)) {
   if(order.getUpdatedAt().isAfter(Instant.now().minus(delay))) continue;
   try {
    if(status==Order.Status.CONFIRMING) service.confirm(order.getId());
    else service.cancel(order.getId());
   } catch (Exception ex) {
    log.warn("Recovery of order {} in state {} failed; will retry: {}",order.getId(),status,ex.toString());
   }
  }
 }
}
