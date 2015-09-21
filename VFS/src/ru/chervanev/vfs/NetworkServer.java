package ru.chervanev.vfs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Сетевой сервер.
 * Принимает входящие подключения и обеспечивает их совместное взаимодействие (регистрация и нотификация) 
 */
public class NetworkServer {

	// конфигурация
	private VFSProperties properties;
	// сокет
	private ServerSocket server;
	// клиенты (concurrent тип не используется намеренно, атомарность обеспечивается синхронизированными методами)
	private HashMap<String,ClientSession> clients = new HashMap<String,ClientSession>();

	/**
	 * Стартовый метод
	 */
	public static void main(String[] args) {
		NetworkServer server = new NetworkServer();
		server.Start();
	}
	
	public NetworkServer()
	{
		// загрузка конфигурации сервера (или создание файла конфигурации по умолчанию)
	    properties = new VFSProperties();
	    properties.load();

	}
	
	/**
	 * Запуск сервера
	 */
	public void Start()
	{
		try {
			// открытие сокета
			server = new ServerSocket(properties.getPort());
		} catch (Exception e) { 
			// e.g. IO or Invalid Argument Exception 
			System.err.printf("Unable to start server at port %d", properties.getPort());
			System.exit(-1);
			return;
		}			
		// запуск цикла ожидания клиентов
		acceptClients();
	}

	/**
	 * Цикл приема входящих соединений
	 */
	private void acceptClients() {
		while (true)
		{
			try {
				Socket client = server.accept();
				// запуск нового потока-сессии
				new ClientSession(this, client).start();				
				
			} catch (IOException e) {
				// nothing to do
			}
		}
	}
	
	/**
	 * Удаление сессии в случае сетевого отключения клиента
	 * @param client - сессия для удаления
	 */
	public synchronized void deleteClient(ClientSession client)
	{
		if (client.getUserName() != null && clients.remove(client.getUserName()) != null)
			notifySystem(client.getUserName() + " disconnected", client);
	}
	
	/**
	 * Метод регистрации клиентской сессии на сервере.
	 * Единственным условием регистрации является уникальность имени пользователя (регистрозависимое)
	 * Синхроннизированный метод.
	 * @param client - сессия для регистрации
	 * @throws EAlreadyUserExists - в случае нарушения уникальности имени пользователя
	 */
	public synchronized void registerUser(ClientSession client) throws EAlreadyUserExists
	{
		if (!clients.containsKey(client.getUserName()))
		{
			clients.put(client.getUserName(), client);
			notifySystem(client.getUserName() + " connected", client);
			client.notifySession(String.format("Currently connected %d user(s)", clients.size()));
		}
		else
			throw new EAlreadyUserExists(client.getUserName());
		
	}

	/**
	 * Метод нотификации всех параллельных сессий системы (кроме инициатора нотификации).
	 * Синхроннизированный метод.
	 * @param message - текст нотификации
	 * @param client - клиент, который нотифицирует остальные сессии системы
	 */
	public synchronized void notifySystem(String message, ClientSession client)
	{
		for (ClientSession session : clients.values()) {
			if (!session.equals(client))
				session.notifySession(message);
		}
	}

	// классы исключения:
	@SuppressWarnings("serial")
	public class VFSException extends Exception {
		public VFSException(String message)
		{
			super(message);
		}
	}	
	
	@SuppressWarnings("serial")
	public class EAlreadyUserExists extends VFSException 
	{ 
		public EAlreadyUserExists(String name)
		{
			super(String.format("Username \"%s\" has been used", name));
		}
	}
}
