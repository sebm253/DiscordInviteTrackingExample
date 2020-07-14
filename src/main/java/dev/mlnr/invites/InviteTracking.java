package dev.mlnr.invites;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InviteTracking extends ListenerAdapter
{
    private final Map<String, InviteData> inviteCache = new HashMap<>(); // initialize a Map for invites; key - a String, invite's code; value - InviteData object to prevent storing jda entities

    @Override
    public void onGuildInviteCreate(@NotNull final GuildInviteCreateEvent event) // gets fired when an invite is created, lets cache it
    {
        final var code = event.getCode(); // get invite's code
        final var inviteData = new InviteData(event.getInvite()); // create an InviteData object for the invite
        inviteCache.put(code, inviteData); // put code as a key and InviteData object as a value into the map; cache
    }

    @Override
    public void onGuildInviteDelete(@NotNull final GuildInviteDeleteEvent event) // gets fired when an invite is deleted, lets uncache it
    {
        final var code = event.getCode(); // get invite's code
        inviteCache.remove(code); // remove map entry based on deleted invite's code; uncache
    }

    @Override
    public void onGuildMemberJoin(@NotNull final GuildMemberJoinEvent event) // gets fired when a member joined, lets try to get the invite the member used
    {
        final var guild = event.getGuild(); // get the guild a member joined to
        final var guildId = guild.getIdLong(); // get guild's id to filter cached invites by it
        final var user = event.getUser(); // get the user who joined
        final var selfMember = guild.getSelfMember(); // get your bot's member object for this guild

        if (!selfMember.hasPermission(Permission.MANAGE_SERVER) || user.isBot()) // check if your bot doesn't have MANAGE_SERVER permission and the user who joined is a bot, if either of those is true, return
            return;

        guild.retrieveInvites().queue(retrievedInvites -> // retrieve all guild's invites, makes a request; it's necessary to have MANAGE_SERVER permission
        {
            final var storedInvitesForThisGuild = inviteCache.entrySet().stream() // stream map's entries
                                                                        .filter(entry -> entry.getValue().getGuildId() == guildId) // filter entries and their values to only collect invites for this guild
                                                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); // collect into a new map
            for (final var retrievedInvite : retrievedInvites) // iterate through retrieved invites
            {
                final var cachedInvite = storedInvitesForThisGuild.get(retrievedInvite.getCode()); // get InviteData object for this invite
                if (retrievedInvite.getUses() > cachedInvite.getUses()) // check if retrieved invite's usage count is bigger than the cached one's
                {
                    cachedInvite.incrementUses(); // increment cached invite's usage count
                    final var toLog = String.format("User %s used invite with url %s, created by %s to join.", user.getAsTag(), retrievedInvite.getUrl(), retrievedInvite.getInviter().getAsTag());
                    System.out.println(toLog); // print info about the user, invite link they probably used and the inviter (user who created the invite)
                    break; // we most likely found the correct invite, stop iterating
                }
            }
        });
    }

    @Override
    public void onGuildReady(@NotNull final GuildReadyEvent event) // gets fired when a guild gets cached, lets try to cache its invites
    {
        final var guild = event.getGuild();
        attemptInviteCaching(guild); // attempt to store guild's invites
    }

    @Override
    public void onGuildJoin(@NotNull final GuildJoinEvent event) // gets fired when your bot joined a guild, lets try to store its invites
    {
        final var guild = event.getGuild();
        attemptInviteCaching(guild); // attempt to store guild's invites
    }

    @Override
    public void onGuildLeave(@NotNull final GuildLeaveEvent event) // gets fired when your bot left a guild, uncache all invites for it
    {
        final var guildId = event.getGuild().getIdLong();
        inviteCache.entrySet().removeIf(entry -> entry.getValue().getGuildId() == guildId); // remove entry from the map if its value's guild id is the one your bot has left
    }

    private void attemptInviteCaching(final Guild guild) // helper method to prevent duplicate code for GuildReadyEvent and GuildJoinEvent
    {
        final var selfMember = guild.getSelfMember();

        if (selfMember.hasPermission(Permission.MANAGE_SERVER)) // check if your bot has MANAGE_SERVER permission to retrieve the invites
            guild.retrieveInvites().queue(retrievedInvites ->
                  retrievedInvites.forEach(retrievedInvite -> inviteCache.put(retrievedInvite.getCode(), new InviteData(retrievedInvite)))); // iterate through invites and store them
    }
}