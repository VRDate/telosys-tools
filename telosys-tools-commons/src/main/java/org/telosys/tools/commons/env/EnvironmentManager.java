/**
 *  Copyright (C) 2008-2014  Telosys project org. ( http://www.telosys.org/ )
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.gnu.org/licenses/lgpl.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.telosys.tools.commons.env;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.telosys.tools.commons.FileUtil;
import org.telosys.tools.commons.StrUtil;

/**
 * Telosys Tools environment manager <br>
 * For environment initialization (folders creation, files initialization, etc) <br>
 * All the methods are designed to log the actions result in a StringBuffer.
 * 
 * @author L Guerin
 *
 */
public class EnvironmentManager {

	public final static String TELOSYS_TOOLS_CFG = "telosys-tools.cfg" ;
	
	public final static String DATABASES_DBCFG   = "databases.dbcfg" ;

	private final String environmentDirectory ;
	
	
	/**
	 * Constructor
	 * @param environmentDirectory the directory where the environment is located (OS full path)
	 */
	public EnvironmentManager(String environmentDirectory) {
		super();
		this.environmentDirectory = environmentDirectory;
	}

	
	/**
	 * Returns the environment directory (OS full path)
	 * @return
	 */
	public String getEnvironmentDirectory() {
		return environmentDirectory;
	}

	/**
	 * Returns the TelosysTools configuration file path (OS full path)
	 * @return
	 */
	public String getTelosysToolsConfigFile() {
		return FileUtil.buildFilePath(environmentDirectory, TELOSYS_TOOLS_CFG) ;
	}

	/**
	 * Initializes the environment using the standard folders and configuration files
	 * @param sb
	 */
	public void initStandardEnvironment(StringBuffer sb){
		createFolder( "TelosysTools", sb );
		createFolder( "TelosysTools/downloads", sb );
		createFolder( "TelosysTools/lib", sb );
		createFolder( "TelosysTools/templates", sb );
		initDatabasesConfigFile( "TelosysTools", sb );
		initTelosysToolsConfigFile( sb);		
	}
	
	/**
	 * Creates a folder in the environment folder
	 * @param folderToBeCreated 
	 * @param sb
	 */
	public void createFolder(String folderToBeCreated, StringBuffer sb){

		if ( ! StrUtil.nullOrVoid(folderToBeCreated) )  {
			folderToBeCreated = folderToBeCreated.trim() ;
			String newDirPath = FileUtil.buildFilePath(environmentDirectory, folderToBeCreated);
			File newDir = new File(newDirPath) ; 
			if ( newDir.exists() ) {
				sb.append(". folder '" + folderToBeCreated + "' exists (not created)");
			}
			else {
				boolean created = newDir.mkdirs();
				//boolean created = EclipseProjUtil.createFolder(project, folderName ) ;	
				if ( created ) {
					sb.append(". folder '" + folderToBeCreated + "' created");
				}
				else {
					sb.append(". folder '" + folderToBeCreated + "' not created (ERROR)");
				}
			}
		}
		sb.append("\n");
	}	
	

	/**
	 * Initializes the Telosys Tools configuration file <br>
	 * Copy the default configuration file in the environment folder
	 * @param sb
	 */
	public void initTelosysToolsConfigFile( StringBuffer sb ){
		String destinationFullPath = FileUtil.buildFilePath(environmentDirectory, TELOSYS_TOOLS_CFG) ; 
		copyFileFromMetaInf(destinationFullPath, TELOSYS_TOOLS_CFG, sb);
	}
	
	/**
	 * Initializes the Telosys Tools databases configuration file <br>
	 * Copy the default databases configuration file in the given environment sub-folder 
	 * @param folder
	 * @param sb
	 */
	public void initDatabasesConfigFile( String folder, StringBuffer sb ){
		String dirFullPath = fullPathInEnvironmentDir(folder);
		File destinationDir = new File (dirFullPath) ;
		if ( destinationDir.exists() != true ) {
			sb.append("ERROR : cannot create '" + DATABASES_DBCFG + "' file, directory '" + folder + "' doesn't exist ! \n");
			return ;
		}
		if ( destinationDir.isDirectory() != true ) {
			sb.append("ERROR : cannot create '" + DATABASES_DBCFG + "' file, '" + folder + "' is not a directory ! \n");
			return ;
		}
		
		String outsideFilePath = FileUtil.buildFilePath(dirFullPath, DATABASES_DBCFG) ; 
		copyFileFromMetaInf(outsideFilePath, DATABASES_DBCFG, sb);
	}

	private String fullPathInEnvironmentDir(String dir){
		
		String dir2 = "" ;
		if ( ! StrUtil.nullOrVoid(dir) )  {
			dir2 = dir.trim() ;
		}
		return FileUtil.buildFilePath(environmentDirectory, dir2);
	}
	
	//private final static String INSIDE_FILE = "/META-INF/files/" + Const.DATABASES_DBCFG ;
    private static final int BUFFER_SIZE = 1024 ; // 1 kb   
	private final static String META_INF_FILES = "/META-INF/files/" ;
	private void copyFileFromMetaInf(String destFullPath, String fileName, StringBuffer sb){
		File destFile = new File (destFullPath) ;
		if ( destFile.exists() ) {
			sb.append(". file '" + destFullPath + "' exists (not created)");
		}
		else {
			String fileNameInMetaInf = META_INF_FILES + fileName ;
			//--- Get input stream (file in JAR)
			InputStream is = EnvironmentManager.class.getResourceAsStream(fileNameInMetaInf);
			if ( is != null ) {
				//--- Open output stream
				FileOutputStream fos = null;
		        try
		        {
		            fos = new FileOutputStream(destFullPath);
		            try {
						copyAndClose(is, fos);
						sb.append(". file '" + destFullPath + "' created. \n");
					} catch (IOException ioex) {
						sb.append("ERROR : IOException : " + ioex.getMessage() + "\n");
					}
		        } catch (FileNotFoundException ex)
		        {
		            sb.append("ERROR : cannot create output file '" + destFullPath + "' ! \n");
		        }
		        
//		        //--- Copy and close
//				byte buffer[] = new byte[BUFFER_SIZE];
//				int len = 0;
//				
//				try
//	            {
//	                while ((len = is.read(buffer)) > 0)
//	                {
//	                	fos.write(buffer, 0, len);
//	                }
//	                is.close();
//	                fos.close();
//	                
//	    			sb.append(". file '" + destFullPath + "' created.");
//	                
//	            } catch (IOException ioex)
//	            {
//		            sb.append("ERROR : IOException : " + ioex.getMessage() );
//	            }

			}
			else {
				sb.append("ERROR : '" + fileName + "' input file not found in jar ! \n");
			}
		}
	}
	
	private void copyAndClose(InputStream is, FileOutputStream fos) throws IOException {
		byte buffer[] = new byte[BUFFER_SIZE];
		int len = 0;
		
        while ((len = is.read(buffer)) > 0)
        {
        	fos.write(buffer, 0, len);
        }
        is.close();
        fos.close();
	}
}
