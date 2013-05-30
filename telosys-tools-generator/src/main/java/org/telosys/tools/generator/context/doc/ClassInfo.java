package org.telosys.tools.generator.context.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClassInfo {

	private final static String[] VOID_TEXT = new String[0] ;
	
	private String   name ;
	
	private String   contextName ;
	private String[] docText ;
	private String   since ;
	private boolean  deprecated ;
	
	//private LinkedList<MethodInfo> methodsInfoList ;
	private Map<String,MethodInfo> methodsInfoMap  ;
	
	public ClassInfo() {
		super();
		name = "???" ;
		docText = VOID_TEXT;
		since = "" ;
		deprecated = false ;
		//methodsInfoList = new LinkedList<MethodInfo>();
		methodsInfoMap  = new HashMap<String,MethodInfo>();
	}
	
	public String getJavaClassName() {
		return name;
	}
	protected void setJavaClassName(String name) {
		this.name = name;
	}
	
	public String getContextName() {
		return contextName;
	}
	protected void setContextName(String contextName) {
		this.contextName = contextName;
	}
	
	public String[] getDocText() {
		return docText;
	}
	protected void setDocText(String[] docText) {
		this.docText = docText;
	}
	
	public String getSince() {
		return since;
	}
	protected void setSince(String since) {
		this.since = since;
	}
	
	public boolean isDeprecated() {
		return deprecated;
	}
	protected void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}
	
	protected void addMethodInfo(MethodInfo methodInfo) {
		//methodsInfoList.add(methodInfo);
		methodsInfoMap.put(methodInfo.getSignature(), methodInfo);
	}

	public static <T extends Comparable<? super T>> List<T> sortList(Collection<T> c) {
		  List<T> list = new ArrayList<T>(c);
		  java.util.Collections.sort(list);
		  return list;
	}
	
	public List<MethodInfo> getMethodsInfo() {
		//return methodsInfoList ;
		Collection<MethodInfo> methods = methodsInfoMap.values();
		List<MethodInfo> list = new LinkedList<MethodInfo>();
		for ( MethodInfo m : methods ) {
			list.add(m);
		}
		Collections.sort(list);
		return list ;
	}

	public MethodInfo getMethodInfo(String signature) {
		return methodsInfoMap.get(signature) ;
	}

	public int getMethodsCount() {
		//return ( methodsInfoList != null ? methodsInfoList.size() : 0 ) ;
		return ( methodsInfoMap != null ? methodsInfoMap.size() : 0 ) ;
	}


	@Override
	public String toString() {
		return "ClassInfo : name=" + name + ", contextName=" + contextName 
				+ "\n docText=" + Arrays.toString(docText) 
				+ "\n since=" + since
				+ "\n deprecated=" + deprecated 
				+ "\n nb methodsInfo=" + methodsInfoMap.size()
				;
	}
	
	
}
