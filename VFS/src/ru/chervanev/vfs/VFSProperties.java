package ru.chervanev.vfs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * ����� ��������� ������� ������� 
 */
public class VFSProperties extends Properties {

	private static final long serialVersionUID = 5396552419904815264L;
	final static public String fileName = "app.config";
	
	/**
	 * ������ �������������� ��������
	 */
	private int getIntProperty(String key)
	{
		int result;
		try
		{
			result = Integer.parseInt(getProperty(key));
		}catch(NumberFormatException exception)
		{
			setDefaults(); // TO DO: set default property value by name 
			save();
			result = Integer.parseInt(getProperty(key)); 
		}
		return result;		
	}

	public int getPort()
	{
		return getIntProperty("port");
	}
    	

	/**
	 * ���������� �������� �� ���������
	 */
	private void setDefaults()
	{
		setProperty("port", "8123");
	}
	
	/**
	 * ���������� ����������������� �����
	 */
	private void save()
	{
		try {
			OutputStream out = new FileOutputStream(fileName);
			super.store(out, "");
		} catch (IOException e) {
			System.err.printf("File \"%s\" can't be created or updated", fileName);
		}	
	}
	
	/**
	 * �������� �������� �� �����
	 */
	public void load()
	{
	    InputStream in;
		try {
			in = new FileInputStream(fileName);
			load(in);
		} catch (IOException ss) {
			setDefaults();
			save();
		}		
	}
}
