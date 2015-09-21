package ru.chervanev.vfs;

import java.util.ArrayList;

/**
 * ‘айл виртуальной файловой системы
 */
public class File extends AtomEdit implements Comparable<File> 
{
	// им€
	private String name;
	// родительска€ директори€
	private Directory parent;
	// пользовательские блокировки
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
	 * ”даление
	 */
	public void delete() throws FileSystemException 
	{
		checkLocking();
		parent.delFile(this);		
	}
	
	/**
	 * ѕроверка на пользовательские блокировки
	 */
	private void checkLocking() throws FileSystemException 
	{
		if (isLocked())
			throw new FileSystemException(String.format("File %s is locked", getName()));	}

	/**
	 * ”становка пользовательской блокировки 
	 * @throws FileSystemException - при попытке установить блокировку дважды или нарушении доступа
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
	 * —н€тие пользовательской блокировки 
	 * @throws FileSystemException - при нарушении доступа
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
	 * ‘орматирование информации о пользовательских блокировках в текстовый вид
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
	 *  опирование файла 
	 * @throws FileSystemException - при нарушении доступа
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
