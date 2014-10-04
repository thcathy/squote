package thc.util;
 
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;
 
public class BeanUtilsTest {
	@Test	
	public void testConstructorIsPrivate() {
		final Constructor<?>[] constructors = BeanUtils.class.getDeclaredConstructors();
	    for (Constructor<?> constructor : constructors) {
	        assertTrue("All constructor should be private", Modifier.isPrivate(constructor.getModifiers()));
	    }
	}
}