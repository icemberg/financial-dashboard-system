package com.financedashboard.zorvyn.repository.interfaces;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.financedashboard.zorvyn.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
}