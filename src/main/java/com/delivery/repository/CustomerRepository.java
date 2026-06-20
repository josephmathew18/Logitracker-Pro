package com.delivery.repository;

import com.delivery.model.Customer;
import com.delivery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserUsername(String username);
    Optional<Customer> findByEmail(String email);
}

