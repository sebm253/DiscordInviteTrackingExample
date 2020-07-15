package dev.mlnr.invites;

import net.dv8tion.jda.api.entities.Invite;

public class InviteData // this object is useful to prevent storing jda entities - check https://github.com/DV8FromTheWorld/JDA#entity-lifetimes
{
    private final long guildId; // store guild's id
    private int uses; // store invite's usage count

    public InviteData(final Invite invite)
    {
        this.guildId = invite.getGuild().getIdLong();
        this.uses = invite.getUses();
    }

    public long getGuildId()
    {
        return guildId;
    }

    public int getUses()
    {
        return uses;
    }

    public void incrementUses() // increment stored invite's usage count
    {
        this.uses++;
    }
}