package com.example.demologin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"store"})
    Optional<User> findWithStoreByUsername(String username);

    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndUserIdNot(String email, Long userId);


    Page<User> findByRole_Name(String roleName, Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByRole_NameAndStatus(String roleName, UserStatus status, Pageable pageable);

    List<User> findAllByRole_Name(String roleName);

    boolean existsByRole_Id(Long id);
}
