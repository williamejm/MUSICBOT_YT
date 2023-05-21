package first.gradle.project;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class PlayerManager {

    private static PlayerManager instance;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private PlayerManager() {
        this.playerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new HashMap<>();

        AudioSourceManagers.registerRemoteSources(this.playerManager);
        AudioSourceManagers.registerLocalSource(this.playerManager);
    }

    public synchronized GuildMusicManager getGuildMusicManager(Guild guild) {
        GuildMusicManager musicManager = this.musicManagers.get(guild.getIdLong());

        if (musicManager == null) {
            musicManager = new GuildMusicManager(this.playerManager);
            this.musicManagers.put(guild.getIdLong(), musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public void load(TextChannel channel, String url) {
        if (!isURL(url)) {
            url = "ytsearch:" + url;
        }

        final String link = url;
        GuildMusicManager musicManager = this.getGuildMusicManager(channel.getGuild());
        this.playerManager.loadItemOrdered(musicManager, link, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (musicManager.player.getPlayingTrack() == null) {
                    channel.sendMessageFormat(":arrow_forward:  Now Playing **%s** by **%s**.", track.getInfo().title, track.getInfo().author).queue();
                } else {
                    channel.sendMessageFormat(":play_pause:  Added **%s** by **%s** to the queue.", track.getInfo().title, track.getInfo().author).queue();
                }
                play(musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getTracks().get(0);
                if (!isURL(link)) {
                    if (musicManager.player.getPlayingTrack() == null) {
                        channel.sendMessageFormat(":arrow_forward:  Now Playing **%s** by **%s**.", firstTrack.getInfo().title, firstTrack.getInfo().author).queue();
                    } else {
                        channel.sendMessageFormat(":play_pause:  Added **%s** by **%s**.", firstTrack.getInfo().title, firstTrack.getInfo().author).queue();
                    }
                    play(musicManager, firstTrack);
                } else {
                    if (musicManager.player.getPlayingTrack() == null) {
                        channel.sendMessageFormat(":arrow_forward:  Now Playing **%s** by **%s** from playlist [**%s**].", firstTrack.getInfo().title, firstTrack.getInfo().author, playlist.getName()).queue();
                    } else {
                        channel.sendMessageFormat(":play_pause:  Added **%s** by **%s** from playlist [**%s**] to the queue.", firstTrack.getInfo().title, firstTrack.getInfo().author, playlist.getName()).queue();
                    }
                    for (int i = 0; i < playlist.getTracks().size(); i++) {
                        play(musicManager, playlist.getTracks().get(i));
                    }
                    channel.sendMessageFormat(":play_pause:  Added **%d** more songs from playlist [**%s**] to the queue.", playlist.getTracks().size() - 1, playlist.getName()).queue();
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessageFormat(":x:  Invalid URL: `%s`.", link).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessageFormat(":x:  Could not load: `%s`.", exception.getMessage()).queue();
            }
        });
    }

    public void showQueue(TextChannel channel) {
        GuildMusicManager musicManager = this.getGuildMusicManager(channel.getGuild());
        channel.sendMessageFormat(":arrow_forward:  Now Playing **%s** by **%s**.", musicManager.player.getPlayingTrack().getInfo().title, musicManager.player.getPlayingTrack().getInfo().author).queue();

        int i = 1;
        for (AudioTrack track: musicManager.scheduler.queue) {
            channel.sendMessageFormat(i + ": **%s** by **%s**", track.getInfo().title, track.getInfo().author).queue();
            i++;
        }
    }

    private void play(GuildMusicManager musicManager, AudioTrack track) {
        if (musicManager.player.getPlayingTrack() == null) {
            musicManager.player.playTrack(track);
        } else {
            musicManager.scheduler.queue(track);
        }
    }

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public boolean isURL(String url){
        if (!url.contains(".com")) {
            return false;
        } else {
            try {
                new URI(url);
                return true;
            } catch (URISyntaxException e){
                return false;
            }
        }
    }
}
