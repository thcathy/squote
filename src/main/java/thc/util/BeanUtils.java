package thc.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BeanUtils {
	private static Logger logger = LoggerFactory.getLogger(BeanUtils.class);

	// private constructor prevents instantiation
	private BeanUtils() { throw new UnsupportedOperationException(); }
	
	/**
	 * This method returns true if the collection is null or is empty.
	 * @param collection
	 * @return true | false
	 */
	public static boolean isEmpty( Collection<?> collection ){
		if( collection == null || collection.isEmpty() ){
			return true;
		}
		return false;
	}

	/**
	 * This method returns true of the map is null or is empty.
	 * @param map
	 * @return true | false
	 */
	public static boolean isEmpty( Map<?, ?> map ){
		if( map == null || map.isEmpty() ){
			return true;
		}
		return false;
	}

	/**
	 * This method returns true if the objet is null.
	 * @param object
	 * @return true | false
	 */
	public static boolean isEmpty( Object object ){
		if( object == null ){
			return true;
		}
		return false;
	}

	/**
	 * This method returns true if the input array is null or its length is zero.
	 * @param array
	 * @return true | false
	 */
	public static boolean isEmpty( Object[] array ){
		if( array == null || array.length == 0 ){
			return true;
		}
		return false;
	}

	/**
	 * This method returns true if the input string is null or its length is zero.
	 * @param string
	 * @return true | false
	 */
	public static boolean isEmpty( String string ){
		if( string == null || string.trim().length() == 0 ){
			return true;
		}
		return false;
	}

	/**
	 * Return a ordered list with max size is total
	 * @param total: max size of the list
	 * @param o: the medium element
	 * @param lowerList: elements lower than o
	 * @param higherList: elements higher than o
	 */
	public static<T> List<T> orderedList(int total, T o, List<T> lowerList, List<T> higherList) {
		int minSize = (int) Math.floor(total / 2);
		List<T> subResults = new ArrayList<T>();
		if (lowerList == null) lowerList = new ArrayList<T>();
		if (higherList == null) higherList = new ArrayList<T>();
		int lowerSize = lowerList.size();
		int higherSize = higherList.size();

		if (logger.isInfoEnabled()) {
			logger.info("orderedList: total[" + total + "]");
			logger.info("orderedList: lowerList.size:" + lowerSize);
			for (Object o1 : lowerList) logger.info("orderedList: lowerList[" + o1 + "]");
			logger.info("orderedList: higherList.size:" + higherSize);
			for (Object o2 : higherList) logger.info("orderedList: higherList[" + o2 + "]");
			logger.info("orderedList: minSize:" + minSize);
		}

		// do not have enough results for both list
		if (lowerSize < minSize && higherSize < minSize) {
			logger.info("orderedList: both lists are less than [" + minSize + "]");
			subResults.addAll(higherList);
			subResults.add(o);
			subResults.addAll(lowerList);
		}
		// lower is less than minreq
		else if (lowerSize < minSize) {
			logger.info("orderedList: lower list is less than [" + minSize + "]");
			subResults.add(o);
			subResults.addAll(lowerList);

			int index = total - subResults.size();
			if (index > higherSize) index = higherSize;
			logger.info("orderedList: get higherList index[" + index + "]");
			subResults.addAll(0, higherList.subList(higherList.size() - index, higherList.size()));
		}
		// higher is less than minreq
		else if (higherSize < minSize) {
			logger.info("orderedList: higher list is less than [" + minSize + "]");
			subResults.addAll(higherList);
			subResults.add(o);

			int index = total - subResults.size();
			if (index > lowerSize) index = lowerSize;
			logger.info("orderedList: get lowerList index[" + index + "]");
			subResults.addAll(lowerList.subList(0, index));
		}
		// Normal cases
		else {
			logger.info("orderedList: both list >= [" + minSize + "]");
			subResults.addAll(higherList.subList(higherList.size() - minSize, higherList.size()));
			subResults.add(o);
			subResults.addAll(lowerList.subList(0, minSize));
		}

		logger.info("orderedList: retrun sub list size:" + subResults.size());
		return subResults;
	}

	public static <T> Comparator<T> getCompare(final String... methods) {
		return new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				outer: for (String methodName : methods) {
					Object object1 = o1;
					Object object2 = o2;
					Class innerClass = o1.getClass();
					try {
						String[] methodNames = methodName.split("\\.");
						for (int i = 0; i < methodNames.length - 1; i++) {
							Method m = innerClass.getMethod(methodNames[i], null);
							innerClass = m.getReturnType();
							object1 = m.invoke(object1, null);
							object2 = m.invoke(object2, null);
							if (object1 == null || object2 == null) {
								if (object1 != null) {
									return -1;
								} else if (object2 != null) {
									return 1;
								} else {
									continue outer;
								}

							}
						}
						Method m = innerClass.getMethod(methodNames[methodNames.length - 1], null);
						Comparable v1 = (Comparable) m.invoke(object1, null);
						int v = v1.compareTo(m.invoke(object2, null));
						if (v != 0) {
							return v;
						}
					} catch (NullPointerException e) {
						return 0;
					} catch (Exception e) {
						System.out.println("NOOOOOOOOOOOO ~_~_~_~_~_~_~_~~__~_~_");
						return 0;
					}
				}
			return 0;
			}
		};
	}
}
