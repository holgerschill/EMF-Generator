package generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;


public class GenModelRegisterHelper {

	public void registerGenModel(ResourceSet rs, URI genmodelURI) {
		Resource res = rs.getResource(genmodelURI, true);
		if (res == null)
			return;
		for (EObject object : res.getContents())
			if (object instanceof GenModel)
				registerGenModel((GenModel) object);
	}

	protected Collection<GenPackage> collectGenPackages(GenModel genModel) {
		List<GenPackage> pkgs = new ArrayList<GenPackage>();
		for (GenPackage pkg : genModel.getGenPackages()) {
			pkgs.add(pkg);
			pkgs.addAll(collectGenPackages(pkg));
		}
		pkgs.addAll(genModel.getUsedGenPackages());
		return pkgs;
	}

	protected Collection<GenPackage> collectGenPackages(GenPackage genPackage) {
		List<GenPackage> pkgs = new ArrayList<GenPackage>();
		for (GenPackage pkg : genPackage.getNestedGenPackages()) {
			pkgs.add(pkg);
			pkgs.addAll(collectGenPackages(pkg));
		}
		return pkgs;
	}

	public void registerGenModel(GenModel genModel) {
		Map<String, URI> registry = EcorePlugin.getEPackageNsURIToGenModelLocationMap();
		for (GenPackage pkg : collectGenPackages(genModel)) {
			String nsURI = pkg.getEcorePackage().getNsURI();
			if (nsURI != null) {
				URI newUri = pkg.eResource().getURI();
				if (registry.containsKey(nsURI)) {
					URI oldURI = registry.get(nsURI);
					if (!oldURI.equals(newUri))
						System.out.println("There is already a GenModel registered for NamespaceURI '" + nsURI
								+ "'. It will be overwritten from '" + oldURI + "' to '" + newUri + "'");
					else
						continue;
				}
				registry.put(nsURI, newUri);
			}
		}
	}
}
