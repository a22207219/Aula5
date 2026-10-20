package pt.ulusofona.cd.order.model;
import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="orders") public class Order {
@Id private UUID id;@Column(name="product_id",nullable=false) private UUID productId;@Column(nullable=false) private int quantity;@Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;@Column(name="created_at",nullable=false) private Instant createdAt;@Column(name="updated_at",nullable=false) private Instant updatedAt;@Version private long version;@Column(name="restore_required",nullable=false) private boolean restoreRequired;
protected Order(){} public Order(UUID productId,int quantity){this.id=UUID.randomUUID();this.productId=productId;this.quantity=quantity;this.status=Status.PENDING;this.createdAt=Instant.now();this.updatedAt=createdAt;}
public UUID getId(){return id;}public UUID getProductId(){return productId;}public int getQuantity(){return quantity;}public Status getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public long getVersion(){return version;}public void setStatus(Status value){status=value;updatedAt=Instant.now();}public void change(UUID productId,int quantity){this.productId=productId;this.quantity=quantity;updatedAt=Instant.now();}
public boolean isRestoreRequired(){return restoreRequired;}public void setRestoreRequired(boolean v){restoreRequired=v;}public enum Status {PENDING,CONFIRMING,CONFIRMED,CANCELLING,CANCELLED,COMPENSATION_REQUIRED}
}
