package sonny;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.time.Duration;

public class App {

    private static DiscordApi api;
    private static List<String> randomMessages = new ArrayList<>();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!(\\w+)\\s+(\\S+)\\s*(.*)$");

    public static void handleCommands(String messageContent, TextChannel channel, MessageCreateEvent event) {
        Matcher matcher = COMMAND_PATTERN.matcher(messageContent);
        if (matcher.find()) {
            System.out.println("handleCommands triggered with content: " + messageContent); // Debug log
            String command = matcher.group(1);
            String userId = matcher.group(2);
            String reason = matcher.group(3);

            api.getUserById(userId).thenAccept(user -> {
                if (user != null) {
                    Server server = event.getServer().orElse(null);
                    switch (command.toLowerCase()) {
                        case "ban":
                            String[] parts = reason.split("\\s", 2);
                            int durationInDays = Integer.parseInt(parts[0]);
                            String banReason = parts.length > 1 ? parts[1] : "";
                            Duration duration = Duration.ofDays(durationInDays);
                            channel.sendMessage("Banned user: " + user.getName() + " for reason: " + banReason + " for duration: " + durationInDays + " days.");
                            server.banUser(user, duration, banReason);
                            break;
                        case "kick":
                            channel.sendMessage("Kicked user: " + user.getName() + " for reason: " + reason);
                            server.kickUser(user, reason); // Implement kick logic
                            break;
                        case "mute":
                            String[] parts1 = reason.split("\\s", 2);
                            String time = parts1[0];
                            String muteReason = parts1.length > 1 ? parts1[1] : "";
                            channel.sendMessage("Muted user: " + user.getName() + " for reason: " + muteReason + " for duration: " + time);
                            // You will need to implement your own logic for muting the user based on the duration and reason
                            break;
                        default:
                            channel.sendMessage("Unknown command.");
                            break;
                    }
                } else {
                    channel.sendMessage("User not found.");
                }
            });
        }
    }

    public static void main(String[] args) {
        api = new DiscordApiBuilder()
                .setToken("MTEyOTUzMjQ1NTY5MTUwNTc5NQ.Gq-TNV.8aXMGtiIoIilR_-rOIV5GhykedyHw-W3oQ0ILI")  // Remember not to hardcode the token!
                .setAllIntents()
                .login()
                .join();
                api.updateActivity("Download Bright Eye!");


        api.addMessageCreateListener(event -> {
            String messageContent = event.getMessageContent();

            if (messageContent.startsWith("!add randommessage ")) {
                String newMessage = messageContent.substring("!add randommessage ".length());
                randomMessages.add(newMessage);
                event.getChannel().sendMessage("Added new message: " + newMessage);
            } else if (messageContent.equals("!view randommessages")) {
                if (randomMessages.isEmpty()) {
                    event.getChannel().sendMessage("No random messages added yet.");
                    return;
                }
        
                StringBuilder sb = new StringBuilder("Current random messages:\n");
                int index = 1;
                for (String msg : randomMessages) {
                    sb.append(index++).append(". ").append(msg).append("\n");
                }
                sb.append("Use `!delete randommessage [number]` to delete a message.");
                event.getChannel().sendMessage(sb.toString());
            } else if (messageContent.startsWith("!delete randommessage ")) {
                try {
                    int msgNumber = Integer.parseInt(messageContent.substring("!delete randommessage ".length()));
                    if (msgNumber > 0 && msgNumber <= randomMessages.size()) {
                        String deletedMsg = randomMessages.remove(msgNumber - 1);
                        event.getChannel().sendMessage("Deleted message: " + deletedMsg);
                    } else {
                        event.getChannel().sendMessage("Invalid message number.");
                    }
                } catch (NumberFormatException e) {
                    event.getChannel().sendMessage("Please provide a valid message number to delete.");
                }
            } else if (COMMAND_PATTERN.matcher(messageContent).matches()) {
                handleCommands(messageContent, event.getChannel(), event);
            }
        });

        api.addServerMemberJoinListener(event -> {
            User newUser = event.getUser();
            String welcomeMessage = "Hi! Welcome to the Bright Eye server, we are one of the premier AI mobile apps on the IOS app store that provides generative and analytical AI services.\n"
            + "Check us out on the app store: https://apps.apple.com/us/app/bright-eye/id1593932475\n"
            + "We provide:\n"
            + "• Text generation via GPT-4\n"
            + "• Image generation via Stable Diffusion\n"
            + "• Specialized generations such as poem, code, and short story generations!\n"
            + "• Analytical AI including: image captioning, Text analysis, Person counts and statistics.\n"
            + "Enjoy the community and the app!";
            newUser.sendMessage(welcomeMessage);
        });

        String channelId = "1040284766890635295";  // Example channelId
        postRandomMessages("1040284766450221096", channelId);  // Example guildId and channelId

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int estimatedUserCount = getEstimatedUserCount();
                System.out.println("Estimated User Count: " + estimatedUserCount);
            }
        }, 0, 60 * 60 * 1000);  // Print estimated user count hourly
    }

    public static void postRandomMessages(String guildId, String channelId) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (randomMessages.isEmpty()) return;

                Random random = new Random();
                int index = random.nextInt(randomMessages.size());
                String randomMessage = randomMessages.get(index);

                Server guild = api.getServerById(guildId).orElse(null);
                if (guild != null) {
                    TextChannel channel = guild.getTextChannelById(channelId).orElse(null);
                    if (channel != null) {
                        channel.sendMessage(randomMessage);
                    }
                }
            }
        }, 0, 60 * 60 * 1000);  // Post random messages hourly
    }

    public static int getEstimatedUserCount() {
        // Your implementation to fetch the user count 
        // For now, returning a mock value for demonstration purposes
        return 1500;  
    }
}