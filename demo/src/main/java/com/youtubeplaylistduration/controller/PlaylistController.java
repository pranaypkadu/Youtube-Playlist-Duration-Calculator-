package com.youtubeplaylistduration.controller;

import com.youtubeplaylistduration.model.PlaylistDuration;
import com.youtubeplaylistduration.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlist") // Base path for all endpoints in this controller
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @GetMapping("/duration") // Maps to /api/playlist/duration
    public PlaylistDuration getPlaylistDuration(@RequestParam String playlistId) {
        return playlistService.calculatePlaylistDuration(playlistId);
    }
}
