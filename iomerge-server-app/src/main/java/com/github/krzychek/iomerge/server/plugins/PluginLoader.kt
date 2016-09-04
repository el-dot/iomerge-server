package com.github.krzychek.iomerge.server.plugins

import com.github.krzychek.iomerge.server.api.PluginProperties.PLUGIN_CLASSES_PROP
import com.github.krzychek.iomerge.server.api.appState.AppStateManager
import com.github.krzychek.iomerge.server.api.network.MessageDispatcher
import com.github.krzychek.iomerge.server.config.AppConfiguration
import com.github.krzychek.iomerge.server.network.MessageDispatcherImpl
import org.pmw.tinylog.Logger
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton class PluginLoader
@Inject constructor(appStateManager: AppStateManager, messageDispatcher: MessageDispatcherImpl, appConfiguration: AppConfiguration) {

	private val injectableObjects = mapOf(
			AppStateManager::class.java to appStateManager,
			MessageDispatcher::class.java to messageDispatcher
	)

	private val pluginClasses = readPluginJars(appConfiguration.pluginsDir)
			.flatMap { loadPluginClasses(it) }

	private val classToObject = WeakHashMap<Class<*>, Any>()

	/**
	 * @return stream of jar files in  plugins directory
	 */
	private fun readPluginJars(dir: File): Array<File> {
		return dir.listFiles { file -> file.isFile && file.name.endsWith(".jar") }
				?: emptyArray<File>()
	}

	/**
	 * create (or gets from cache, if already created) plugin objects of particular supertype
	 */
	fun <T> getPluginObjectsOfType(type: Class<T>): List<T>
			= pluginClasses
			.filter { type.isAssignableFrom(it) }
			.map { @Suppress("UNCHECKED_CAST") (it as Class<out T>) }
			.map { getObjectOfType(it) }


	/**
	 * creates objects of particular class or return if was created before
	 */
	private fun <T> getObjectOfType(clazz: Class<T>): T = clazz.cast(
			classToObject[clazz] ?:
					clazz.constructors.maxBy { it.parameterCount }!!.run {
						val parameters = parameterTypes.map { injectableObjects[it] }.toTypedArray()
						newInstance(*parameters)
					}
	)


	/**
	 * @return stream of configuration classes in given plugin jar file
	 */
	private fun loadPluginClasses(pluginJar: File): List<Class<*>> {
		try {
			val pluginClassLoader = URLClassLoader(
					arrayOf(pluginJar.toURI().toURL()),
					Thread.currentThread().contextClassLoader)

			// load classes
			return pluginClassLoader.getConfigurationClassNames()
					.map { pluginClassLoader.loadClass(it) }

		} catch (ex: Exception) {
			Logger.warn(ex, "Problem while loading plugin ", pluginJar)
			return emptyList()
		}
	}

	private fun ClassLoader.getConfigurationClassNames(): List<String> {
		val properties = Properties()
		this.getResourceAsStream("iomerge-plugin.properties").use {
			properties.load(it)
		}

		val classNames = properties.getProperty(PLUGIN_CLASSES_PROP)

		if (classNames.isNullOrEmpty())
			throw IllegalArgumentException("Plugin mendatory property: '$PLUGIN_CLASSES_PROP' not found")

		return classNames.split(",").filter { !it.isEmpty() }
	}
}