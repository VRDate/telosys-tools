package org.telosys.tools.eclipse.plugin.editors.dbrep;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.telosys.tools.commons.TelosysToolsLogger;
import org.telosys.tools.commons.Variable;
import org.telosys.tools.eclipse.plugin.commons.MsgBox;
import org.telosys.tools.eclipse.plugin.commons.TelosysPluginException;
import org.telosys.tools.generator.Generator;
import org.telosys.tools.generator.GeneratorException;
import org.telosys.tools.generator.RepositoryModelUtil;
import org.telosys.tools.generator.config.IGeneratorConfig;
import org.telosys.tools.generator.context.JavaBeanClass;
import org.telosys.tools.generator.context.Target;
import org.telosys.tools.generator.target.TargetDefinition;
import org.telosys.tools.repository.model.Entity;
import org.telosys.tools.repository.model.RepositoryModel;

/**
 * Eclipse runnable task with a progress bar 
 * for code generation 
 *  
 * @author Laurent Guerin
 *
 */
public class GenerationTaskWithProgress implements IRunnableWithProgress 
{
	private final static String ENTITY_NONE = "(no entity)" ;
	private final static String NO_TEMPLATE = "(no template)" ;
	
	private final LinkedList<String>           _entities ;
	private final LinkedList<TargetDefinition> _genericTargets ;
	private final RepositoryModel      _repositoryModel ;
	private final IGeneratorConfig     _generatorConfig ;
	private final IProject             _project ;
	private final TelosysToolsLogger   _logger ;

	//private String _currentEntityName = ENTITY_NONE ;
	private Target _currentTarget = null ;
	
	private int    _result = 0 ;
	
	//--------------------------------------------------------------------------------------------------
	/**
	 * Constructor
	 * @param entities
	 * @param targets
	 * @param repositoryModel
	 * @param generatorConfig
	 * @param project
	 * @param logger
	 * @throws TelosysPluginException
	 */
	public GenerationTaskWithProgress(
			LinkedList<String> entities, 
			LinkedList<TargetDefinition> targets,
			RepositoryModel repositoryModel, 
			IGeneratorConfig generatorConfig, 
			IProject project,
			TelosysToolsLogger logger
			) throws TelosysPluginException
	{
		super();
		
		_entities = entities ;
		_genericTargets = targets ;
		_repositoryModel = repositoryModel ;
		_generatorConfig = generatorConfig ;
		_project  = project ;
		_logger = logger ;
		
		if ( _entities == null ) throw new TelosysPluginException("_entities is null ");
		if ( _genericTargets == null ) throw new TelosysPluginException("_genericTargets is null ");
		if ( _repositoryModel == null ) throw new TelosysPluginException("_repositoryModel is null ");
		if ( _generatorConfig == null ) throw new TelosysPluginException("_generatorConfig is null ");
		if ( _project == null ) throw new TelosysPluginException("_project is null ");
		if ( _logger == null ) throw new TelosysPluginException("_logger is null ");
		
		_logger.log(this, "Task created");
	}
	
	//--------------------------------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException 
	{
		_logger.log(this, "run");
		
		//--- Build the list of "ONCE" targets ( NEW in version 2.0.3 / Feb 2013 )
		List<TargetDefinition> onceTargets   = new LinkedList<TargetDefinition>() ; 
		List<TargetDefinition> entityTargets = new LinkedList<TargetDefinition>() ; 
		for ( TargetDefinition targetDefinition : _genericTargets ) {
			if ( targetDefinition.isOnce() ) {
				onceTargets.add(targetDefinition);
			}
			else {
				entityTargets.add(targetDefinition);
			}
		}
		
		_result = 0 ;
		//--- Number of generations expected
		int totalWorkTasks = ( _entities.size() * entityTargets.size() ) + onceTargets.size() ;
		
		//--- Build the selected entities list 
		List<JavaBeanClass> selectedEntities;
		try {
			selectedEntities = RepositoryModelUtil.buildJavaBeanClasses(_entities, 
														_repositoryModel, _generatorConfig.getProjectConfiguration() );
		} catch (GeneratorException e1) {
			MsgBox.error("Cannot build selected entities ", e1);
			return ;
		}
		
		Variable[] projectVariables = _generatorConfig.getProjectConfiguration().getVariables();
		
		// count = total number of work units into which the main task is been subdivided
		progressMonitor.beginTask("Bulk generation in progress", totalWorkTasks ); 
			
		//--- For each entity
		for ( String entityName : _entities ) {
			
			_logger.log(this, "run : entity " + entityName );
			Entity entity = _repositoryModel.getEntityByName(entityName);
			if ( entity != null )
			{
				//--- For each "entity target" 
				for ( TargetDefinition targetDefinition : entityTargets ) {
					
					//--- Get a specialized target for the current entity
					Target target = new Target( targetDefinition, entity.getName(), 
							entity.getBeanJavaClass(), projectVariables );
					
					generateTarget(progressMonitor, target, selectedEntities); 
					
				}
				//--- One TARGET done
			}
			else
			{
				_logger.error("Entity '" + entityName + "' not found in the repository") ;
			}
			
			//--- One ENTITY done
		} // end of "For each entity"
		
		//--- Finally, generate the "ONCE" targets ( NEW in version 2.0.3 / Feb 2013 )
		for ( TargetDefinition targetDefinition : onceTargets ) {
			Target target = new Target( targetDefinition, "", "", projectVariables );
			generateTarget(progressMonitor, target, selectedEntities); 
		}
		
		//--- Notifies that the work is done; that is, either the main task is completed or the user canceled it.
		progressMonitor.done();
		
		if (progressMonitor.isCanceled()) // Returns whether cancellation of current operation has been requested
		{
			throw new InterruptedException("The bulk generation was cancelled");
		}
	}
	
	//--------------------------------------------------------------------------------------------------
	private void generateTarget(IProgressMonitor progressMonitor, Target target, List<JavaBeanClass> selectedEntities) 
					throws InvocationTargetException, InterruptedException 
	{

		_logger.log(this, "Generate TARGET : entity name '" + target.getEntityName() + "' - target file '" + target.getFile() + "' ");
		
		_currentTarget = target ;
		
		progressMonitor.subTask("Entity '" + target.getEntityName() + "' : target file '" + target.getFile() + "' ");
		
		//--- Possible multiple generated targets for one main target (with embedded generator)
		LinkedList<Target> generatedTargets = new LinkedList<Target>();
		try {
			Generator generator = new Generator(target, _generatorConfig, _logger);
			generator.setSelectedEntitiesInContext(selectedEntities); // New [2013-02-04]
			generator.generateTarget(target, _repositoryModel, generatedTargets);						
			
		} catch (GeneratorException e) {
			// if the "run" method must propagate a checked exception, 
			// it should wrap it inside an InvocationTargetException; 
			throw new InvocationTargetException(e);
		}

		//--- Refresh the generated files
		for ( Target generatedTarget : generatedTargets ) {
			_logger.log(this, "Refresh generated target : " + generatedTarget.getFile() );

			String outputFileNameInProject = generatedTarget.getOutputFileNameInProject() ;
			IFile iFile = _project.getFile( outputFileNameInProject );
			try {
				iFile.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				MsgBox.error("Cannot refresh file \n " + outputFileNameInProject );
				throw new InterruptedException("Cannot refresh file.");
			}
			
			//--- One more file : increment result count
			_result++ ;
		}
		
		//--- One TARGET done
		// Notifies that a given number of work unit of the main task has been completed. 
		// Note that this amount represents an installment, as opposed to a cumulative amount of work done to date.
		progressMonitor.worked(1); // One unit done (not cumulative)

	}
	
	//--------------------------------------------------------------------------------------------------
	/**
	 * Returns the name of the entity currently under generation 
	 * @return
	 */
	public String getCurrentEntityName() {
		if ( _currentTarget == null ) return ENTITY_NONE ;
		String entityName = _currentTarget.getEntityName() ;
		if ( entityName == null ) {
			return ENTITY_NONE ;
		}
		else if ( entityName.trim().length() == 0 ) {
			return ENTITY_NONE ;
		}
		else {
			return entityName ;
		}
	}
	
	//--------------------------------------------------------------------------------------------------
	/**
	 * Returns the name of the template currently in use for generation 
	 * @return
	 */
	public String getCurrentTemplateName() {
		if ( _currentTarget == null ) return NO_TEMPLATE ;
		return _currentTarget.getTemplate() ;
	}
	
	//--------------------------------------------------------------------------------------------------
	/**
	 * Returns the operation result : number of files generated
	 * @return
	 */
	public int getResult()
	{
		return _result ;
	}
}
