package ru.chervanev.vfs;
/**
 * ������� ��������� ���������� ������ 
 */
public interface ISession {
	String getUserName();
	void notifySession(String message);
	void notifySystem(String message);
}
