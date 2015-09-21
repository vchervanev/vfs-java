package ru.chervanev.vfs;
/**
 * Реализация ВФС в памяти
 * 
 *  Все параметры пути директорий или файлов должны быть абсолютными
 *
 */
public class MemoryVFS implements IFileSystem {

	// singleton единой файловой системы	
	private static MemoryVFS instance = new MemoryVFS();
	// контейнер для директорий верхнего уровня:
	private Directory root;
	
	public static MemoryVFS getInstance()
	{		
		return instance;
	}
	
	public MemoryVFS()
	{
		root = new Directory("");
		try {
			// автоматически создаем диск С:
			new Directory(root, "C:");
		} catch (FileSystemException e) {
			// nothing to do			
		}
	}

	/**
	 * Создание директории. Промежуточные директории должны существовать.
	 * Родительская директория эксклюзивно блокируется на период добавления.
	 * Если родительская директория редактируется другим пользователем - добавление невозможно. 
	 */
	@Override
	public void md(String dirName) throws FileSystemException 
	{
		//убираем с конца \ если есть		
		if (dirName.endsWith("\\"))
			dirName = dirName.substring(0, dirName.length()-1);
		int i = dirName.lastIndexOf('\\');
		Directory parentDir;
		String child;
		if (i == -1)
		{
			throw new FileSystemException("Absolute directory name is required");
		}

		//родительская директория указана до последнего "\" и должна существовать
		String parent = dirName.substring(0, i);
		//после "\" указано имя новой директории
		child = dirName.substring(i+1, dirName.length());
		
		// ищем директорию:
		parentDir = root.findDir(parent);

		// начинаем редактировать родителя. никто не сможет его удалить, скопировать и т.п.
		parentDir.beginUpdate();
		try
		{
			//создаем
			new Directory(parentDir, child);
		}
		finally
		{
			parentDir.endUpdate();
		}

	}

	/**
	 * Удаление директории. Дочерние директории и пользовательские блокировки файлов запрещают удаление.  
	 * 
	 * Перед удалением директория эксклюзивно блокируется.
	 * Если хотя бы одна дочерняя директория или файл редактируются другим пользователем - удаление невозможно.
	 */
	@Override
	public void rd(String dirName) throws FileSystemException {
		Directory dir = root.findDir(dirName);
		if (dir.getParent()==root)
			throw new FileSystemException(String.format("Cannot delete root directory: %s", dirName));

		// блокируем до проверки на наличие вложенных директорий
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
			// снимаем блокировку только в случае ошибки
			// иначе не требуется, объекты вне ФС
			dir.endUpdate();
			throw e;
		}
	}

	/**
	 * Удаление директории и ее вложенных директорий.
	 * Пользовательские блокировки файлов запрещают удаление.
	 * 
	 * Перед удалением директория эксклюзивно блокируется рекурсивно.
	 * Если хотя бы одна дочерняя директория или файл редактируются другим пользователем - удаление невозможно.
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
	 * Создание файла. Путь должен существовать.
	 * При создании файла родительская директория эксклюзивно блокируется.
	 */
	@Override
	public void mf(String fileName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, false);
		helper.createFile();
	}

	/**
	 * Удаление файла. Файл должен существовать.
	 * При удалении файла директория и файл блокируются.
	 */
	@Override
	public void del(String fileName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.deleteFile();
	}

	/**
	 * Пользовательская блокировка файла.
	 * Для установки блокировки файл эксклюзивно блокируется.
	 */
	@Override
	public void lock(String fileName, String userName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.getFile().lock(userName);
	}

	/**
	 * Снятие пользовательской блокировки файла.
	 * Для снятия блокировки файл эксклюзивно блокируется.
	 */
	@Override
	public void unlock(String fileName, String userName) throws FileSystemException 
	{
		FileHelper helper = new FileHelper(fileName, true);
		helper.getFile().unlock(userName);
	}

	/***
	 * Разбор имени, которое может быть как файлом, так и директории
	 * @param source имя файла или каталога
	 * @return объект файл или директория
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
	 * Рекурсивное копирование файла или директории в новую директорию.
	 *
	 * На время работы метода:
	 * 
	 * В случае копирования файла копируемый файл эксклюзивно блокируется
	 * В случае копирования директории копируемая директория эксклюзивно блокируется рекурсивно
	 * Целевая директория экслюзивно блокируется
	 */
	@Override
	public void copy(String source, String dirName) throws FileSystemException 
	{
		// источник для копирования - либо директория, либо файл. 
		// допускается совпадание имени директории и файла
		// первым проверяется наличие директории с указанным именем
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
	 * Перемещение файла или директории в новую директорию
	 * 
	 * На время перемещения исходный файл и целевая директория блокируются
	 * В случае перемещения директории исходня директория блокируется рекурсивно
	 */
	@Override
	public void move(String source, String dirName) throws FileSystemException 
	{
		// источник для перемещения - либо директория, либо файл. 
		// допускается совпадание имени директории и файла
		// первым проверяется наличие директории с указанным именем
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
	 * Метод возвращает объект-указатель на корень ВФС 
	 */
	@Override
	public Directory listFileSystem() {
		return root;
	}

	/**
	 * Класс разбора полного имени файла и поиска его директории и, опционально, файла.
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
				//родительская директория указана до последнего "\"
				String parent = fullName.substring(0, i);
				//после "\" указано имя новой директории
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
			 * Создание файла, после разбора его имени и полного пути
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
			 * Удаление файла
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
