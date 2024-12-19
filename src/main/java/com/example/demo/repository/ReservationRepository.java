package com.example.demo.repository;

import com.example.demo.entity.Reservation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdAndItemId(Long userId, Long itemId);

    // Fetch Join : 한 번에 필요한 모든 정보를 가져옴
    // 따로따로 정보를 가져올 필요가 없어서 효율적
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.item i")
    List<Reservation> findAllUserAndItem();

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByItemId(Long itemId);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.item.id = :id " +
            "AND NOT (r.endAt <= :startAt OR r.startAt >= :endAt) " +
            "AND r.status = 'APPROVED'")
    List<Reservation> findConflictingReservations(
            @Param("id") Long id,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    default Reservation findByIdOrElseThrow(Long reservationId) {
        return findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 데이터가 존재하지 않습니다."));
    }
}
