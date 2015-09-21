package ru.chervanev.vfs;

/**
 * Базовый интерфейс ВФС 
 */
public interface IFileSystem
{
	void md(String dirName) throws FileSystemException;
	void rd(String dirName) throws FileSystemException;
	void delTree(String dirName) throws FileSystemException;
	void mf(String fileName) throws FileSystemException;
	void del(String fileName) throws FileSystemException;
	void lock(String fileName, String userName) throws FileSystemException;
	void unlock(String fileName, String userName) throws FileSystemException;
	void copy(String source, String dirName) throws FileSystemException;
	void move(String source, String dirName) throws FileSystemException;
	Directory listFileSystem();
}
