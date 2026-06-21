package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

public class NeosCompanionBot extends TelegramLongPollingBot {

    private final Map<Long, String> userState = new HashMap<>();

    // REPLACE WITH YOUR NUMERIC ID (e.g., "12345678")
    private final String ADMIN_CHAT_ID = System.getenv("ADMIN_ID");
    private final String CHANNEL_URL = "https://t.me/neos_terminal";

    @Override
    public String getBotUsername() {
        return "NeosCompanionBot";
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String username = (update.getMessage().getFrom().getUserName() != null)
                    ? update.getMessage().getFrom().getUserName()
                    : "friend";

            // 1. ADMIN REPLY
            if (String.valueOf(chatId).equals(ADMIN_CHAT_ID) && messageText.startsWith("/reply")) {
                handleAdminReply(messageText);
                return;
            }

            // 2. USER STATE HANDLING
            if (userState.containsKey(chatId)) {
                handleStateAction(chatId, messageText, username);
                return;
            }

            // 3. MAIN COMMANDS
            switch (messageText.split(" ")[0].toLowerCase()) {
                case "/start":
                    sendResponse(chatId, "<b>Hello " + username + "! 👋</b>\n\nWelcome to the <b>Neo's Terminal Companion</b>. I am here to ensure you have the best experience possible with our tools.\n\nHow can I assist you today?\n\n<i>Type /help to see all available options.</i>");
                    break;

                case "/suggest":
                    userState.put(chatId, "SUGGESTION");
                    sendResponse(chatId, "💡 <b>We value your ideas!</b>\n\nPlease type your suggestion below. Whether it's a new feature or a small improvement, we'd love to hear how we can make Neo's Terminal better for you.");
                    break;

                case "/bug":
                    userState.put(chatId, "BUG");
                    sendResponse(chatId, "🪲 <b>Reporting a Bug</b>\n\nI'm sorry to hear you've encountered an issue. Please describe what happened in as much detail as possible so our developers can investigate it immediately.");
                    break;

                case "/contact":
                    userState.put(chatId, "CONTACT");
                    sendResponse(chatId, "📩 <b>Get in Touch</b>\n\nYour message will be sent directly to our administrator. Please type your message below, and we will get back to you as soon as we can.");
                    break;

                case "/about":
                    sendResponse(chatId, "🤖 <b>About This Bot</b>\n\nThis is the official <b>Neo's Companion Bot</b>, designed to bridge the gap between our users and the development team.\n\nMy mission is to help you report issues, share creative ideas, and keep you updated on the evolution of Neo's Terminal. Built with ❤️ for the community.");
                    break;

                case "/channel":
                    String channelInfo = "📢 <b>Neo's Terminal Community</b>\n\nJoin our official channel to receive real-time updates, patch notes, and exclusive insights into new features!\n\n✨ <b>Join us here:</b> " + CHANNEL_URL;
                    sendResponse(chatId, channelInfo);
                    break;

                case "/help":
                    String helpMsg = "✨ <b>Available Commands:</b>\n\n" +
                            "🚀 /start - Restart the bot\n" +
                            "💡 /suggest - Share an idea\n" +
                            "🪲 /bug - Report an issue\n" +
                            "📩 /contact - Talk to Admin\n" +
                            "ℹ️ /about - Bot information\n" +
                            "📢 /channel - Join the community";
                    sendResponse(chatId, helpMsg);
                    break;

                default:
                    sendResponse(chatId, "I'm sorry, I didn't quite catch that. 🤔\n\nCould you please use one of the commands from the /help menu?");
                    break;
            }
        }
    }

    private void handleStateAction(long chatId, String text, String username) {
        String type = userState.get(chatId);
        String cleanText = text.replace("<", "&lt;").replace(">", "&gt;");

        String adminMsg = "🔔 <b>New Message Received</b>\n" +
                "<b>Category:</b> " + type + "\n" +
                "<b>From:</b> @" + username + " (<code>" + chatId + "</code>)\n\n" +
                "<b>Content:</b> " + cleanText + "\n\n" +
                "👉 <i>To reply, use:</i> <code>/reply " + chatId + " [text]</code>";

        if (forwardToAdmin(adminMsg)) {
            sendResponse(chatId, "<b>Thank you!</b> ✨\n\nYour " + type.toLowerCase() + " has been successfully delivered. We appreciate your contribution to Neo's Terminal!");
        } else {
            sendResponse(chatId, "❌ <b>Notice:</b> I was unable to reach the administrator at this moment. Please try again in a few minutes.");
        }

        userState.remove(chatId);
    }

    private void handleAdminReply(String fullText) {
        try {
            String[] parts = fullText.split(" ", 3);
            if (parts.length < 3) {
                sendResponse(Long.parseLong(ADMIN_CHAT_ID), "⚠️ <b>Usage:</b> <code>/reply [ID] [Message]</code>");
                return;
            }
            long targetUser = Long.parseLong(parts[1]);
            String replyBody = parts[2];

            sendResponse(targetUser, "💬 <b>Message from Neo's Admin:</b>\n\n" + replyBody);
            sendResponse(Long.parseLong(ADMIN_CHAT_ID), "✅ <b>Reply successfully sent!</b>");
        } catch (Exception e) {
            sendResponse(Long.parseLong(ADMIN_CHAT_ID), "❌ <b>Error:</b> Could not send message. Verify the User ID.");
        }
    }

    private boolean forwardToAdmin(String text) {
        SendMessage message = new SendMessage();
        message.setChatId(ADMIN_CHAT_ID);
        message.setText(text);
        message.setParseMode("HTML");
        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            return false;
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        message.setDisableWebPagePreview(false); // Allows channel link preview
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}