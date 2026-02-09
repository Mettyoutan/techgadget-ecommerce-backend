package com.techgadget.ecommerce.repository;

import com.techgadget.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u " +
                  "left join u.addresses " +
                  "where u.id = :id")
    Optional<User> findByIdWithAddress(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    @Query("select u from User u " +
            "left join u.addresses " +
            "where u.email = :email")
    Optional<User> findByEmailWithAddresses(@Param("email") String email);

    Optional<User> findByUsername(String username);

    @Query("select u from User u " +
                  "left join u.addresses " +
                  "where u.username = :username")
    Optional<User> findByUsernameWithAddress(@Param("username") String username);

    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);

    boolean existsByEmailAndUsername(String email, String username);
}
