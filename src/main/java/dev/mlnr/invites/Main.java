package dev.mlnr.invites;

import net.dv8tion.jda.api.JDABuilder;

import java.util.EnumSet;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_INVITES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MEMBERS;

public class Main
{
    public static void main(final String[] args)
    {
        try
        {
            JDABuilder.create("token",
                    EnumSet.of(
                            GUILD_MEMBERS,                               // required to receive GuildMemberJoinEvent; privileged intent - you need to enable it in the application dashboard - https://discord.com/developers/applications/yourBotsId/bot
                            GUILD_INVITES                                // required to receive GuildInviteCreateEvent and GuildInviteDeleteEvent, required to cache/uncache invites
                    ))
                    .addEventListeners(new InviteTracking())             // add InviteTracking class as a listener to receive events
                    .build()
                    .awaitReady();                                       // block the thread until jda is ready
        }
        catch (final Exception ex)
        {
            System.out.println("An error occurred: " + ex.getMessage()); // would be better to replace this with a proper logger but i'll keep it like this for the sake of simplicity
        }
    }
}
