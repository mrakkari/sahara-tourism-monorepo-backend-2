package com.camping.duneinsolite.repository;

import com.camping.duneinsolite.model.Reservation;
import com.camping.duneinsolite.model.User;
import com.camping.duneinsolite.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByUserUserId(UUID userId);
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    @Query("SELECT r FROM Reservation r WHERE LOWER(r.user.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Reservation> searchByUserName(@Param("name") String name);
}