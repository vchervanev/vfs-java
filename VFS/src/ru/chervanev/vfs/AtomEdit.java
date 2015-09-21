package ru.chervanev.vfs;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Класс для реализации установки атомарного флага по эксклюзивной блокировке ресурса 
 */
public abstract class AtomEdit 
{
	private AtomicBoolean isEditing = new AtomicBoolean(false);
	
	public abstract String getName();
	
	/**
	 * Начало редактирования.
	 * 
	 *  Гарантируется, что у одного объекта только один поток сможет успешно вызвать этот метод.
	 *  После этого, до момента вызова  endUpdate повторный вызов метода beginUpdate невозможен.
	 */
	public void beginUpdate() throws FileSystemException
	{
		if (!isEditing.compareAndSet(false, true))
		{
			throw new FileSystemException(String.format("Object %s is being edited by another user", getName()));
		}
	}
	/**
	 * Окончание редактирование. Допускается вызов даже без успешного вызова beginUpdate.
	 */
	public void endUpdate()
	{
		isEditing.compareAndSet(true, false);
	}
	
	public boolean isEditing()
	{
		return isEditing.get();
	}

}
