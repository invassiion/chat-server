package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private List<ClientHandler> clients;
    private PrintWriter out;
    private String userName;

    public ClientHandler(List<ClientHandler> clients, Socket clientSocket) {
        this.clients = clients;
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        try (
              BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
              PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            this.out = out;
            out.println("Введите ваше имя:");
            this.userName = in.readLine();
            broadcastMessage("Пользователь " + userName + " присоединился к чату!");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/")) {
                    handlecommand(message);
                } else {
                    System.out.println(userName + ": " + message);
                    broadcastMessage(userName + ": " + message);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка в обработке клиента: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handlecommand(String message) {
        if (message.equalsIgnoreCase("/exit")) {
            closeConnection();
        } else if (message.equalsIgnoreCase("/list")) {
            sendMessage("Список пользователей:");
            for (ClientHandler client : clients) {
                sendMessage(client.userName);
            }
        } else if (message.startsWith("/name ")) {
            String newName = message.substring(6);
            if (!newName.isEmpty()) {
                broadcastMessage(userName + " изменил имя на " + newName);
                userName = newName;
            } else {
                sendMessage("Некорректное имя.");
            }
        } else {
            sendMessage("Неизвестная команда: " + message);
        }

    }

    private void sendClientList() {
        out.println("Подключенные пользователи:");
        for (ClientHandler client : clients) {
            if (client != this) {
                out.println(client.clientSocket.getRemoteSocketAddress());
            }
        }
    }

    private void closeConnection() {
        try {
            clients.remove(this);
            broadcastMessage("Пользователь " + userName + " покинул чат.");
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            if (client != this) {
                client.sendMessage(message);
            }
        }
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}



