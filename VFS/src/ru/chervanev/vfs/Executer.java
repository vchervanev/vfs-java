package ru.chervanev.vfs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ”ниверсальный исполнитель методов одного объекта по имени с произвольным числом текстовых параметров
 *	
 * Ќа основе рефлексии.
 */
public abstract class Executer 
{
	protected Object object;
	/**
	 * ¬ыполнение команды
	 */
	public void execute(String command) throws Exception
	{
		// разбор команды
		parse(command);
		// поиск метода (регистронезависимо)
		Method targetMethod = null;
		for (Method method : object.getClass().getMethods())
		{
			// число параметров должно совпадать
			if (method.getName().compareToIgnoreCase(methodName) == 0 && method.getParameterTypes().length == parameters.size())
			{
				// тип параметров должен быть строковым
				// возможно приведение типов, но не требуетс€
				for(int i=0;i<parameters.size();i++)
				{
					if (!method.getParameterTypes()[i].equals(String.class))
						break;
				}
				targetMethod = method;
				break;
			}			
		}
		// ошибка поиска
		if (targetMethod == null)
			throw new Exception(String.format("Method called \"%s\" with parameters count %d has not been found", methodName, parameters.size()));
		// запуск. ошибки выполнени€ пробрасыватс€ наружу без изменений
		targetMethod.invoke(object, parameters.toArray());
	}
	
	// разделитель - пробел, хот€ возможна поддержка выражений в кавычках
	private Pattern pattern = Pattern.compile("\\S+"); // ((\\\"[^\"]+\\\")|\\S+)
	private String methodName;
	private ArrayList<String> parameters = new ArrayList<String>();
	
	// разбор команды на им€ метода и массив строковых параметров
	private void parse(String command) throws Exception
	{
		// поиск по шаблону
		Matcher matcher = pattern.matcher(command);
		parameters.clear();
		methodName = null;
		
		//первый элемент - им€ метода
		if(matcher.find())
			methodName = format(matcher.group());
		
		if (methodName == null)
			throw new Exception("Method name is empty");

		// остальные элементы - параметры
		while (matcher.find())
		    parameters.add(format(matcher.group()));
	}

	/**
	 * ќбработка текстовых параметров в части форматировани€
	 */
	private String format(String text)
	{
		String result = text.trim();
	
		/* на будущее
		if (result.startsWith("\"") && result.endsWith("\""))
		{
			result = result.substring(1, result.length()-1);
			result = result.trim();
		}
		*/
		return result;
	}
}