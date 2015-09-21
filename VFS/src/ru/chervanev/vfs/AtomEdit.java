package ru.chervanev.vfs;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ����� ��� ���������� ��������� ���������� ����� �� ������������ ���������� ������� 
 */
public abstract class AtomEdit 
{
	private AtomicBoolean isEditing = new AtomicBoolean(false);
	
	public abstract String getName();
	
	/**
	 * ������ ��������������.
	 * 
	 *  �������������, ��� � ������ ������� ������ ���� ����� ������ ������� ������� ���� �����.
	 *  ����� �����, �� ������� ������  endUpdate ��������� ����� ������ beginUpdate ����������.
	 */
	public void beginUpdate() throws FileSystemException
	{
		if (!isEditing.compareAndSet(false, true))
		{
			throw new FileSystemException(String.format("Object %s is being edited by another user", getName()));
		}
	}
	/**
	 * ��������� ��������������. ����������� ����� ���� ��� ��������� ������ beginUpdate.
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
