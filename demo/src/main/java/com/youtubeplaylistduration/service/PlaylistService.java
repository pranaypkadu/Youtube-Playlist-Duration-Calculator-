package com.youtubeplaylistduration.service;

import com.youtubeplaylistduration.model.PlaylistDuration;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class PlaylistService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public PlaylistDuration calculatePlaylistDuration(String playlistId) {
        try {
            YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null
            )
            .setApplicationName("YouTube-Playlist-Duration")
            .build();

            List<String> videoIds = fetchVideoIdsFromPlaylist(youtube, playlistId);
            Duration totalDuration = calculateTotalDuration(youtube, videoIds);

            // Create and populate PlaylistDuration object
            PlaylistDuration playlistDuration = new PlaylistDuration();
            playlistDuration.setTotalLength(formatDuration(totalDuration));
            playlistDuration.setAt1_25x(formatDuration(scaleDuration(totalDuration, 1.25)));
            playlistDuration.setAt1_50x(formatDuration(scaleDuration(totalDuration, 1.50)));
            playlistDuration.setAt1_75x(formatDuration(scaleDuration(totalDuration, 1.75)));
            playlistDuration.setAt2_00x(formatDuration(scaleDuration(totalDuration, 2.00)));

            return playlistDuration;

        } catch (Exception e) {
            throw new RuntimeException("Error calculating playlist duration", e);
        }
    }

    private List<String> fetchVideoIdsFromPlaylist(YouTube youtube, String playlistId) throws Exception {
        List<String> videoIds = new ArrayList<>();
        YouTube.PlaylistItems.List request = youtube.playlistItems()
            .list(Arrays.asList("contentDetails"))
            .setPlaylistId(playlistId)
            .setKey(apiKey)
            .setMaxResults(50L);

        String nextPageToken = null;
        do {
            request.setPageToken(nextPageToken);
            PlaylistItemListResponse response = request.execute();
            for (PlaylistItem item : response.getItems()) {
                videoIds.add(item.getContentDetails().getVideoId());
            }
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null);

        return videoIds;
    }

    private Duration calculateTotalDuration(YouTube youtube, List<String> videoIds) throws Exception {
        Duration totalDuration = Duration.ZERO;

        for (int i = 0; i < videoIds.size(); i += 50) {
            List<String> batch = videoIds.subList(i, Math.min(i + 50, videoIds.size()));

            YouTube.Videos.List request = youtube.videos()
                .list(Arrays.asList("contentDetails"))
                .setId(batch)
                .setKey(apiKey);

            VideoListResponse response = request.execute();
            for (Video video : response.getItems()) {
                Duration videoDuration = Duration.parse(video.getContentDetails().getDuration());
                totalDuration = totalDuration.plus(videoDuration);
            }
        }

        return totalDuration;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }

    private Duration scaleDuration(Duration duration, double speedFactor) {
        long scaledSeconds = (long) (duration.getSeconds() / speedFactor);
        return Duration.ofSeconds(scaledSeconds);
    }
}
