package pt.ulusofona.cd.order.repository;
import pt.ulusofona.cd.order.model.Order;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import jakarta.persistence.LockModeType;import java.util.*;
public interface OrderRepository extends JpaRepository<Order,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Order o where o.id = :id") Optional<Order> locked(@Param("id") UUID id); List<Order> findTop100ByStatusOrderByUpdatedAtAsc(Order.Status status);}
