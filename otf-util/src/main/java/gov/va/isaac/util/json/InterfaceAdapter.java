/**
 * Copyright Notice
 * 
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.va.isaac.util.json;

/**
 * {@link InterfaceAdapter}
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * 
 * Mostly copied from http://stackoverflow.com/questions/4795349/how-to-serialize-a-class-with-an-interface/9550086#9550086
 * 
 */
import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T>
{
	public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context)
	{
		final JsonObject wrapper = new JsonObject();
		wrapper.addProperty("type", object.getClass().getName());
		wrapper.add("data", context.serialize(object));
		return wrapper;
	}

	public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException
	{
		final JsonObject wrapper = (JsonObject) elem;
		final JsonElement typeName = get(wrapper, "type");
		final JsonElement data = get(wrapper, "data");
		final Type actualType = typeForName(typeName);
		return context.deserialize(data, actualType);
	}

	private Type typeForName(final JsonElement typeElem)
	{
		try
		{
			return Class.forName(typeElem.getAsString());
		}
		catch (ClassNotFoundException e)
		{
			throw new JsonParseException(e);
		}
	}

	private JsonElement get(final JsonObject wrapper, String memberName)
	{
		final JsonElement elem = wrapper.get(memberName);
		if (elem == null)
			throw new JsonParseException("no '" + memberName + "' member found in what was expected to be an interface wrapper");
		return elem;
	}
}
