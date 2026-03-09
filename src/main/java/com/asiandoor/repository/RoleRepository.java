package com.asiandoor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asiandoor.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
