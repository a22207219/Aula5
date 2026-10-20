package pt.ulusofona.cd.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import pt.ulusofona.cd.order.client.ProductClient;
import pt.ulusofona.cd.order.client.SupplierClient;
import pt.ulusofona.cd.order.model.Order;
import pt.ulusofona.cd.order.model.dto.OrderResponse;
import pt.ulusofona.cd.order.repository.OrderRepository;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
 private final OrderRepository repo=mock(OrderRepository.class);
 private final ProductClient products=mock(ProductClient.class);
 private final SupplierClient suppliers=mock(SupplierClient.class);
 private final OrderTransitions transitions=mock(OrderTransitions.class);
 private final OrderService service=new OrderService(repo,products,suppliers,transitions);
 private OrderResponse response(UUID id,Order.Status status){return new OrderResponse(id,UUID.randomUUID(),2,status,null,null,0);}
 @Test void repeatedConfirmDoesNotDeductTwice(){UUID id=UUID.randomUUID();when(transitions.beginConfirm(id)).thenReturn(response(id,Order.Status.CONFIRMED));assertEquals(Order.Status.CONFIRMED,service.confirm(id).status());verifyNoInteractions(products);}
 @Test void lostConfirmResponseCanRetryWithSameOperationId(){UUID id=UUID.randomUUID();OrderResponse inProgress=response(id,Order.Status.CONFIRMING);when(transitions.beginConfirm(id)).thenReturn(inProgress);when(transitions.finish(id,Order.Status.CONFIRMING,Order.Status.CONFIRMED)).thenReturn(response(id,Order.Status.CONFIRMED));doThrow(new ResourceAccessException("response lost")).doNothing().when(products).decrement(eq(inProgress.productId()),any());assertThrows(ResponseStatusException.class,()->service.confirm(id));assertEquals(Order.Status.CONFIRMED,service.confirm(id).status());var captor=org.mockito.ArgumentCaptor.forClass(ProductClient.StockRequest.class);verify(products,times(2)).decrement(eq(inProgress.productId()),captor.capture());assertEquals(captor.getAllValues().get(0).operationId(),captor.getAllValues().get(1).operationId());}
 @Test void repeatedCancelDoesNotRestoreTwice(){UUID id=UUID.randomUUID();when(transitions.beginCancel(id)).thenReturn(response(id,Order.Status.CANCELLED));assertEquals(Order.Status.CANCELLED,service.cancel(id).status());verifyNoInteractions(products);}
 @Test void pendingCancelNeverChangesStock(){UUID id=UUID.randomUUID();when(transitions.beginCancel(id)).thenReturn(response(id,Order.Status.CANCELLING));when(transitions.restoreRequired(id)).thenReturn(false);when(transitions.finish(id,Order.Status.CANCELLING,Order.Status.CANCELLED)).thenReturn(response(id,Order.Status.CANCELLED));assertEquals(Order.Status.CANCELLED,service.cancel(id).status());verifyNoInteractions(products);}
}
