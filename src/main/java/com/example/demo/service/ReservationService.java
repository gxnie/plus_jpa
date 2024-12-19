package com.example.demo.service;

import com.example.demo.dto.ReservationResponseDto;
import com.example.demo.entity.*;
import com.example.demo.exception.ReservationConflictException;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jdk.incubator.vector.Byte128Vector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.demo.entity.ReservationStatus.*;


@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final RentalLogService rentalLogService;
    private final JPAQueryFactory queryFactory;

    public ReservationService(ReservationRepository reservationRepository,
                              ItemRepository itemRepository,
                              UserRepository userRepository,
                              RentalLogService rentalLogService, JPAQueryFactory queryFactory) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.rentalLogService = rentalLogService;
        this.queryFactory = queryFactory;
    }

    // TODO: 1. 트랜잭션 이해
    /**
     * Reservation 저장 -> 성공
     * RentalLog 저장 -> 실패 (2중 저장)
     * 트랜잭션 -> 하나라도 실패하면 모든 작업을 되돌림(롤백)
     * */
    @Transactional
    public void createReservation(Long itemId, Long userId, LocalDateTime startAt, LocalDateTime endAt) {
        // 쉽게 데이터를 생성하려면 아래 유효성검사 주석 처리
//        List<Reservation> haveReservations = reservationRepository.findConflictingReservations(itemId, startAt, endAt);
//        if(!haveReservations.isEmpty()) {
//            throw new ReservationConflictException("해당 물건은 이미 그 시간에 예약이 있습니다.");
//        }
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 값이 존재하지 않습니다."));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 값이 존재하지 않습니다."));

        Reservation reservation = new Reservation(item, user, "PENDING", startAt, endAt);
        Reservation savedReservation = reservationRepository.save(reservation);

        RentalLog rentalLog = new RentalLog(savedReservation, "로그 메세지", "CREATE");
        rentalLogService.save(rentalLog);
    }

    // TODO: 3. N+1 문제
    public List<ReservationResponseDto> getReservations() {
        List<Reservation> reservations = reservationRepository.findAllUserAndItem();

        return reservations.stream().map(reservation -> {
            User user = reservation.getUser();
            Item item = reservation.getItem();

            return new ReservationResponseDto(
                    reservation.getId(),
                    user.getNickname(),
                    item.getName(),
                    reservation.getStartAt(),
                    reservation.getEndAt()
            );
        }).toList();
    }

    // TODO: 5. QueryDSL 검색 개선
    public List<ReservationResponseDto> searchAndConvertReservations(Long userId, Long itemId) {
        List<Reservation> reservations = searchReservations(userId, itemId);
        return convertToDto(reservations);
    }

    public List<Reservation> searchReservations(Long userId, Long itemId) {
        // QueryDSL 사용하기 위ㅎ서 Q타입 객체 선언(Reservation,user,item 엔티티 기반)
        QReservation reservation = QReservation.reservation;
        QUser user = QUser.user;
        QItem item = QItem.item;

        // selectFrom(조회대상)을 reservation 엔티티로 지정
        // 연관관계가 있는 User, Item 엔티티를 LEFT JOIN 해주면서, FetchJoin으로 한 번에 연관 데이터 가져옴
        JPAQuery<Reservation> query = queryFactory
                .selectFrom(reservation)
                .leftJoin(reservation.user, user).fetchJoin()
                .leftJoin(reservation.item, item).fetchJoin();

        // userId와 itemId가 null이 아닐 경우에만 where 조건 추가
        // -> userId, itemId 중 null인게 있을때, where 조건 적용하지 않고, 모든 예약을 조회하거나 하나의 조건만 설정가능
        if (userId != null) {
            query.where(reservation.user.id.eq(userId));
        }

        if (itemId != null) {
            query.where(reservation.item.id.eq(itemId));
        }

        // 쿼리 실행하고 결과 리스트 반환!
        return query.fetch();
    }

    private List<ReservationResponseDto> convertToDto(List<Reservation> reservations) {
        return reservations.stream()
                .map(reservation -> new ReservationResponseDto(
                        reservation.getId(),
                        reservation.getUser().getNickname(),
                        reservation.getItem().getName(),
                        reservation.getStartAt(),
                        reservation.getEndAt()
                ))
                .toList();
    }

    // TODO: 7. 리팩토링
    @Transactional
    public ReservationResponseDto updateReservationStatus(Long reservationId, String status) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 데이터가 존재하지 않습니다."));
        ReservationStatus newStatus = ReservationStatus.valueOf(status);

        // reservation 엔티티에서 현재 상태를 가져온다 (Enum 타입)
        ReservationStatus currentStatus = reservation.getStatus();

        /**
         * newStatus에 따라서 조건 예외처리
         * APPROVED로 변경하려는 경우: 현재 상태가 PENDING일 때만 가능, 아니면 예외
         * CANCELED로 변경하려는 경우: 현재 상태가 EXPIRED가 아닐 때만 가능, EXPIRED 상태인 예약은 취소 할 수 없음
         * EXPIRED로 변경하려는 경우: 현재 상태가 PENDING일 때만 가능, 아니면 예외
         * 위의 조건 제외하고 정의되지 않은 상태인 경우 예외
         * */
        switch (newStatus) {
            case APPROVED:
                if (currentStatus != ReservationStatus.PENDING) {
                    throw new IllegalArgumentException("PENDING 상태만 APPROVED로 변경 가능합니다.");
                }
                break;
            case CANCELED:
                if (currentStatus == EXPIRED) {
                    throw new IllegalArgumentException("EXPIRED 상태인 예약은 취소할 수 없습니다.");
                }
                break;
            case EXPIRED:
                if (currentStatus != ReservationStatus.PENDING) {
                    throw new IllegalArgumentException("PENDING 상태만 EXPIRED로 변경 가능합니다.");
                }
                break;
            default:
                throw new IllegalArgumentException("올바르지 않은 상태: " + status);
        }
        reservation.updateStatus(newStatus);
        return convertToDto(reservation);
    }

    private ReservationResponseDto convertToDto(Reservation reservation) {
        String nickname = reservation.getUser().getNickname(); // user 객체에서 닉네임 얻어옴
        String itemName = reservation.getItem().getName();     // item 객체에서 이름 얻어옴
        LocalDateTime startAt = reservation.getStartAt();
        LocalDateTime endAt = reservation.getEndAt();

        return new ReservationResponseDto(reservation.getId(), nickname, itemName, startAt, endAt);
    }
}
