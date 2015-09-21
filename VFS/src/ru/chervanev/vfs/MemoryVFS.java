package ru.chervanev.vfs;
/**
 * ���������� ��� � ������
 * 
 *  ��� ��������� ���� ���������� ��� ������ ������ ���� �����������
 *
 */
public class MemoryVFS implements IFileSystem {

	// singleton ������ �������� �������	
	private static MemoryVFS instance = new MemoryVFS();
	// ��������� ��� ���������� �������� ������:
	private Directory root;
	
	public static MemoryVFS getInstance()
	{		
		return instance;
	}
	
	public MemoryVFS()
	{
		root = new Directory("");
		try {
			// ������������� ������� ���� �:
			new Directory(root, "C:");
		} catch (FileSystemException e) {
			// nothing to do			
		}
	}

	/**
	 * �������� ����������. ������������� ���������� ������ ������������.
	 * ������������ ���������� ����������� ����������� �� ������ ����������.
	 * ���� ������������ ���������� ������������� ������ ������������� - ���������� ����������. 
	 */
	@Override
	public void md(String dirName) throws FileSystemException 
	{
		//������� � ����� \ ���� ����		
		if (dirName.endsWith("\\"))
			dirName = dirName.substring(0, dirName.length()-1);
		int i = dirName.lastIndexOf('\\');
		Directory parentDir;
		String child;
		if (i == -1)
		{
			throw new FileSystemException("Absolute directory name is required");
		}

		//������������ ���������� ������� �� ���������� "\" � ������ ������������
		String parent = dirName.substring(0, i);
		//����� "\" ������� ��� ����� ����������
		child = dirName.substring(i+1, dirName.length());
		
		// ���� ����������:
		parentDir = root.findDir(parent);

		// �������� ������������� ��������. ����� �� ������ ��� �������, ����������� � �.�.
		parentDir.beginUpdate();
		try
		{
			//�������
			new Directory(parentDir, child);
		}
		finally
		{
			parentDir.endUpdate();
		}

	}

	/**
	 * �������� ����������. �������� ���������� � ���������������� ���������� ������ ��������� ��������.  
	 * 
	 * ����� ��������� ���������� ����������� �����������.
	 * ���� ���� �� ���� �������� ���������� ��� ���� ������������� ������ ������������� - �������� ����������.
	 */
	@Override
	public void rd(String dirName) throws FileSystemException {
		Directory dir = root.findDir(dirName);
		if (dir.getParent()==root)
			throw new FileSystemException(String.format("Cannot delete root directory: %s", dirName));

		// ��������� �� �������� �� ������� ��������� ����������
		dir.beginUpdate();
		try
		{
			if (dir.hasChildren())
			{
				throw new FileSystemException(String.format("Cannot delete directory: %s - sub directory exists", dirName));
			}
			dir.delete();
		}catch(FileSystemException e)
		{
			// ������� ���������� ������ � ������ ������
			// ����� �� ���������, ������� ��� ��
			dir.endUpdate();
			throw e;
		}
	}

	/**
	 * �������� ���������� � �� ��������� ����������.
	 * ���������������� ���������� ������ ��������� ��������.
	 * 
	 * ����� ��������� ���������� ����������� ����������� ����������.
	 * ���� ���� �� ���� �������� ���������� ��� ���� ������������� ������ ������������� - �������� ����������.
	 */
	@Override
	public void delTree(String dirName) throws FileSystemException {
		Directory dir = root.findDir(dirName);
		if (dir.getParent()==root)
			throw new FileSystemException(String.format("Cannot delete root directory: %s", dirName));

		dir.beginUpdateResursive();
		try
		{
			dir.delTree();
		}catch(FileSystemException ex)
		{
			dir.endUpdateResursive();
			throw ex;
		}		
	}

	/**
	 * �������� �����. ���� ������ ������������.
	 * ��� �������� ����� ������������ ���������� ����������� �����������.
	 */
	@Override
	public void mf(String fileName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, false);
		helper.createFile();
	}

	/**
	 * �������� �����. ���� ������ ������������.
	 * ��� �������� ����� ���������� � ���� �����������.
	 */
	@Override
	public void del(String fileName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.deleteFile();
	}

	/**
	 * ���������������� ���������� �����.
	 * ��� ��������� ���������� ���� ����������� �����������.
	 */
	@Override
	public void lock(String fileName, String userName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.getFile().lock(userName);
	}

	/**
	 * ������ ���������������� ���������� �����.
	 * ��� ������ ���������� ���� ����������� �����������.
	 */
	@Override
	public void unlock(String fileName, String userName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.getFile().unlock(userName);
	}

	/***
	 * ������ �����, ������� ����� ���� ��� ������, ��� � ����������
	 * @param source ��� ����� ��� ��������
	 * @return ������ ���� ��� ����������
	 * @throws FileSystemException
	 */
	private Object parseSource(String source) throws FileSystemException
	{
		try
		{
			return root.findDir(source);
		}catch(FileSystemException x)
		{
		}
		try
		{
			return new FileHelper(source, true).getFile();
		}catch(FileSystemException x)
		{
			throw new FileSystemException(String.format("Source file or directory %s is not found", source));
		}
		
	}
	/**
	 * ����������� ����������� ����� ��� ���������� � ����� ����������.
	 *
	 * �� ����� ������ ������:
	 * 
	 * � ������ ����������� ����� ���������� ���� ����������� �����������
	 * � ������ ����������� ���������� ���������� ���������� ����������� ����������� ����������
	 * ������� ���������� ���������� �����������
	 */
	@Override
	public void copy(String source, String dirName) throws FileSystemException 
	{
		// �������� ��� ����������� - ���� ����������, ���� ����. 
		// ����������� ���������� ����� ���������� � �����
		// ������ ����������� ������� ���������� � ��������� ������
		Object src = parseSource(source);
				Directory destDir = root.findDir(dirName);
		destDir.beginUpdate();
		try
		{
			if (src.getClass().isAssignableFrom(Directory.class) )
			{
				Directory srcDir = (Directory)src;
				srcDir.beginUpdateResursive();
				try	{
					srcDir.copy(destDir);
				}finally{
					srcDir.endUpdateResursive();
				}
			} else
			{
				File srcFile = (File)src;
				srcFile.beginUpdate();
				try	{
					srcFile.copy(destDir);
				}finally{
					srcFile.endUpdate();
				}
			}
		}
		finally{
			destDir.endUpdate();
		}
		
	}
	/**
	 * ����������� ����� ��� ���������� � ����� ����������
	 * 
	 * �� ����� ����������� �������� ���� � ������� ���������� �����������
	 * � ������ ����������� ���������� ������� ���������� ����������� ����������
	 */
	@Override
	public void move(String source, String dirName) throws FileSystemException 
	{
		// �������� ��� ����������� - ���� ����������, ���� ����. 
		// ����������� ���������� ����� ���������� � �����
		// ������ ����������� ������� ���������� � ��������� ������
		Object src = parseSource(source);
		Directory destDir = root.findDir(dirName);
		
		destDir.beginUpdate();
		try
		{
			if (src.getClass().isAssignableFrom(Directory.class) )
			{
				Directory srcDir = (Directory)src;
				srcDir.beginUpdateResursive();
				try{
					srcDir.move(destDir);
				}finally{
					srcDir.endUpdateResursive();
				}				
			} else
			{
				File srcFile = (File)src;
				srcFile.beginUpdate();
				try {
					srcFile.move(destDir);
				}finally{
					srcFile.endUpdate();
				}
			}
		}
		finally
		{
			destDir.endUpdate();
		}
	}

	/**
	 * ����� ���������� ������-��������� �� ������ ��� 
	 */
	@Override
	public Directory listFileSystem() {
		return root;
	}

	/**
	 * ����� ������� ������� ����� ����� � ������ ��� ���������� �, �����������, �����.
	 * 
	 */
	private class FileHelper
	{
		private Directory directory;
		private String fileName;
		private File file;
		
		public FileHelper(String fullName, boolean findFile) throws FileSystemException
		{
			int i = fullName.lastIndexOf('\\');
			if (i == -1)
			{
				throw new FileSystemException("Absolute file name is required");
			}else
			{
				//������������ ���������� ������� �� ���������� "\"
				String parent = fullName.substring(0, i);
				//����� "\" ������� ��� ����� ����������
				fileName = fullName.substring(i+1, fullName.length());
				
				directory = root.findDir(parent);
			}
			if (findFile)
				file = directory.findFile(fileName);
			}
			
			public File getFile() 
			{
				return file;
			}

			/**
			 * �������� �����, ����� ������� ��� ����� � ������� ����
			 */
			public void createFile() throws FileSystemException 
			{
				directory.beginUpdate();
				try	{
					file = new File(directory, fileName);
				}finally{
					directory.endUpdate();
				}
			}

			/**
			 * �������� �����
			 */
			public void deleteFile() throws FileSystemException
			{
				
				directory.beginUpdate();
				try	{
					file.beginUpdate();
					try{
						file.delete();
					}finally{					
						file.endUpdate();
					}
				}finally{
					directory.endUpdate();
				}			
			}
	}
}
