package com.neckfree.neckfree.server.service;

import com.neckfree.neckfree.server.entity.PostureRecord;
import com.neckfree.neckfree.server.repository.PostureRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostureService {

    private final PostureRepository postureRepository;

    public PostureService(PostureRepository postureRepository) {
        this.postureRepository = postureRepository;
    }

    public List<PostureRecord> getAllRecords() {
        return postureRepository.findAll();
    }

    public PostureRecord saveRecord(PostureRecord record) {
        return postureRepository.save(record);
    }
}
