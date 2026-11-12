
package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CustomerAddress;

@Repository
public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Integer> {

    List<CustomerAddress> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    Optional<CustomerAddress> findByAddressIdAndUser_UserId(
            Integer addressId, Integer userId);

    Optional<CustomerAddress> findByUser_UserIdAndDefaultAddressTrue(
            Integer userId);

    boolean existsByUser_UserId(Integer userId);

    void deleteByUser_UserId(Integer userId);
}
