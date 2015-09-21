package ru.chervanev.vfs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import ru.chervanev.vfs.ISession;
/**
 * ѕользовательска€ сесси€ дл€ сетевого сервера.
 * 
 *  —читывает любые команды по сетевому соединению и прозрачно передает их на выполнение
 *  –еализует методы идентификации пользовател€, нотификации сесии и нотификации остальных сессий (ISession)
 */
public class ClientSession extends Thread implements ISession{
	
	private Socket socket;
	private NetworkServer server;
	//стримы сокета:
	private InputStream in;	
	private OutputStream out;
	
	// писатель/читатель сокета:
	private PrintWriter writer;
	private BufferedReader reader;
	//им€ пользовател€
	private String userName;
	// адаптер исполнител€ команд
	private VFSExecuterAdapter executer;
	
	public ClientSession(NetworkServer server, Socket socket)
	{
		this.server = server;
		this.socket = socket;
	}
	
	/**
	 * “ело потока-сессии
	 */
	public void run()
	{
		try {
			// инициализаци€
			in = socket.getInputStream();
			out = socket.getOutputStream();
			writer = new PrintWriter(out);
			reader = new BufferedReader(new InputStreamReader(in));
			executer = new VFSExecuterAdapter(this);
		} catch (Exception e) {
			// по любой ошибке продолжение работы потока невозможно
			System.err.print(e.getMessage());
			return;
		}
		// работа потока продолжаетс€, если клиент введет уникальное им€:
		if (registerSession())
		{
			// цикл обаботки команд
			processCommands();
			//завершение работы потока
			close();
		}
		
	}

	/**
	 * ћетод цикл обаботки команд
	 */
	private void processCommands() 
	{
		// цикл
		while (socket.isConnected())
		{
			String command;
			try
			{
				// чтение команды
				command = reader.readLine();
				if (command==null)
					return;
			}catch(Exception e)
			{
				return;
			}
			
			try {
				// выполнение команды
				executer.execute(command);
			} catch (InvocationTargetException e) {
				notifySession(e.getTargetException().getMessage());
			} catch (Exception e) {
				notifySession(e.getMessage());
			}

		}		
	}

	/**
	 * ћетод считывани€ имени пользовател€ клиента и попытка зарегистрировать его на сервере
	 * @return true если регистраци€ прошла успешно
	 */
	private boolean registerSession() 
	{
		try {
			userName = reader.readLine();
			server.registerUser(this);
		} catch (NetworkServer.EAlreadyUserExists e) {
			userName = null;
			notifySession(e.getMessage());
			close();
			return false;
		} catch (IOException e) {
			close();
			return false;
		}
		return true;
	}

	/**
	 * ћетод закрыти€ сокета дл€ завершени€ работы потока
	 */
	private void close()
	{
		if (socket.isConnected())
			try {
				socket.close();
			} catch (IOException e) {}
		server.deleteClient(this);
	}

	@Override
	public String getUserName() 
	{
		return userName;
	}

	/**
	 * ћетод нотификации сессии
	 */
	@Override
	public void notifySession(String message)
	{
		writer.println(message);
		writer.flush();
		// кто-то внес изменени€ в ‘—, требуетс€ проверка:
		executer.checkDeletion();		
	}

	/**
	 * ћетод нотификации параллельных сессий
	 */
	@Override
	public void notifySystem(String message) 
	{
		server.notifySystem(message, this);	
	}
}