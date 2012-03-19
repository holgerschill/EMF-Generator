package generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.w3c.dom.Document;


public class PlatformInitializer {

	private static String platformRootPath = null;
	private Map<String, String> bundleNameMapping = new HashMap<String, String>();

	static {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION,
				new XMIResourceFactoryImpl());
		EPackage.Registry.INSTANCE.put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
	}

	public void setPlatformUri(String pathToPlatform) {
		File f = new File(pathToPlatform);
		if (!f.exists())
			System.out.println("The platformUri location '" + pathToPlatform + "' does not exist");
		if (!f.isDirectory())
			System.out.println("The platformUri location must point to a directory");
		String path = f.getAbsolutePath();
		try {
			path = f.getCanonicalPath();
		}
		catch (IOException e) {
			System.out.println("Error when registering platform location");
		}
		if (platformRootPath == null || !platformRootPath.equals(path)) {
			platformRootPath = path;
			System.out.println("Registering platform uri '" + path + "'");
			if (f.exists()) {
				scanFolder(f);
			}
		}
	}

	protected boolean scanFolder(File f) {
		return scanFolder(f, new HashSet<String>());
	}

	protected boolean scanFolder(File f, Set<String> visitedPathes) {
		try {
			if (!visitedPathes.add(f.getCanonicalPath()))
				return true;
		}
		catch (IOException e) {
			return true;
		}
		File[] files = f.listFiles();
		boolean containsProject = false;
		File dotProject = null;
		if (files != null) {
			for (File file : files) {
				if (file.exists() && file.isDirectory() && !file.getName().startsWith(".")) {
					containsProject |= scanFolder(file, visitedPathes);
				}
				else if (".project".equals(file.getName())) {
					dotProject = file;
				}
				else if (file.getName().endsWith(".jar")) {
					registerBundle(file);
				}
			}
		}
		if (!containsProject && dotProject != null)
			registerProject(dotProject);
		return containsProject || dotProject != null;
	}

	protected void registerBundle(File file) {
		try {
			JarFile jarFile = new JarFile(file);
			Manifest manifest = jarFile.getManifest();
			if (manifest == null)
				return;
			String name = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
			if (name != null) {
				final int indexOf = name.indexOf(';');
				if (indexOf > 0)
					name = name.substring(0, indexOf);
				if (EcorePlugin.getPlatformResourceMap().containsKey(name))
					return;
				String path = "archive:" + file.toURI() + "!/";
				URI uri = URI.createURI(path);
				EcorePlugin.getPlatformResourceMap().put(name, uri);
			}
		}
		catch (ZipException e) {
			System.out.println("Could not open Jar file " + file.getAbsolutePath() + ".");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	protected void registerProject(File file) {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new FileInputStream(file));
			String name = document.getDocumentElement().getElementsByTagName("name").item(0).getTextContent();

			URI uri = URI.createFileURI(file.getParentFile().getCanonicalPath() + File.separator);
			EcorePlugin.getPlatformResourceMap().put(name, uri);
			if (bundleNameMapping.get(name) != null) {
				EcorePlugin.getPlatformResourceMap().put(bundleNameMapping.get(name), uri);
			}
			System.out.println("Registering project " + name + " at '" + uri + "'");
		}
		catch (Exception e) {
			throw new WrappedException("Couldn't read " + file, e);
		}
	}

}
