package com.brenbrit.brenbot.listeners;

import java.util.Dictionary;
import java.util.Hashtable;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

    private Dictionary<String, String> basicTextCommands;

    public CommandListener() {
        basicTextCommands = new Hashtable<String, String>();
        System.out.println("CommandListener initialized.");
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        System.out.println("Slash command event!");

        // Was the command a basic text command?
        if (basicTextCommands.get(event.getName()) != null) {
            System.out.println("Command received!");
//            event.getTextChannel().
//                sendMessage(basicTextCommands.get(event.getName())).queue();

            // reply or acknowledge
            event.reply(basicTextCommands.get(event.getName()))
                .setEphemeral(false).queue();
        }
    }

    // Function which simply adds basic text command to the Hashtable
    // this.basicTextCommands.
    public void addBasicTextCommand(String command, String output) {
        basicTextCommands.put(command, output);
    }

}