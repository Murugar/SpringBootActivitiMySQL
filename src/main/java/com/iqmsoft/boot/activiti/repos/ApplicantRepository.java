package com.iqmsoft.boot.activiti.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iqmsoft.boot.activiti.model.Applicant;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

}