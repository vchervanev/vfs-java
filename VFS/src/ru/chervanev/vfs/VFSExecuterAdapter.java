package ru.chervanev.vfs;

import java.util.ArrayList;
import java.util.Collections;

/**
 * ������� Executer ��� ������� ��������� ������ �� ����� ���������� IFileSystem
 * Run-time �������� MemoryVFS ��������� � ����������� ������������ ����������� ��������  
 * 
 * ������������ �� MemoryVFS �� ������������, ��� ������� ����������.
 * �������������� ����������� - �������� ������������� MemoryVFSProxy ��� ���������� � ���������.
 */
public class VFSExecuterAdapter extends Executer
{
	private ISession session;
	private IFileSystem fileSystem;
	private Directory currentDir;
	
	public VFSExecuterAdapter(ISession session) throws Exception
	{
		super.object = this;
		this.session = session;
		
		try {
			// �������� � ������� ���������� ��� �� �����
			fileSystem = (IFileSystem)Class.forName("ru.chervanev.vfs.MemoryVFS").getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
			// ������� ������� �� ���������
			currentDir = fileSystem.listFileSystem().findDir("C:");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new Exception("Unable to instantiate File System ");			
		}
	}
	
	/**
	 * ��������� ���������� ������.
	 * 
	 * ���������� ����������� ��� ��� ������: �������� �� �������� ������� ���������� ����� � �����������
	 * 
	 */
	public void execute(String command) throws Exception
	{
		checkDeletion();
		super.execute(command);
		session.notifySystem(String.format("User %s performs command: %s", session.getUserName(), command));
	}
	
	/**
	 * ��������, �� ���� �� ������� ���������� ������� ������ ������������� 
	 */
	public void checkDeletion() {
		if (currentDir.isDeleted())
		{
			try {
				currentDir = fileSystem.listFileSystem().findDir("c:");
				session.notifySession("Current directory was deleted by another user. Current directory is c:\\");				
			} catch (FileSystemException e) {
				// nothing to do
			}
		}
	}
		

	/**
	 * �������� ��������
	 * 
	 *  �������������� ��������, ���������� �� ������� ���������� ������
	 */
	public void rd(String dirName) throws FileSystemException 
	{
		String fullDirName = formatName(dirName);
		if (currentDir.getFullName().toLowerCase().equals(fullDirName.toLowerCase()))
			throw new FileSystemException(String.format("Can not delete current directory: %s", dirName));

		fileSystem.rd(fullDirName);
	}

	/**
	 * �������� �������� ���������
	 * 
	 *  �������������� ��������, ���������� �� ������� ���������� ������
	 */
	public void delTree(String dirName) throws FileSystemException 
	{
		String fullDirName = formatName(dirName);
		if (currentDir.getFullName().toLowerCase().equals(fullDirName.toLowerCase()))
			throw new FileSystemException(String.format("Can not delete current directory: %s", dirName));
		if (currentDir.getFullName().toLowerCase().startsWith(fullDirName.toLowerCase()))
			throw new FileSystemException(String.format("Cannot delete parent of current directory: %s", dirName));
		
		fileSystem.delTree(fullDirName);
	}
	
	// ������ ��������� ������������ � �������������� ���� ����������� ����� ����������/������ ���� �������������, ������������ ������� ���������� ������
	
	public void md(String dirName) throws FileSystemException {
		fileSystem.md(formatName(dirName));
	}

	public void cd(String dirName) throws FileSystemException {
		
		currentDir = fileSystem.listFileSystem().findDir(formatName(dirName));		
	}
	
	public void mf(String fileName) throws FileSystemException 
	{
		fileSystem.mf(formatName(fileName));
	}
	
	public void del(String fileName) throws FileSystemException 
	{
		fileSystem.del(formatName(fileName));
	}
	
	public void lock(String fileName) throws FileSystemException 
	{
		fileSystem.lock(formatName(fileName), session.getUserName());
	}
	
	public void unlock(String fileName) throws FileSystemException 
	{
		fileSystem.unlock(formatName(fileName), session.getUserName());
	}
	
	public void copy(String source, String fileName) throws FileSystemException 
	{
		fileSystem.copy(formatName(source), formatName(fileName));		
	}
	
	public void move(String source, String fileName) throws FileSystemException 
	{
		fileSystem.move(formatName(source), formatName(fileName));
	}
		
	/**
	 * ����� �� ����� ��������� ���������
	 */
	public void print()
	{
		StringBuilder sb = new StringBuilder();
		for(Directory drive : fileSystem.listFileSystem().dirCollection())
		{
			buildString(sb, "", "", drive);
		}		
		session.notifySession(sb.toString());
	}
	
	/**
	 * ����������� ����� ���������� ��������� ���������� ��������������
	 * 
	 * ��� ������ ���������� � ����� ����������� ����������
	 * 
	 * @param sb - StringBuilder
	 * @param prefix1 - ������� ��� ����������� � �������� ������
	 * @param prefix2 - ������� ��� ����������� � "������" - ����� ������� � prefix1 �� ����������� ������
	 * @param dir - ������� ������� ��� ������������ ��������
	 */
	private void buildString(StringBuilder sb, String prefix1, String prefix2,Directory dir)
	{
		sb.append(prefix1);
		sb.append(dir.getName());
		sb.append(dir.isEditing()?" [!]":"");
		sb.append("\n");
		ArrayList<Directory> list = new ArrayList<Directory>(dir.dirCollection());
		Collections.sort(list);
		
		ArrayList<File> files = new ArrayList<File>(dir.fileCollection());
		Collections.sort(files);
		
		for(int i=0;i<list.size();i++)
		{
			if (i<list.size()-1 || files.size()!=0)
				buildString(sb, prefix2 + "|_", prefix2 + "| ", list.get(i));
			else
				buildString(sb, prefix2 + "|_", prefix2 + "  ", list.get(i));
		}	
		
		for(int i=0;i<files.size();i++)
		{
			sb.append(prefix2);
			sb.append("|_");
			sb.append(files.get(i).getName());
			sb.append(files.get(i).lockInfo());
			sb.append(files.get(i).isEditing()?" [!]":"");
			sb.append("\n");						
		}
	}

	/**
	 * ���������� ����� ����� ��� �������� �� �����������
	 * @param dirOrFileName - ��� ����� ��� ��������, ��������� ������������� 
	 * @return ���������� ��� ����� ��� �������� (������������ �� �����������, ������ ������� �: � ������ �����)
	 */
	public String formatName(String dirOrFileName)
	{
		if (dirOrFileName.length()<2 || dirOrFileName.charAt(1)!=':')
		{
			return currentDir.getFullName() + "\\" + dirOrFileName;
		} else
			return dirOrFileName;
	}
	
	/**
	 * ����� ��� ������������ � ����������� ���������� ���������� ������� 
	 */
	public void test(String arg) throws FileSystemException
	{
		if (arg.equals("cd"))
		{
			session.notifySession(currentDir.getFullName());
		} else if (arg.equals("user"))
		{
			session.notifySession(session.getUserName());
		} else if (arg.startsWith("dl\\"))
		{
			fileSystem.listFileSystem().findDir(formatName(arg.substring(3))).beginUpdate();
			session.notifySession(String.format("%s editing is started", arg.substring(3)));
		} else if (arg.startsWith("fl\\"))
		{
			fileSystem.listFileSystem().findFile(formatName(arg.substring(3))).beginUpdate();
			session.notifySession(String.format("%s editing is started", arg.substring(3)));
		} else if (arg.startsWith("ul"))
		{
			fileSystem.listFileSystem().endUpdateResursive();
			session.notifySession(String.format("All editings are finished"));
		} else if (arg.startsWith("rl\\"))
		{
			fileSystem.listFileSystem().findDir(formatName(arg.substring(3))).beginUpdate();
			session.notifySession(String.format("%s recursive editing is started", arg.substring(3)));
		} else session.notifySession("Use test:\n\tcd\n\tuser\n\tfl\\filename\n\tdl\\dirname\n\trl\\dirname\n\tul");
	}

}
