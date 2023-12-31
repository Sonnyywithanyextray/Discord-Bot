package sonny;

import sonny.services.CognitoHelper;
import sonny.services.DatabaseConnectionService;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;


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
                        case "postupdate":
                            if (reason == null) {
                                channel.sendMessage("Please provide the update message.");
                                return;
                            }
                            postUpdateToChannel(reason);
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
            .setToken("bot token")
            .setAllIntents()
            .login()
            .join();
            api.updateActivity("Download Bright Eye!");

        
        // Set up an event listener for messages
         api.addMessageCreateListener(event -> {
            String content = event.getMessageContent();

            if (content.equals("!downloads")) {
                int estimatedUserCount = CognitoHelper.getEstimatedUserCount();
                event.getChannel().sendMessage("Estimated number of downloads: " + estimatedUserCount);
            }
        });
        

        //Defining a mapping of role names to descriptions
        Map<String, String> roleDescriptions = new HashMap<>();
        roleDescriptions.put("Referrer", "You've invited so many users! We appreciate your help in growing Bright Eye.");
        roleDescriptions.put("Bug Hunter", "With your help, we have squashed/are in the process of squashing so many bugs! Please continue to help us keep those bugs at bay!");
        roleDescriptions.put("Ultimate Suggester", "You've helped define and improve Bright Eye with your many suggestions. Keep up the good work!");
        roleDescriptions.put("Regular user", "You've been using the server so much. Hopefully you do the same with the app. Congratulations");
        roleDescriptions.put("Active user", "You've been using the server alot!  Hopefully you do the same with the app. Congratulations");
        roleDescriptions.put("Ultimate Suggester", "You've helped define and improve Bright Eye with your many suggestions. Keep up the good work!");
        roleDescriptions.put("Member", "Welcome to the multipurpose AI world. Welcome to Bright Eye.");
        roleDescriptions.put("Server Booster", "Thank you for boosting our server!");
        roleDescriptions.put("Moderator", "Thank you for protecting our community!");


        api.addUserRoleAddListener(event -> {
            // Retrieve the user who had a role added
            User user = event.getUser();

            // Fetch the role's description (or a default message if not found)
            String roleDescription = roleDescriptions.getOrDefault(event.getRole().getName(), "");
            
            // Send a private message to the user
            new MessageBuilder()
            .append("Hello, " + user.getDiscriminatedName() + "! You have been given the role: " + event.getRole().getName() + ". " + roleDescription)                    
            .send(user);
        });

        api.addMessageCreateListener(event -> {
            String messageContent = event.getMessageContent();

        // Check if it's a suggestion
        if (messageContent.startsWith("!suggestion ") && SUGGESTION_CHANNEL_ID.equals(event.getChannel().getIdAsString())) {
            String suggestion = messageContent.substring("!suggestion ".length());

            //Insert the suggestion into the database
            DatabaseConnectionService.insertSuggestion(suggestion, event.getMessageAuthor().getDisplayName(), event.getMessageAuthor().getAvatar().getUrl().toString());

            // Send a confirmation message to the channel
            event.getChannel().sendMessage("Thank you for your suggestion! It has been recorded.");

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
            String authorDisplayName = event.getMessageAuthor().getDisplayName();
            String avatarUrl = event.getMessageAuthor().getAvatar().getUrl().toString();

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

                // Insert the bug into the database
                DatabaseConnectionService.insertBug(bug, authorDisplayName,avatarUrl);
            });
        }
        else if (messageContent.startsWith("!postgenupdate")) {
            String updateMessage = messageContent.substring("!postgenupdate ".length());
            String formattedUpdateMessage = "📢 New Server Update 📢\n\n" + updateMessage;
            postUpdateToChannel(formattedUpdateMessage);
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
        }, 0, 24 * 60 * 60 * 1000);  // Post random messages hourly
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
            ResultSet rs = stmt.executeQuery("SELECT * FROM Suggestions");

            while (rs.next()) {
                String title = rs.getString("title");
                String suggestion = rs.getString("suggestion");
                String footer = rs.getString("footer");
                String thumbnail = rs.getString("thumbnail");

                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(suggestion)
                    .setFooter(footer)
                    .setThumbnail(thumbnail);
                channel.sendMessage(embed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessage("Error fetching suggestions.");
        }
    }

    private static void viewBugs(TextChannel channel) {
        try (Connection conn = DatabaseConnectionService.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Bugs");
            while (rs.next()) {
                String title = rs.getString("title");
                String description = rs.getString("description");
                String footer = rs.getString("footer");
                String thumbnail = rs.getString("thumbnail");
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setFooter(footer)
                    .setThumbnail(thumbnail);
            channel.sendMessage(embed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessage("Error fetching bugs.");
        }
    }
    private static void postUpdateToChannel(String updateMessage) {
        TextChannel updateChannel = api.getTextChannelById("1059965006394962021").orElse(null);
        if (updateChannel != null) {
            updateChannel.sendMessage(updateMessage);
        } else {
            System.out.println("Update channel not found.");
        }
    }    
} 