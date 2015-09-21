package ru.chervanev.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *  ласс-клиент сетевого сервера
 *
 * ќбрабатывает команды Connect и Quit
 * ќстальные команды отсылает на сервер (если подключен)
 * јсинхронно считывает данные из стрима сокета.
 */
public class Client {

	private volatile Socket socket = null; 
	private boolean isActive = true;
	// чтение консоли
	private BufferedReader conIn = new BufferedReader(new InputStreamReader(System.in));
	// запись консоли
	private PrintWriter conOut = new PrintWriter(System.out);
	// запись в сокет
	private PrintWriter netOut;
	// поток чтени€ сокета
	private NetReader netReader;

	/**
	 * —тартовый метод
	 */
	public static void main(String[] args) 
	{
		Client client = new Client();
		client.run();
	}

	/**
	 * ќсновной метод
	 */
	private void run() 
	{
		// ÷икл разбора команд, пока пользователь не ввел Quit
		while(isActive())
		{
			try {
				parseCommand(conIn.readLine());
			} catch (IOException e) {
				return;
			}			
		}		
	}

	/**
	 * ќбработка отдельной команды
	 * @param command - текст команды
	 */
	private void parseCommand(String command) 
	{
		// возможно консоль была закрыта, например ctrl+c
		if (command==null)
		{
			quit();
			return;
		}
		// сначала провер€ютс€ системные команды. все остальные отсылаютс€ на сервер
		if (!processLocalCommand(command))
			processRemoteCommand(command);
	}

	/**
	 * «авершение рабочего цикла программы
	 */
	private void quit() 
	{
		isActive = false;
		disconnect();
	}

	private boolean isActive()
	{
		return isActive;
	}
	
	/**
	 * ћетод обработки системных команд
	 * ¬озвращает True если переданна€ команда системна€ (вне зависимости от результата выполнени€) 
	 */
	private boolean processLocalCommand(String command)
	{
		command = command.trim();
		// им€ команды - до первого пробела
		String cmdName = command.split(" ")[0].toLowerCase();
		String cmdParam = command.substring(cmdName.length(), command.length()).trim();
		if (cmdName.equals("quit"))
		{
			quit();
			return true;
		} else if (cmdName.equals("connect"))
		{
			String[] params = cmdParam.split(" ");
			if (params.length==2)
			{
				// первый параметр Quit может содержать порт, а может и нет
				int i = params[0].indexOf(':');
				int port;
				String host;
				if (i==-1)
				{
					port = 8123;
					host = params[0];
				} else {
					try
					{
						port = Integer.parseInt(params[0].substring(i+1));
						host = params[0].substring(0, i);
					}catch(NumberFormatException e)
					{
						println("Invalid port number");
						return true;
					}
				}
				// ѕопытка соединитьс€
				if (connect(host, port))
				{
					// автоматически отсылаем на сервер им€ пользовател€
					netOut.println(params[1].trim());
					netOut.flush();
				}

			}else				
				println("Use command connect:\nconnect host[:port] UserName");
				
			return true;			
		}	
		return false;
	}

	/**
	 * ћетод завершени€ сетевого подключени€ (если есть)
	 */
	private void disconnect() {
		try {
			if (socket!=null && socket.isConnected())
			{
				socket.close();
				socket = null;
				try {
					netReader.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// nothing to do
		}
	}

	/**
	 * ћетод открыти€ сокета
	 * @param server - хост дл€ соединени€
	 * @param port - порт
	 * @return - true если соединение прошло успешно, иначе false
	 */
	private boolean connect(String server, int port)
	{
		try {
			disconnect();
			socket = new Socket(server, port);
			netOut = new PrintWriter(socket.getOutputStream());
			netReader = new NetReader(socket);
			return true;
		} catch (IOException e) {
			socket = null;
			netOut = null;
			println(String.format("Cannot connect %s:%d", server, port));
			return false;
		}
	}
	
	/**
	 * ћетод отправки команды на сервер
	 * @param command - текст команды
	 */
	private void processRemoteCommand(String command)
	{
		if (socket != null)
		{
			netOut.println(command);
			netOut.flush();			
		}
		else 
		{
			println("Cannot invoke command in offline mode.");
		}
	}
	
	/**
	 * ћетод вывода данных в консоль
	 * @param line - строка дл€ вывода
	 */
	private void println(String line)
	{
		conOut.println(line);
		conOut.flush();
	}
	
	/**
	 * ¬спомогательный поток, дл€ чтени€ вход€щих данных стрима 
	 *
	 */
	class NetReader extends Thread
	{
		BufferedReader reader;
		public NetReader(Socket socket) throws IOException
		{
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			start();
		}
		
		public void run()
		{
			try
			{
				// цикл построчного чтени€
				while (socket!=null && socket.isConnected())
				{
					String line = reader.readLine();
					if (line==null)
					{
						break;
					}
					println(line);				
				}
				if (socket!=null)
					disconnect();
			}catch(IOException x)
			{
				println("Disconnected");
				if (socket!=null)
					disconnect();
			}
		}
	}
}
