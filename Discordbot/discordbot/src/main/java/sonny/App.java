package sonny;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import sonny.services.DatabaseConnectionService;

import org.javacord.api.entity.permission.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;

public class App {

    private static DiscordApi api;
    private static List<String> randomMessages = new ArrayList<>();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!(\\w+)(?:\\s+(\\S+)(?:\\s+(.*))?)?$");
    private static final String ANNOUNCEMENTS_CHANNEL_ID = "1055148431288582194"; 
    private static final String SUGGESTION_CHANNEL_ID = "1055996985032847440";
    private static final String BUG_CHANNEL_ID = "1055997017471590411";

    public static void handleCommands(String messageContent, TextChannel channel, MessageCreateEvent event) {

        Matcher matcher = COMMAND_PATTERN.matcher(messageContent);
        if (matcher.find()) {
            System.out.println("handleCommands triggered with content: " + messageContent); // Debug log
            String command = matcher.group(1);
            String userId = matcher.group(2);
            String reason = matcher.group(3);

            if (command.equals("!testInsert")) {
                DatabaseConnectionService.insertDummyBug();
                
            }

            switch (command.toLowerCase()) {
                case "viewsuggestions":
                    viewSuggestions(channel);
                    return;
                case "viewbugs":
                    viewBugs(channel);
                    return;
            }
            api.getUserById(userId).thenAccept(user -> {
                if (user != null) {
                    Server server = event.getServer().orElse(null);
                    switch (command.toLowerCase()) {
                        case "ban":
                            if (reason == null) {
                                channel.sendMessage("Please provide a ban duration and reason.");
                                return;
                            }
                            String[] parts = reason.split("\\s", 2);
                            int durationInDays = Integer.parseInt(parts[0]);
                            String banReason = parts.length > 1 ? parts[1] : "";
                            Duration duration = Duration.ofDays(durationInDays);
                            channel.sendMessage("Banned user: " + user.getName() + " for reason: " + banReason + " for duration: " + durationInDays + " days.");
                            server.banUser(user, duration, banReason);
                            break;
                        case "kick":
                            if (reason == null) {
                                channel.sendMessage("Please provide a kick reason.");
                                return;
                            }
                            channel.sendMessage("Kicked user: " + user.getName() + " for reason: " + reason);
                            server.kickUser(user, reason);
                            break;
                        case "mute":
                            if (reason == null) {
                                channel.sendMessage("Please provide a mute duration and reason.");
                                return;
                            }
                            String[] parts1 = reason.split("\\s", 2);
                            String time = parts1[0];
                            String muteReason = parts1.length > 1 ? parts1[1] : "";
                            channel.sendMessage("Muted user: " + user.getName() + " for reason: " + muteReason + " for duration: " + time);
                            //must implement more logic to complete the mute cmd.
                            break;
                        default:
                            channel.sendMessage("Unknown command or incorrect format.");
                            break;
                    }
                } else {
                    channel.sendMessage("User not found.");
                }
            });
        

            
            
            if (messageContent.startsWith("!announceupdate ")) {
                String[] parts = messageContent.substring("!announceupdate ".length()).split("\\|");
            
                if (parts.length < 2) {
                    channel.sendMessage("Incorrect format. Use `!announceupdate [version] | [details]`.");
                    return;
                }

                String version = parts[0].trim();
                String details = parts[1].trim();
                String formattedAnnouncement = String.format(
                "📢 New App Update 📢\n\nVersion: %s\nDetails: %s", 
                version, 
                details
            );

                TextChannel announcementChannel = api.getTextChannelById(ANNOUNCEMENTS_CHANNEL_ID).orElse(null);
                if (announcementChannel != null) {
                    announcementChannel.sendMessage(formattedAnnouncement);
                } else {
                    channel.sendMessage("Failed to send the announcement. Please check if the announcement channel ID is correct.");
                }
            }
        }
    }

    public static void main(String[] args) {
        api = new DiscordApiBuilder()
            .setToken("private token information")
            .setAllIntents()
            .login()
            .join();
            api.updateActivity("Download Bright Eye!");


        api.addMessageCreateListener(event -> {
            String messageContent = event.getMessageContent();

        // Check if it's a suggestion
        if (messageContent.startsWith("!suggestion ") && SUGGESTION_CHANNEL_ID.equals(event.getChannel().getIdAsString())) {
        String suggestion = messageContent.substring("!suggestion ".length());
    
        // Get current time in Eastern Time Zone
        ZonedDateTime nowInEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z");
        String formattedTime = nowInEastern.format(formatter);

        // Create the embed
        EmbedBuilder embed = new EmbedBuilder()
        .setTitle("New Suggestion")
        .setDescription(suggestion)
        .setFooter("Suggested by: " + event.getMessageAuthor().getDisplayName())
        .setThumbnail(event.getMessageAuthor().getAvatar())
        .addField("Submitted At", formattedTime, false)
        .addField("Votes", "👍 0% | 👎 0%", false);  // initial percentage; this will change with actual tracking
    
            
        event.getChannel().sendMessage(embed).thenAcceptAsync(msg -> {
            msg.addReaction("👍");  // thumbs up
            msg.addReaction("👎");  // thumbs down
    });
    }

    
    
    // Check if it's a bug report
    else if (messageContent.startsWith("!bug ") && BUG_CHANNEL_ID.equals(event.getChannel().getIdAsString())) {
        String bug = messageContent.substring("!bug ".length());
        
        // Get current time in Eastern Time Zone
        ZonedDateTime nowInEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z");
        String formattedTime = nowInEastern.format(formatter);

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Bug Reported")
            .setDescription(bug)
            .setFooter("Reported by: " + event.getMessageAuthor().getDisplayName())
            .setThumbnail(event.getMessageAuthor().getAvatar())
            .addField("Submitted At", formattedTime, false);
        
        event.getChannel().sendMessage(embed).thenAcceptAsync(msg -> {
            msg.addReaction("👍");  // thumbs up for 'acknowledged'
            msg.addReaction("👎");  // thumbs down for 'not a bug' or 'can't reproduce'
        });
    }


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
            Server server = event.getServer();
                // Find the "verified" role by its name
            List<Role> verifiedRoles = server.getRolesByName("Member");
                if (!verifiedRoles.isEmpty()) {
                    Role verifiedRole = verifiedRoles.get(0); // Get the first "verified" role, assuming there's only one
                    server.addRoleToUser(newUser, verifiedRole); // Assign the role to the new user
                } else {
                    System.out.println("Member role not found!");
                }
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
        // implementation to fetch the user count 
        // Will look into AWS API in future to find out if I can fetch user count. For now, returning a mock value for demonstration purposes
        return 1500;  
    }

    private static void viewSuggestions(TextChannel channel) {
        System.out.println("Entering viewSuggestions method...");  // Debug statement at start
        try (Connection conn = DatabaseConnectionService.getConnection()) {
        Statement stmt = conn.createStatement();
        System.out.println("Executing query for suggestions...");  // Debug statement after DB query
        ResultSet rs = stmt.executeQuery("SELECT * FROM Suggestions");

        StringBuilder sb = new StringBuilder("Suggestions:\n");
        while (rs.next()) {
            String suggestion = rs.getString("suggestion_text");  // assuming column name in database is 'suggestion_text'
            sb.append("- ").append(suggestion).append("\n");
        }
        channel.sendMessage(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error encountered in viewSuggestions method: " + e.getMessage());  // Debug statement in catch block
            channel.sendMessage("Error fetching suggestions.");
        }
    }

    private static void viewBugs(TextChannel channel) {
        System.out.println("Entering viewBugs method...");  // Debug statement at start
        try (Connection conn = DatabaseConnectionService.getConnection()) {
            Statement stmt = conn.createStatement();
            System.out.println("Executing query for bugs...");  // Debug statement after DB query
            ResultSet rs = stmt.executeQuery("SELECT * FROM Bugs");
    
            StringBuilder sb = new StringBuilder("Bugs:\n");
            while (rs.next()) {
                String bug = rs.getString("bug_text");  // assuming column name in database is 'bug_text'
                sb.append("- ").append(bug).append("\n");
            }
            channel.sendMessage(sb.toString());
    
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error encountered in viewBugs method: " + e.getMessage());  // Debug statement in catch block
            channel.sendMessage("Error fetching bugs.");
        }
    }
}
