package org.aw.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by YURI-AK on 2017/4/7.
 */
public class MapUtil {
	public static Map<Object,Object> getAMap(Object key,Object value){
		Map<Object,Object> map=new HashMap<Object,Object>();
		map.put(key,value);
		return map;
	}
}
