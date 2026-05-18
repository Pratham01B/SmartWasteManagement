package com.smartwaste.repository;

import com.smartwaste.entity.CollectionRoute;
import com.smartwaste.entity.CollectionRoute.RouteStatus;
import com.smartwaste.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CollectionRouteRepository extends JpaRepository<CollectionRoute, Long> {

    List<CollectionRoute> findByWorker(User worker);

    List<CollectionRoute> findByWorkerAndScheduledDate(User worker, LocalDate date);

    List<CollectionRoute> findByScheduledDateAndStatus(LocalDate date, RouteStatus status);

    List<CollectionRoute> findByPincodeAndScheduledDate(String pincode, LocalDate date);

    long countByStatus(RouteStatus status);
}
