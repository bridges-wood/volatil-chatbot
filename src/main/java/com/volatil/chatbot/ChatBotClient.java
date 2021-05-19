package com.volatil.chatbot;

import java.net.Socket;

import com.volatil.core.client.Client;
import com.volatil.core.client.bot.BotReceiver;
import com.volatil.core.client.bot.BotTransmitter;
import com.volatil.core.utils.ExitHandler;
import com.volatil.core.utils.Logger;
import com.volatil.core.utils.Message;
import com.volatil.core.utils.MessageQueue;

public class ChatBotClient extends Client {
  private final String[] GREETINGS = { "hello", "hi", "hey", "yo" };
  private MessageQueue<String> messages = new MessageQueue<String>();
  private BotReceiver incoming;
  private BotTransmitter outgoing;
  private ExitHandler ex = new ExitHandler();
  private Logger log;

  public ChatBotClient(String[] args) {
    super(args);
    Socket serverSocket = getServerSocket();
    incoming = new BotReceiver(serverSocket, messages, log);
    outgoing = new ChatBotTransmitter(serverSocket, messages);
    this.log = log();
  }

  private class ChatBotTransmitter extends BotTransmitter {
    public ChatBotTransmitter(Socket serverSocket, MessageQueue<String> input) {
      super(serverSocket, input);
    }

    @Override
    protected String generateResponse(String message) {
      String origin = Message.extractOrigin(message);
      if (isGreeting(message))
        return "Hey, " + origin + ".";
      return null;
    }

    private boolean isGreeting(String message) {
      String formattedMessage = message.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "");
      String[] words = formattedMessage.split(" ");
      for (String word : words) {
        for (String greeting : GREETINGS) {
          if (word.equals(greeting))
            return true;
        }
      }
      return false;
    }
  }

  @Override
  public void start() {
    log.info("Started.");
    outgoing.start();
    incoming.start();
    ex.start();
    while (true) {
      if (!outgoing.isAlive() || !incoming.isAlive() || !ex.isAlive())
        break;
    }
    cleanup();
  }

  @Override
  public void cleanup() {
    try {
      outgoing.interrupt();
      outgoing.join();
      incoming.interrupt();
      incoming.join();
      ex.interrupt();
      ex.join();
    } catch (InterruptedException e) {
      super.cleanup();
      log.error(e.getMessage());
    }
  }

  public static void main(String[] args) {
    ChatBotClient client = new ChatBotClient(args);
    client.start();
  }
}
