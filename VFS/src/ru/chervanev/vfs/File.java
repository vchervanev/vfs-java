package ru.chervanev.vfs;

import java.util.ArrayList;

/**
 * ���� ����������� �������� �������
 */
public class File extends AtomEdit implements Comparable<File> 
{
	// ���
	private String name;
	// ������������ ����������
	private Directory parent;
	// ���������������� ����������
	private ArrayList<String> userLockList = new ArrayList<String>();
	
	public File(Directory parent, String name) throws FileSystemException
	{
		this.name = name;
		this.parent = parent;
		this.parent.addFile(this);
	}
	
	public String getName()
	{
		return name;
	}

	@Override
	public int compareTo(File o) {
		return this.getName().compareTo(o.getName());
	}

	public boolean isLocked()
	{
		return userLockList.size()!=0;
	}
	
	/**
	 * ��������
	 */
	public void delete() throws FileSystemException 
	{
		checkLocking();
		parent.delFile(this);		
	}
	
	/**
	 * �������� �� ���������������� ����������
	 */
	private void checkLocking() throws FileSystemException 
	{
		if (isLocked())
			throw new FileSystemException(String.format("File %s is locked", getName()));	}

	/**
	 * ��������� ���������������� ���������� 
	 * @throws FileSystemException - ��� ������� ���������� ���������� ������ ��� ��������� �������
	 */
	public synchronized void lock(String user) throws FileSystemException
	{
		beginUpdate();
		try
		{
			if (!userLockList.contains(user))			
				userLockList.add(user);
			else
				throw new FileSystemException(String.format("File %s already locked by %s", getName(), user));
		}
		finally
		{
			endUpdate();
		}
	}

	/**
	 * ������ ���������������� ���������� 
	 * @throws FileSystemException - ��� ��������� �������
	 */
	
	public void unlock(String user) throws FileSystemException
	{
		beginUpdate();
		try
		{
			if (!userLockList.remove(user))
				throw new FileSystemException(String.format("File %s is not locked by %s", getName(), user));
		}
		finally
		{
			endUpdate();
		}
	}

	/**
	 * �������������� ���������� � ���������������� ����������� � ��������� ���
	 * @return
	 */
	public String lockInfo()
	{
		if (userLockList.size()==0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		sb.append("[LOCKED by ");
		for(int i=0;i<userLockList.size();i++)
		{
			if (i!=0)
				sb.append(", ");
			sb.append(userLockList.get(i));
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * ����������� ����� 
	 * @throws FileSystemException - ��� ��������� �������
	 */
	public void copy(Directory destDir) throws FileSystemException 
	{
		new File(destDir, getName());		
	}

	public void move(Directory destDir) throws FileSystemException 
	{
		checkLocking();
		destDir.addFile(this);
		parent.delFile(this);
		parent = destDir;		
	}
}
