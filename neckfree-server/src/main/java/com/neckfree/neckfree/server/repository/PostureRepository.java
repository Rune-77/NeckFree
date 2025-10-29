package com.neckfree.neckfree.server.repository;

import com.neckfree.neckfree.server.entity.PostureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostureRepository extends JpaRepository<PostureRecord, Long> {
    List<PostureRecord> findByUserId(String userId);
}
