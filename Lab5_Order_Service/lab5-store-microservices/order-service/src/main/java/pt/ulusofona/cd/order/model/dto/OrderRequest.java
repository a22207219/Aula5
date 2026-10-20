package pt.ulusofona.cd.order.model.dto;
import jakarta.validation.constraints.*;import java.util.UUID;
public record OrderRequest(@NotNull UUID productId,@Min(1) int quantity){}
