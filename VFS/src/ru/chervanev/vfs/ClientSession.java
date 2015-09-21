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
 * ���������������� ������ ��� �������� �������.
 * 
 *  ��������� ����� ������� �� �������� ���������� � ��������� �������� �� �� ����������
 *  ��������� ������ ������������� ������������, ����������� ����� � ����������� ��������� ������ (ISession)
 */
public class ClientSession extends Thread implements ISession{
	
	private Socket socket;
	private NetworkServer server;
	//������ ������:
	private InputStream in;	
	private OutputStream out;
	
	// ��������/�������� ������:
	private PrintWriter writer;
	private BufferedReader reader;
	//��� ������������
	private String userName;
	// ������� ����������� ������
	private VFSExecuterAdapter executer;
	
	public ClientSession(NetworkServer server, Socket socket)
	{
		this.server = server;
		this.socket = socket;
	}
	
	/**
	 * ���� ������-������
	 */
	public void run()
	{
		try {
			// �������������
			in = socket.getInputStream();
			out = socket.getOutputStream();
			writer = new PrintWriter(out);
			reader = new BufferedReader(new InputStreamReader(in));
			executer = new VFSExecuterAdapter(this);
		} catch (Exception e) {
			// �� ����� ������ ����������� ������ ������ ����������
			System.err.print(e.getMessage());
			return;
		}
		// ������ ������ ������������, ���� ������ ������ ���������� ���:
		if (registerSession())
		{
			// ���� �������� ������
			processCommands();
			//���������� ������ ������
			close();
		}
		
	}

	/**
	 * ����� ���� �������� ������
	 */
	private void processCommands() 
	{
		// ����
		while (socket.isConnected())
		{
			String command;
			try
			{
				// ������ �������
				command = reader.readLine();
				if (command==null)
					return;
			}catch(Exception e)
			{
				return;
			}
			
			try {
				// ���������� �������
				executer.execute(command);
			} catch (InvocationTargetException e) {
				notifySession(e.getTargetException().getMessage());
			} catch (Exception e) {
				notifySession(e.getMessage());
			}

		}		
	}

	/**
	 * ����� ���������� ����� ������������ ������� � ������� ���������������� ��� �� �������
	 * @return true ���� ����������� ������ �������
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
	 * ����� �������� ������ ��� ���������� ������ ������
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
	 * ����� ����������� ������
	 */
	@Override
	public void notifySession(String message)
	{
		writer.println(message);
		writer.flush();
		// ���-�� ���� ��������� � ��, ��������� ��������:
		executer.checkDeletion();		
	}

	/**
	 * ����� ����������� ������������ ������
	 */
	@Override
	public void notifySystem(String message) 
	{
		server.notifySystem(message, this);	
	}
}