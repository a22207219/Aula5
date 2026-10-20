package pt.ulusofona.cd.order.client;
import org.springframework.web.service.annotation.*;import org.springframework.web.bind.annotation.*;import java.util.UUID;
@HttpExchange("/api/v1/products") public interface ProductClient {
 @GetExchange("/{id}") ProductInfo get(@PathVariable UUID id);
 @PostExchange("/{id}/stock/decrement") void decrement(@PathVariable UUID id,@RequestBody StockRequest request);
 @PostExchange("/{id}/stock/increment") void increment(@PathVariable UUID id,@RequestBody StockRequest request);
 record ProductInfo(UUID id,UUID supplierId){} record StockRequest(int quantity,UUID operationId) {}
}
