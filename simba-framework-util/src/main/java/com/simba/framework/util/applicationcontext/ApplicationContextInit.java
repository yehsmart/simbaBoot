package com.simba.framework.util.applicationcontext;

/**
 * Spring容器初始化完成之后，需要执行方法时，实现这个接口，注册成Bean
 * 
 * @author caozhejun
 *
 */
public interface ApplicationContextInit {

	/**
	 * Spring容器初始化完成之后要执行的方法
	 */
	public void init();
}
