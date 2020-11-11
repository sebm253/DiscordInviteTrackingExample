package dev.mlnr.invites;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InviteTracking extends ListenerAdapter
{
    private final Map<String, InviteData> inviteCache = new ConcurrentHashMap<>();                    // initialize a thread safe Map for invites; key - a String, invite's code; value - InviteData object to prevent storing jda entities

    // keep in mind that invite events will only fire for channels in which your bot has MANAGE_CHANNEL perm
    @Override
    public void onGuildInviteCreate(final GuildInviteCreateEvent event)                               // gets fired when an invite is created, lets cache it
    {
        final String code = event.getCode();                                                          // get invite's code
        final InviteData inviteData = new InviteData(event.getInvite());                              // create an InviteData object for the invite
        inviteCache.put(code, inviteData);                                                            // put code as a key and InviteData object as a value into the map; cache
    }

    @Override
    public void onGuildInviteDelete(final GuildInviteDeleteEvent event)                               // gets fired when an invite is deleted, lets uncache it
    {
        final String code = event.getCode();                                                          // get invite's code
        inviteCache.remove(code);                                                                     // remove map entry based on deleted invite's code; uncache
    }

    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event)                                   // gets fired when a member joined, lets try to get the invite the member used
    {
        final Guild guild = event.getGuild();                                                         // get the guild a member joined to
        final User user = event.getUser();                                                            // get the user who joined
        final Member selfMember = guild.getSelfMember();                                              // get your bot's member object for this guild

        if (!selfMember.hasPermission(Permission.MANAGE_SERVER) || user.isBot())                      // check if your bot doesn't have MANAGE_SERVER permission and the user who joined is a bot, if either of those is true, return
            return;

        guild.retrieveInvites().queue(retrievedInvites ->                                             // retrieve all guild's invites
        {
            for (final Invite retrievedInvite : retrievedInvites)                                     // iterate through retrieved invites
            {
                final String code = retrievedInvite.getCode();                                        // get currently iterated Invite's code
                final InviteData cachedInvite = inviteCache.get(code);                                // get InviteData object for this invite
                if (cachedInvite == null)                                                             // check for invites that are not in cache, probably one-use invites or idk
                    continue;
                if (retrievedInvite.getUses() == cachedInvite.getUses())                              // check if retrieved invite's usage count is equal to the cached one's, if true, continue looping
                    continue;
                cachedInvite.incrementUses();                                                         // increment cached invite's usage count
                final String pattern = "User %s used invite with url %s, created by %s to join.";     // create a "pattern" for the string to print
                final String tag = user.getAsTag();                                                   // get user's tag, as like Name#Discriminator, eg /home/canelex_#6666
                final String url = retrievedInvite.getUrl();                                          // get invite's url
                final String inviterTag = retrievedInvite.getInviter().getAsTag();                    // get inviter's tag (inviter = the user that created the invite)
                final String toLog = String.format(pattern, tag, url, inviterTag);                    // format the pattern with variables
                System.out.println(toLog);                                                            // print info about the user, invite link they probably used and the inviter
                break;                                                                                // we most likely found the correct invite, stop iterating
            }
        });
    }

    @Override
    public void onGuildReady(final GuildReadyEvent event)                                             // gets fired when a guild gets cached, lets try to cache its invites
    {
        final Guild guild = event.getGuild();                                                         // get the guild that finished setting up
        attemptInviteCaching(guild);                                                                  // attempt to store guild's invites
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event)                                               // gets fired when your bot joined a guild, lets try to store its invites
    {
        final Guild guild = event.getGuild();                                                         // get the guild that finished setting up
        attemptInviteCaching(guild);                                                                  // attempt to store guild's invites
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event)                                             // gets fired when your bot left a guild, uncache all invites for it
    {
        final long guildId = event.getGuild().getIdLong();                                            // get the id of the guild your bot left
        inviteCache.entrySet().removeIf(entry -> entry.getValue().getGuildId() == guildId);           // remove entry from the map if its value's guild id is the one your bot has left
    }

    private void attemptInviteCaching(final Guild guild)                                              // helper method to prevent duplicate code for GuildReadyEvent and GuildJoinEvent
    {
        final Member selfMember = guild.getSelfMember();                                              // get your bot's member object for this guild

        if (!selfMember.hasPermission(Permission.MANAGE_SERVER))                                      // check if your bot doesn't have MANAGE_SERVER permission to retrieve the invites, if true, return
            return;

        guild.retrieveInvites().queue(retrievedInvites ->                                             // retrieve all guild's invites
        {
            retrievedInvites.forEach(retrievedInvite ->                                               // iterate over invites..
                    inviteCache.put(retrievedInvite.getCode(), new InviteData(retrievedInvite)));     // and store them
        });
    }
}
