package ru.chervanev.vfs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * ����� ����������
 * ����� ���, �������� ��������� ���������� � �����
 * ������������ ��������� ���������� ���������� ��� ���������� � ���� �������� ������ � ����������
 * ������������ ���������� (��� ����� ������).
 */
public class Directory extends AtomEdit implements Comparable<Directory>
{
	private String name;
	private Directory parent;
	private HashMap<String, Directory> children = new HashMap<String, Directory>();
	private HashMap<String, File> files = new HashMap<String, File>();
	
	public String getName()
	{
		return name;
	}
	
	public Directory(String name)
	{
		this.name = name;
	}
	
	public Directory(Directory parent, String name) throws FileSystemException
	{
		this.name = name;
		this.parent = parent;
		this.parent.addChild(this);
	}
	

	/**
	 * ���������� �������� ����������
	 * @param directory - ����������� ���������
	 * @throws FileSystemException - � ������ ��������� ������������ (������������������)
	 */
	private void addChild(Directory directory) throws FileSystemException 
	{		
		String key = directory.getName().toLowerCase();
		if (!children.containsKey(key))
			children.put(key, directory);
		else
			throw new FileSystemException(String.format("Directory %s already exists", directory.getName()));
	}
	/**
	 * ����� ������ ������������� �� �� �����.
	 * 
	 * @param dirName - ��������� �������� ��� �������� ��������� (�������� c:\1\2\3)
	 * @return ������ �� Directory, ����������� dirName
	 * @throws FileSystemException - ���� �����-������ �� ��������� �� ��� ������
	 */
	public Directory findDir(String dirName) throws FileSystemException {
		String[] dirs = dirName.split("\\\\");
		Directory result = this;
		// ������ ����� ���������� �� ��������� ����� � ���������������� ����� �������� ���������� �� ����� 
		if (dirs.length != 1)
		{
			for(int i=0;i<dirs.length;i++)
			{
				result = result.findDir(dirs[i]);
			}
		}
		else
		{
			// ��������� ������������ ������ - �������� ������������ ��� ����������
			String key = dirs[0].toLowerCase();
			result = children.get(key);
			if (result == null)
				throw new FileSystemException(String.format("Directory %s doesn't exist", dirs[0]));
		}
		
		return result;
	}
	
	public String toString()
	{
		return getName();
	}
	
	public Collection<Directory> dirCollection()
	{
		return children.values();
	}
	
	
	public Collection<File> fileCollection()
	{
		return files.values();		
	}	

	public boolean hasChildren() 
	{
		return children.size() != 0;
	}

	public Directory getParent() 
	{
		return parent;
	}

	/**
	 * ����� �������� ����������� ���������� (��������)
	 * @param dir - ������ ����������
	 * @return true ���� ��������� ��������
	 */
	public boolean isChild(Directory dir) 
	{
		if (parent == dir)
			return true;
		else 
			if (parent != null)
				return parent.isChild(dir);
			else
				return false;
	}

	/**
	 * ����������
	 */
	@Override
	public int compareTo(Directory o) 
	{
		return this.toString().compareTo(o.toString());
	}

	/**
	 * ����� �������� ������� ���������������� ����������
	 * @throws FileSystemException - ���� ���� ������������
	 */
	private void checkLocking() throws FileSystemException 
	{
		for(File file : files.values())
		{
			if (file.isLocked())
			{
				throw new FileSystemException(String.format("File %s is locked", file.getName()));
			}
		}
		// ����������� ��������:
		for(Directory directory : children.values())
		{
			directory.checkLocking();
		}		
	}

	/**
	 * �������� �������� ���������
	 * @param directory - ���������� ��� �������� �� �������
	 */
	private void delChild(Directory directory) {
		children.remove(directory.getName().toLowerCase());		
	}

	/**
	 * �������� ���������� ��� �������. ����������� ������� ���������������� ����������.
	 */
	public void delete() throws FileSystemException 
	{
		checkLocking();
		parent.delChild(this);
		parent = null;
	}
	
	/**
	 * �������� ������ ����������. �������� �������� ������ ���������� �� �����������
	 */
	public void delTree() throws FileSystemException
	{
		delete();
		delChildren();		
	}
	
	/**
	 * ����������� �������� �������� ����������
	 */
	private void delChildren() 
	{
		for(Directory subDir : children.values())
		{
			subDir.delChildren();			
		}
		children.clear();
		parent = null;
	}

	/**
	 * ���������� ����� � ����������
	 * @param file - ���� ��� ����������
	 * @throws FileSystemException - ���� �������� ������������ ����� (������������������)
	 */
	public void addFile(File file) throws FileSystemException 
	{
		String key = file.getName().toLowerCase();
		if (!files.containsKey(key))
			files.put(key, file);
		else
			throw new FileSystemException(String.format("File %s already exists", file.getName()));
	}
	/**
	 * ����� ����� � ����������
	 * @param fileName - ��� �����
	 * @return ������ ����
	 * @throws FileSystemException - ���� ���� �� ������
	 */
	public File findFile(String fileName) throws FileSystemException 
	{
		File result;
		int i = fileName.indexOf('\\');
		if (i == -1)
		{
			// ����� ����� � ������� ����������
			result = files.get(fileName.toLowerCase());
			if (result == null)
				throw new FileSystemException(String.format("File %s not found in directory", fileName));
		}
		else
		{
			// ����� ����� � �������� ����������
			//������������ ���������� ������� �� ���������� "\"
			String parentDir = fileName.substring(0, i);
			//����� "\" ������� ��� ����� ����������
			fileName = fileName.substring(i+1);
			result = findDir(parentDir).findFile(fileName);			
		}
		return result;
	}


	/**
	 * �������� �����
	 * @param file
	 */
	public void delFile(File file) 
	{
		files.remove(file.getName().toLowerCase());		
	}

	/**
	 * ����������� ���������� � ����� ������������ ����������
	 * @param destDir - ������� ����������
	 */
	public void copy(Directory destDir) throws FileSystemException 
	{
		// ����� �����
		Directory copy = new Directory(getName());
		// ����������� �����������
		for(Directory sub : children.values())
		{
			sub.copy(copy);
		}
		
		// ����������� ������
		for(File file: files.values())
		{
			new File(copy, file.getName());
		}
		copy.parent = destDir;
		// �������� ����������� �� ������ �� ��������, ����� ������ ���� �������� ������������ ��� ����������� � �������� ����������
		// ��� ���� ����������� �������� �� ������������ �����, ��� ������������������ ����� ���� �������������� �����
		destDir.addChild(copy);
	}

	/**
	 * ����������� ���������� � ����� ������������ ����������
	 * @param destDir - ������� ����������
	 */
	public void move(Directory destDir) throws FileSystemException 
	{
		// �������� ���������������� ����������
		checkLocking();
		// ����� ������� ������ ������������ �����, ������� ������ �����
		destDir.addChild(this);
		parent.delChild(this);
		parent = destDir;
		
	}
	/**
	 * ���������� ������� ����� ����������, ������� � ����� (���������� ��������)
	 * @return - ������ ��� ����������
	 */
	public String getFullName() 
	{
		if (parent==null || parent.getName().isEmpty())
			return getName();
		else
			return parent.getFullName() + "\\" + getName();
	}

	public boolean isDeleted() 
	{
		return parent == null;
	}

	/**
	 * ����� ���������� ������������ ������������ ���������� �� ������������� �������������, � �� ��������� ������ � ����������
	 * � ������ ������������� ��������� ����������� ���������� - ������������ �� ����� ���������� ���������
	 * @throws FileSystemException - ���� ���������� ����������
	 */
	public void beginUpdateResursive() throws FileSystemException
	{
		beginUpdate();
		ArrayList<AtomEdit> edited = new ArrayList<AtomEdit>();
		// �������� �������������� ���� �������� ����������
		try
		{
			// ����������� �����
			for(Directory d : children.values())
			{
				d.beginUpdateResursive();
				// ���������� ���������� ��� ������ � ������ ������
				edited.add(d);
			}

			//���������� � �������, �� ��� ��������:
			for(File f : files.values())
			{
				// ���������� ���� ��� ������ � ������ ������
				f.beginUpdate();
				edited.add(f);
			}

		}catch(FileSystemException e)
		{
			// beginUpdate ������ �������, �������:
			endUpdate();
			// �����-�� �� �������� ��������� ����� ���� ������������, 
			// ��������� ���, ��� ������ �������������:
			for(AtomEdit ed : edited)
			{
				if (ed.getClass().equals(this.getClass()))
					((Directory)ed).endUpdateResursive();
				else
					ed.endUpdate();
			}
			throw e;
		}
		edited.clear();
	}
	
	/**
	 * ��������� ������������ ������������� �������������� 
	 */
	public void endUpdateResursive()
	{
		//�������������� this ���������:
		endUpdate();
		//�������������� ��������� ������ ���������:
		for(File f : files.values())
		{
			f.endUpdate();
		}		
		// ��������:
		for(Directory d : children.values())			
		{
			d.endUpdateResursive();
		}
	}
}
