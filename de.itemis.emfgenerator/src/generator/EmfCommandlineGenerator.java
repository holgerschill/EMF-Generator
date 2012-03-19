package generator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.merge.java.JControlModel;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;


public class EmfCommandlineGenerator {

	public static void main(String[] args) {
		if(args.length != 2){
			System.err.println("ERROR -> Usage: 1. Param: Absolut path to Workspace  2. Param: Platform URI to genmodel");
			return;
		}
		String platform = args[0];
		String file = args[1];
		Generator generator = setup(platform);
		URI uri = URI.createURI(file);
		System.out.println("Loading GenModel with URI: " + uri);
		ResourceSet rs = new ResourceSetImpl();
		Resource resource = rs.getResource(uri, true);
		GenModel genModel = (GenModel) resource.getContents().get(0);
		genModel.setValidateModel(true);
		Diagnostician diagnostician = new Diagnostician();
		for(EObject object : resource.getContents()){
			diagnostician.validate(object);
			
		}
		IStatus validate = genModel.validate();
		
		if(validate.isOK()){
			genModel.setCanGenerate(true);
			genModel.reconcile();
			GenModelRegisterHelper genModelHelper = new GenModelRegisterHelper();
			genModelHelper.registerGenModel(genModel);
			generator.setInput(genModel);
			Diagnostic diagnostic = generator.generate(genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE,
					new BasicMonitor());
			System.out.println(diagnostic.getMessage());
		} else {
			System.err.println(validate.getMessage());
			for(IStatus child : validate.getChildren()){
				System.err.println(child.getMessage());
			}
		}
	}

	private static Generator setup(
			String platform) {
		GenModelPackage.eINSTANCE.getGenAnnotation();
		EcorePackage.eINSTANCE.getEFactoryInstance();
		GenModelPackage.eINSTANCE.getEFactoryInstance();
		PlatformInitializer platformInitializer = new PlatformInitializer();
		platformInitializer.setPlatformUri(platform);
		if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("genmodel"))
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("genmodel",
					new org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl());
		org.eclipse.emf.codegen.ecore.generator.Generator generator = new org.eclipse.emf.codegen.ecore.generator.Generator(){
			@Override
			public JControlModel getJControlModel() {
				if (jControlModel == null) {
				      jControlModel = new JControlModel();
				      jControlModel.initialize(null, options.mergeRulesURI);
				}
				return jControlModel;
			}
		};
		generator.getAdapterFactoryDescriptorRegistry().addDescriptor(GenModelPackage.eNS_URI,org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory.DESCRIPTOR);
		return generator;
	}

}
