package ru.chervanev.vfs;
/**
 * Базовый интерфейс клиентской сессии 
 */
public interface ISession {
	String getUserName();
	void notifySession(String message);
	void notifySystem(String message);
}
