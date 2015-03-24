package thc.util;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class ConcurrentUtilsTest {
	@Test	
	public void getDeclaredConstructors_ShouldBePrivate() {
		final Constructor<?>[] constructors = ConcurrentUtils.class.getDeclaredConstructors();		
	    for (Constructor<?> constructor : constructors) {
	        assertTrue("All constructor should be private", Modifier.isPrivate(constructor.getModifiers()));
	    }
	}
}
