package first.gradle.project;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import javax.security.auth.login.LoginException;

public class Bot implements EventListener {

    private static String bot_token = "Enter bot access token here";
    private static char prefix = '$';

    public static void main(String[] args) throws LoginException, InterruptedException {
        JDABuilder.createDefault(bot_token)
                .addEventListeners(new Bot())
                .enableCache(CacheFlag.VOICE_STATE)
                .build().awaitReady();
    }

    @Override
    public void onEvent(@NotNull GenericEvent e) {
        if (e instanceof MessageReceivedEvent event){

            if (event.getAuthor().isBot() || event.getMember() == null) return;

            String message = event.getMessage().getContentRaw();
            TextChannel channel = event.getTextChannel();
            Role djRole = event.getGuild().getRolesByName("DJ", false).get(0);

            if (message.equals(prefix + "join")) {
                GuildVoiceState userVoice = event.getMember().getVoiceState();

                if (userVoice == null) return;

                AudioChannel ac = userVoice.getChannel();

                if (ac == null) return;

                AudioManager audioManager = event.getGuild().getAudioManager();

                if (audioManager.isConnected()) return;

                audioManager.openAudioConnection(ac);
            }else if (message.equals(prefix + "leave")){
                if (!event.getMember().getRoles().contains(djRole)) return;

                GuildVoiceState userVoice = event.getMember().getVoiceState();

                if (userVoice == null) return;

                AudioChannel ac = userVoice.getChannel();

                if (ac == null) return;

                AudioManager audioManager = event.getGuild().getAudioManager();

                if (audioManager.getConnectedChannel() != ac) return;

                audioManager.closeAudioConnection();
            }else if (message.contains(prefix + "p") || message.contains(prefix + "play")) {

                if (message.indexOf(prefix + "p ") > 0 || message.indexOf(prefix + "play ") > 0) return;

                PlayerManager manager = PlayerManager.getInstance();
                GuildVoiceState userVoice = event.getMember().getVoiceState();

                if (userVoice == null) return;

                AudioChannel ac = userVoice.getChannel();

                if (ac == null) return;

                AudioManager audioManager = event.getGuild().getAudioManager();

                if (!audioManager.isConnected()) audioManager.openAudioConnection(ac);

                String search = message.substring(message.indexOf(" ") + 1);

                manager.load(channel, search);
                manager.getGuildMusicManager(event.getGuild()).player.setVolume(50);
            } else if (message.equals(prefix + "q") || message.equals(prefix + "queue")) {
                GuildVoiceState userVoice = event.getMember().getVoiceState();
                PlayerManager manager = PlayerManager.getInstance();

                if (userVoice == null) return;

                AudioChannel ac = userVoice.getChannel();

                if (ac == null) return;

                AudioManager audioManager = event.getGuild().getAudioManager();

                if (audioManager.getConnectedChannel() != ac) return;

                manager.showQueue(channel);
            } else if (message.equals(prefix + "s") || message.equals(prefix + "skip")) {
                if (!event.getMember().getRoles().contains(djRole)) return;
                
                GuildVoiceState userVoice = event.getMember().getVoiceState();
                PlayerManager manager = PlayerManager.getInstance();

                if (userVoice == null) return;

                AudioChannel ac = userVoice.getChannel();

                if (ac == null) return;

                AudioManager audioManager = event.getGuild().getAudioManager();

                if (audioManager.getConnectedChannel() != ac) return;

                manager.getGuildMusicManager(event.getGuild()).scheduler.nextTrack();
            }
        }
    }
}
