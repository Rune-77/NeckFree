package com.neckfree.neckfree.server.controller;

import com.neckfree.neckfree.server.entity.PostureRecord;
import com.neckfree.neckfree.server.service.PostureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posture")
public class PostureController {

    private final PostureService postureService;

    public PostureController(PostureService postureService) {
        this.postureService = postureService;
    }

    @GetMapping("/all")
    public List<PostureRecord> getAllRecords() {
        return postureService.getAllRecords();
    }

    @PostMapping("/save")
    public PostureRecord saveRecord(@RequestBody PostureRecord record) {
        return postureService.saveRecord(record);
    }
}
