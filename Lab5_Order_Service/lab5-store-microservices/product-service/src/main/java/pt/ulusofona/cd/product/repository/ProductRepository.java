package pt.ulusofona.cd.product.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.ulusofona.cd.product.model.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface ProductRepository extends JpaRepository<Product,UUID>{
@Modifying @Query(value="UPDATE product SET stock=stock-:quantity, version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=:id AND stock>=:quantity",nativeQuery=true) int decrement(@Param("id") UUID id,@Param("quantity") int quantity);
@Modifying @Query(value="UPDATE product SET stock=stock+:quantity, version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=:id",nativeQuery=true) int increment(@Param("id") UUID id,@Param("quantity") int quantity);
@Query(value="SELECT stock FROM product WHERE id=:id",nativeQuery=true) Integer stock(@Param("id") UUID id);
long countBySupplierId(UUID supplierId);boolean existsBySkuIgnoreCase(String sku);}
