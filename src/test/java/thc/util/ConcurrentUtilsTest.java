package thc.util;


import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.springframework.test.util.AssertionErrors.assertTrue;


public class ConcurrentUtilsTest {
	@Test
	public void getDeclaredConstructors_ShouldBePrivate() {
		final Constructor<?>[] constructors = ConcurrentUtils.class.getDeclaredConstructors();		
	    for (Constructor<?> constructor : constructors) {
	        assertTrue("All constructor should be private", Modifier.isPrivate(constructor.getModifiers()));
	    }
	}
}
