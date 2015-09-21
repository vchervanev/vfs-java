package ru.chervanev.vfs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * ������� ������.
 * ��������� �������� ����������� � ������������ �� ���������� �������������� (����������� � �����������) 
 */
public class NetworkServer {

	// ������������
	private VFSProperties properties;
	// �����
	private ServerSocket server;
	// ������� (concurrent ��� �� ������������ ���������, ����������� �������������� ������������������� ��������)
	private HashMap<String,ClientSession> clients = new HashMap<String,ClientSession>();

	/**
	 * ��������� �����
	 */
	public static void main(String[] args) {
		NetworkServer server = new NetworkServer();
		server.Start();
	}
	
	public NetworkServer()
	{
		// �������� ������������ ������� (��� �������� ����� ������������ �� ���������)
	    properties = new VFSProperties();
	    properties.load();

	}
	
	/**
	 * ������ �������
	 */
	public void Start()
	{
		try {
			// �������� ������
			server = new ServerSocket(properties.getPort());
		} catch (Exception e) { 
			// e.g. IO or Invalid Argument Exception 
			System.err.printf("Unable to start server at port %d", properties.getPort());
			System.exit(-1);
			return;
		}			
		// ������ ����� �������� ��������
		acceptClients();
	}

	/**
	 * ���� ������ �������� ����������
	 */
	private void acceptClients() {
		while (true)
		{
			try {
				Socket client = server.accept();
				// ������ ������ ������-������
				new ClientSession(this, client).start();				
				
			} catch (IOException e) {
				// nothing to do
			}
		}
	}
	
	/**
	 * �������� ������ � ������ �������� ���������� �������
	 * @param client - ������ ��� ��������
	 */
	public synchronized void deleteClient(ClientSession client)
	{
		if (client.getUserName() != null && clients.remove(client.getUserName()) != null)
			notifySystem(client.getUserName() + " disconnected", client);
	}
	
	/**
	 * ����� ����������� ���������� ������ �� �������.
	 * ������������ �������� ����������� �������� ������������ ����� ������������ (�����������������)
	 * ������������������� �����.
	 * @param client - ������ ��� �����������
	 * @throws EAlreadyUserExists - � ������ ��������� ������������ ����� ������������
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
	 * ����� ����������� ���� ������������ ������ ������� (����� ���������� �����������).
	 * ������������������� �����.
	 * @param message - ����� �����������
	 * @param client - ������, ������� ������������ ��������� ������ �������
	 */
	public synchronized void notifySystem(String message, ClientSession client)
	{
		for (ClientSession session : clients.values()) {
			if (!session.equals(client))
				session.notifySession(message);
		}
	}

	// ������ ����������:
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
