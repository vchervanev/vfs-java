package ru.chervanev.vfs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ������������� ����������� ������� ������ ������� �� ����� � ������������ ������ ��������� ����������
 *	
 * �� ������ ���������.
 */
public abstract class Executer 
{
	protected Object object;
	/**
	 * ���������� �������
	 */
	public void execute(String command) throws Exception
	{
		// ������ �������
		parse(command);
		// ����� ������ (������������������)
		Method targetMethod = null;
		for (Method method : object.getClass().getMethods())
		{
			// ����� ���������� ������ ���������
			if (method.getName().compareToIgnoreCase(methodName) == 0 && method.getParameterTypes().length == parameters.size())
			{
				// ��� ���������� ������ ���� ���������
				// �������� ���������� �����, �� �� ���������
				for(int i=0;i<parameters.size();i++)
				{
					if (!method.getParameterTypes()[i].equals(String.class))
						break;
				}
				targetMethod = method;
				break;
			}			
		}
		// ������ ������
		if (targetMethod == null)
			throw new Exception(String.format("Method called \"%s\" with parameters count %d has not been found", methodName, parameters.size()));
		// ������. ������ ���������� ������������� ������ ��� ���������
		targetMethod.invoke(object, parameters.toArray());
	}
	
	// ����������� - ������, ���� �������� ��������� ��������� � ��������
	private Pattern pattern = Pattern.compile("\\S+"); // ((\\\"[^\"]+\\\")|\\S+)
	private String methodName;
	private ArrayList<String> parameters = new ArrayList<String>();
	
	// ������ ������� �� ��� ������ � ������ ��������� ����������
	private void parse(String command) throws Exception
	{
		// ����� �� �������
		Matcher matcher = pattern.matcher(command);
		parameters.clear();
		methodName = null;
		
		//������ ������� - ��� ������
		if(matcher.find())
			methodName = format(matcher.group());
		
		if (methodName == null)
			throw new Exception("Method name is empty");

		// ��������� �������� - ���������
		while (matcher.find())
		    parameters.add(format(matcher.group()));
	}

	/**
	 * ��������� ��������� ���������� � ����� ��������������
	 */
	private String format(String text)
	{
		String result = text.trim();
	
		/* �� �������
		if (result.startsWith("\"") && result.endsWith("\""))
		{
			result = result.substring(1, result.length()-1);
			result = result.trim();
		}
		*/
		return result;
	}
}