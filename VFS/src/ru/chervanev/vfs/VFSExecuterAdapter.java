package ru.chervanev.vfs;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Адаптер Executer для запуска текстовых команд на любом экземпляре IFileSystem
 * Run-time привязка MemoryVFS позволяет в перспективе использовать настроечный параметр  
 * 
 * Наследование от MemoryVFS не используется, для слабого связывания.
 * Альтернативная архитектура - создание дополнительно MemoryVFSProxy для связывания с адаптером.
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
			// Привязка к единому экземпляру ВФС по имени
			fileSystem = (IFileSystem)Class.forName("ru.chervanev.vfs.MemoryVFS").getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
			// Текущий каталог по умолчанию
			currentDir = fileSystem.listFileSystem().findDir("C:");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new Exception("Unable to instantiate File System ");			
		}
	}
	
	/**
	 * Адаптация исполнения команд.
	 * 
	 * Вызываются специфичные для ВФС методы: проверка на удаление текущей директории извне и нотификации
	 * 
	 */
	public void execute(String command) throws Exception
	{
		checkDeletion();
		super.execute(command);
		session.notifySystem(String.format("User %s performs command: %s", session.getUserName(), command));
	}
	
	/**
	 * Проверка, не была ли текущая директория удалена другим пользователем 
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
	 * Удаление каталога
	 * 
	 *  Дополнительные проверки, основанные на текущей директории сессии
	 */
	public void rd(String dirName) throws FileSystemException 
	{
		String fullDirName = formatName(dirName);
		if (currentDir.getFullName().toLowerCase().equals(fullDirName.toLowerCase()))
			throw new FileSystemException(String.format("Can not delete current directory: %s", dirName));

		fileSystem.rd(fullDirName);
	}

	/**
	 * Удаление иерархии каталогов
	 * 
	 *  Дополнительные проверки, основанные на текущей директории сессии
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
	
	// Методы прозрачно проксируются с использованием либо абсолютного имени директорий/файлов либо относительных, относительно текущей директории сессии
	
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
	 * Вывод на экран структуры каталогов
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
	 * Рекурсивный метод построения структуры директорий псевдографикой
	 * 
	 * При выводе директории и файлы сортируются независимо
	 * 
	 * @param sb - StringBuilder
	 * @param prefix1 - префикс для отображения у дочерней записи
	 * @param prefix2 - префикс для отображения у "внуков" - будет передан в prefix1 на рекурсивном спуске
	 * @param dir - текущий каталог для рекурсивного перебора
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
	 * Дополнение имени файла или каталога до абсолютного
	 * @param dirOrFileName - имя файла или каталога, введенные пользователем 
	 * @return абсолютное имя файла или каталога (правильность не проверяется, только наличие Х: в начале имени)
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
	 * Метод для тестирования и отображения внутренней информации сервера 
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
