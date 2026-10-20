package pt.ulusofona.cd.order.client;
import org.springframework.web.service.annotation.*;import org.springframework.web.bind.annotation.PathVariable;import java.util.UUID;
@HttpExchange("/api/v1/suppliers") public interface SupplierClient {@GetExchange("/{id}") SupplierInfo get(@PathVariable UUID id);record SupplierInfo(UUID id,boolean active) {}}
