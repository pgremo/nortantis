package nortantis.util;

import java.io.*;

public class Helper 
{
	/**
	 * Creates a deep copy of an object using serialization.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T deepCopy(T toCopy)
	{
		if (toCopy == null)
		{
			return null;
		}
		
		byte[] storedObjectArray = serializableToByteArray(toCopy);
		try (var in = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(storedObjectArray))))
		{
			return (T) in.readObject();
		} catch (IOException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static <T extends Serializable> byte[] serializableToByteArray(T object)
	{
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		byte[] storedObjectArray;
		{
			try (ObjectOutputStream p = new ObjectOutputStream(new BufferedOutputStream(ostream)))
			{
				p.writeObject(object);
				p.flush();
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			storedObjectArray = ostream.toByteArray();
		}
		return storedObjectArray;
	}
}



