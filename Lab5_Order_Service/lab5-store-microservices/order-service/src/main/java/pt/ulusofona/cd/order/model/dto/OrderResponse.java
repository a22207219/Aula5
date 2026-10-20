package pt.ulusofona.cd.order.model.dto;
import pt.ulusofona.cd.order.model.Order;import java.time.Instant;import java.util.UUID;
public record OrderResponse(UUID id,UUID productId,int quantity,Order.Status status,Instant createdAt,Instant updatedAt,long version){public static OrderResponse from(Order o){return new OrderResponse(o.getId(),o.getProductId(),o.getQuantity(),o.getStatus(),o.getCreatedAt(),o.getUpdatedAt(),o.getVersion());}}
