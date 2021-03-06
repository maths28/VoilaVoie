package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Audio;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Audio entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AudioRepository extends JpaRepository<Audio,Long> {
    
}
