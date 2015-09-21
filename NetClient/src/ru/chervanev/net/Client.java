package ru.chervanev.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * �����-������ �������� �������
 *
 * ������������ ������� Connect � Quit
 * ��������� ������� �������� �� ������ (���� ���������)
 * ���������� ��������� ������ �� ������ ������.
 */
public class Client {

	private volatile Socket socket = null; 
	private boolean isActive = true;
	// ������ �������
	private BufferedReader conIn = new BufferedReader(new InputStreamReader(System.in));
	// ������ �������
	private PrintWriter conOut = new PrintWriter(System.out);
	// ������ � �����
	private PrintWriter netOut;
	// ����� ������ ������
	private NetReader netReader;

	/**
	 * ��������� �����
	 */
	public static void main(String[] args) 
	{
		Client client = new Client();
		client.run();
	}

	/**
	 * �������� �����
	 */
	private void run() 
	{
		// ���� ������� ������, ���� ������������ �� ���� Quit
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
	 * ��������� ��������� �������
	 * @param command - ����� �������
	 */
	private void parseCommand(String command) 
	{
		// �������� ������� ���� �������, �������� ctrl+c
		if (command==null)
		{
			quit();
			return;
		}
		// ������� ����������� ��������� �������. ��� ��������� ���������� �� ������
		if (!processLocalCommand(command))
			processRemoteCommand(command);
	}

	/**
	 * ���������� �������� ����� ���������
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
	 * ����� ��������� ��������� ������
	 * ���������� True ���� ���������� ������� ��������� (��� ����������� �� ���������� ����������) 
	 */
	private boolean processLocalCommand(String command)
	{
		command = command.trim();
		// ��� ������� - �� ������� �������
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
				// ������ �������� Quit ����� ��������� ����, � ����� � ���
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
				// ������� �����������
				if (connect(host, port))
				{
					// ������������� �������� �� ������ ��� ������������
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
	 * ����� ���������� �������� ����������� (���� ����)
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
	 * ����� �������� ������
	 * @param server - ���� ��� ����������
	 * @param port - ����
	 * @return - true ���� ���������� ������ �������, ����� false
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
	 * ����� �������� ������� �� ������
	 * @param command - ����� �������
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
	 * ����� ������ ������ � �������
	 * @param line - ������ ��� ������
	 */
	private void println(String line)
	{
		conOut.println(line);
		conOut.flush();
	}
	
	/**
	 * ��������������� �����, ��� ������ �������� ������ ������ 
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
				// ���� ����������� ������
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
