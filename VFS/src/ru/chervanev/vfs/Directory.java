package ru.chervanev.vfs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Класс директория
 * Имеет имя, содержит вложенные директории и файлы
 * Поддерживает атомарную блокировку директории или директории и всех дочерних файлов и директорий
 * Поддерживает сортировку (без учета локали).
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
	 * Добавление дочерней директории
	 * @param directory - добавляемая диретория
	 * @throws FileSystemException - в случае нарушения уникальности (регистронезависимо)
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
	 * Метод поиска поддиректории по ее имени.
	 * 
	 * @param dirName - строковое значение для иерархии каталогов (например c:\1\2\3)
	 * @return ссылка на Directory, описываемый dirName
	 * @throws FileSystemException - если какой-нибудь из каталогов не был найден
	 */
	public Directory findDir(String dirName) throws FileSystemException {
		String[] dirs = dirName.split("\\\\");
		Directory result = this;
		// разбор имени директории на составные части и последовательный поиск дочерней директории по имени 
		if (dirs.length != 1)
		{
			for(int i=0;i<dirs.length;i++)
			{
				result = result.findDir(dirs[i]);
			}
		}
		else
		{
			// окончание рекурсивного спуска - передано единственное имя директории
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
	 * Метод проверки вложенности директорий (рекурсия)
	 * @param dir - другая директория
	 * @return true если диретория дочерняя
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
	 * Компаратор
	 */
	@Override
	public int compareTo(Directory o) 
	{
		return this.toString().compareTo(o.toString());
	}

	/**
	 * Метод проверки наличия пользовательских блокировок
	 * @throws FileSystemException - если файл заблокирован
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
		// рекурсивная проверка:
		for(Directory directory : children.values())
		{
			directory.checkLocking();
		}		
	}

	/**
	 * Удаление дочерней диретории
	 * @param directory - директория для удаления из текущей
	 */
	private void delChild(Directory directory) {
		children.remove(directory.getName().toLowerCase());		
	}

	/**
	 * Удаление директории как объекта. Проверяется наличие пользовательских блокировок.
	 */
	public void delete() throws FileSystemException 
	{
		checkLocking();
		parent.delChild(this);
		parent = null;
	}
	
	/**
	 * Удаление дерева директорий. Вызывает удаление каждой директории по отдельности
	 */
	public void delTree() throws FileSystemException
	{
		delete();
		delChildren();		
	}
	
	/**
	 * Рекурсивное удаление дочерних директорий
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
	 * Добавление файла в директорию
	 * @param file - файл для добавления
	 * @throws FileSystemException - если нарушена уникальность имени (регистронезависимо)
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
	 * Поиск файла в директории
	 * @param fileName - имя файла
	 * @return объект файл
	 * @throws FileSystemException - если файл не найден
	 */
	public File findFile(String fileName) throws FileSystemException 
	{
		File result;
		int i = fileName.indexOf('\\');
		if (i == -1)
		{
			// поиск файла в текущей директории
			result = files.get(fileName.toLowerCase());
			if (result == null)
				throw new FileSystemException(String.format("File %s not found in directory", fileName));
		}
		else
		{
			// поиск файла в дочерней директории
			//родительская директория указана до последнего "\"
			String parentDir = fileName.substring(0, i);
			//после "\" указано имя новой директории
			fileName = fileName.substring(i+1);
			result = findDir(parentDir).findFile(fileName);			
		}
		return result;
	}


	/**
	 * Удаление файла
	 * @param file
	 */
	public void delFile(File file) 
	{
		files.remove(file.getName().toLowerCase());		
	}

	/**
	 * Копирование директории в новую родительскую директорию
	 * @param destDir - целевая директория
	 */
	public void copy(Directory destDir) throws FileSystemException 
	{
		// копия имени
		Directory copy = new Directory(getName());
		// рекурсивное копирование
		for(Directory sub : children.values())
		{
			sub.copy(copy);
		}
		
		// копирование файлов
		for(File file: files.values())
		{
			new File(copy, file.getName());
		}
		copy.parent = destDir;
		// родитель назначается на выходе из рекурсии, чтобы нельзя было получить зацикливания при копировании в дочернюю директорию
		// при этом выполняется проверка на уникальность имени, для производительности можно было продублировать ранее
		destDir.addChild(copy);
	}

	/**
	 * Перемещение директории в новую родительскую директорию
	 * @param destDir - целевая директория
	 */
	public void move(Directory destDir) throws FileSystemException 
	{
		// проверка пользовательских блокировок
		checkLocking();
		// может вызвать ошибку дублирования имени, поэтому первый вызов
		destDir.addChild(this);
		parent.delChild(this);
		parent = destDir;
		
	}
	/**
	 * Построение полного имени директории, начиная с корня (восходящая рекурсия)
	 * @return - полное имя директории
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
	 * Метод атомарного рекурсивного блокирования директории от параллельного редактировния, и ее вложенных файлов и директорий
	 * В случае невозможности закончить рекурсивную блокировку - поставленные до этого блокировки снимаются
	 * @throws FileSystemException - если блокировка невозможна
	 */
	public void beginUpdateResursive() throws FileSystemException
	{
		beginUpdate();
		ArrayList<AtomEdit> edited = new ArrayList<AtomEdit>();
		// начинаем редактирование всех дочерних директорий
		try
		{
			// рекурсивный спуск
			for(Directory d : children.values())
			{
				d.beginUpdateResursive();
				// запоминаем директорию для отката в случае ошибки
				edited.add(d);
			}

			//Аналогично с файлами, но без рекурсии:
			for(File f : files.values())
			{
				// запоминаем файл для отката в случае ошибки
				f.beginUpdate();
				edited.add(f);
			}

		}catch(FileSystemException e)
		{
			// beginUpdate прошел успешно, поэтому:
			endUpdate();
			// какой-то из дочерних элементов может быть заблокирован, 
			// отпускаем все, что начали редактировать:
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
	 * Окончание рекурсивного эксклюзивного редактирования 
	 */
	public void endUpdateResursive()
	{
		//Редактирование this закончено:
		endUpdate();
		//Редактирование вложенных файлов закончено:
		for(File f : files.values())
		{
			f.endUpdate();
		}		
		// рекурсия:
		for(Directory d : children.values())			
		{
			d.endUpdateResursive();
		}
	}
}
