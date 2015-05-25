/**
 *  Copyright 2015 Stephan Rauh, http://www.beyondjava.net
 *  
 *  This file is part of BootsFaces.
 *  
 *  BootsFaces is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  BootsFaces is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with BootsFaces. If not, see <http://www.gnu.org/licenses/>.
 */
package net.bootsfaces.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlBody;
import javax.faces.component.html.HtmlHead;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import net.bootsfaces.C;

/**
 * This class adds the resource needed by BootsFaces and ensures that they are
 * loaded in the correct order. It replaces the former HeadListener.
 * 
 * @author Stephan Rauh
 */
public class AddResourcesListener implements SystemEventListener {

	private static final Logger LOGGER = Logger.getLogger(AddResourcesListener.class.getName());

	/**
	 * Components can request resources by registering them in the ViewMap,
	 * using the RESOURCE_KEY.
	 */
	private static final String RESOURCE_KEY = "net.bootsfaces.listeners.AddResourcesListener.ResourceFiles";

	static {
		LOGGER.info("net.bootsfaces.listeners.AddResourcesListener ready for use.");
	}

	/**
	 * Trigger adding the resources if and only if the event has been fired by
	 * UIViewRoot.
	 */
        @Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
            Logger.getLogger(AddResourcesListener.class.getName()).log(Level.INFO, "processEvent");
		Object source = event.getSource();
		if (source instanceof UIViewRoot) {
			final FacesContext context = FacesContext.getCurrentInstance();
			boolean isProduction = context.isProjectStage(ProjectStage.Production);

			addJavascript((UIViewRoot) source, context, isProduction);
		}
	}

	/**
	 * Add the required Javascript files and the FontAwesome CDN link.
	 * 
	 * @param root
	 *            The UIViewRoot of the JSF tree.
	 * @param context
	 *            The current FacesContext
	 * @param isProduction
	 *            This flag can be used to deliver different version of the JS
	 *            library, optimized for debugging or production.
	 */
	private void addJavascript(UIViewRoot root, FacesContext context, boolean isProduction) {
		Application app = context.getApplication();
		ResourceHandler rh = app.getResourceHandler();

		// If the BootsFaces_USETHEME parameter is true, render Theme CSS link
		String theme = null;
		theme = context.getExternalContext().getInitParameter(C.P_USETHEME);
		if (isFontAwesomeComponentUsedAndRemoveIt() || (theme != null && theme.equals(C.TRUE))) {
			Resource themeResource = rh.createResource(C.BSF_CSS_TBSTHEME, C.BSF_LIBRARY);

			if (themeResource == null) {
				throw new FacesException("Error loading theme, cannot find \"" + C.BSF_CSS_TBSTHEME
						+ "\" resource of \"" + C.BSF_LIBRARY + "\" library");
			} else {
				UIOutput output = new UIOutput();
				output.setRendererType("javax.faces.resource.Stylesheet");
				output.getAttributes().put("name", C.BSF_CSS_TBSTHEME);
				output.getAttributes().put("library", C.BSF_LIBRARY);
				output.getAttributes().put("target", "head");
				addResourceIfNecessary(root, context, output);
			}
		}

		// deactive FontAwesome support if the no-fa facet is found in the
		// h:head tag
		UIComponent header = findHeader(root);
		boolean useCDNImportForFontAwesome = (null == header) || (null == header.getFacet("no-fa"));
		if (useCDNImportForFontAwesome) {
			String useCDN = FacesContext.getCurrentInstance().getExternalContext()
					.getInitParameter("net.bootsfaces.get_fontawesome_from_cdn");
			if (null != useCDN)
				if (useCDN.equalsIgnoreCase("false") || useCDN.equals("no"))
					useCDNImportForFontAwesome = false;
		}

		// Do we have to add font-awesome and jQuery, or are the resources
		// already there?
		boolean loadJQuery = true;
		List<UIComponent> availableResources = root.getComponentResources(context, "head");
		for (UIComponent ava : availableResources) {
			String name = (String) ava.getAttributes().get("name");
			if (null != name) {
				name = name.toLowerCase();
				if ((name.contains("font-awesome") || name.contains("fontawesome")) && name.endsWith("css"))
					useCDNImportForFontAwesome = false;
				if (name.contains("jquery-ui") && name.endsWith(".js")) {
					// do nothing - the if is needed to avoid confusion between
					// jQuery and jQueryUI
				} else if (name.contains("jquery") && name.endsWith(".js")) {
					loadJQuery = false;
				}
			}
		}

		// Font Awesome
		if (useCDNImportForFontAwesome) { // !=null && usefa.equals(C.TRUE)) {
			InternalFALink output = new InternalFALink();
			output.getAttributes().put("src", C.FONTAWESOME_CDN_URL);
			addResourceIfNecessary(root, context, output);
		}

		Map<String, Object> viewMap = root.getViewMap();
		@SuppressWarnings("unchecked")
		Map<String, String> resourceMap = (Map<String, String>) viewMap.get(RESOURCE_KEY);

		if (null != resourceMap) {
			if (loadJQuery) {
				boolean needsJQuery = false;
				for (Entry<String, String> entry : resourceMap.entrySet()) {
					String file = entry.getValue();
					if ("jq/jquery.js".equals(file)) {
						needsJQuery = true;
					}
				}
				if (needsJQuery) {
					UIOutput output = new UIOutput();
					output.setRendererType("javax.faces.resource.Script");
					output.getAttributes().put("name", "jq/jquery.js");
					output.getAttributes().put("library", C.BSF_LIBRARY);
					output.getAttributes().put("target", "head");
					addResourceIfNecessary(root, context, output);
				}

			}

			for (Entry<String, String> entry : resourceMap.entrySet()) {
				String file = entry.getValue();
				String library = entry.getKey().substring(0, entry.getKey().length() - file.length() - 1);
				if (!"jq/jquery.js".equals(file)) {
					UIOutput output = new UIOutput();
					output.setRendererType("javax.faces.resource.Script");
					output.getAttributes().put("name", file);
					output.getAttributes().put("library", library);
					output.getAttributes().put("target", "head");
					addResourceIfNecessary(root, context, output);
				}

			}
		}

		enforceCorrectLoadOrder(root, context);

		{
			InternalIE8CompatiblityLinks output = new InternalIE8CompatiblityLinks();
			addResourceIfNecessary(root, context, output);
		}

	}

	private void addResourceIfNecessary(UIViewRoot root, FacesContext context, InternalIE8CompatiblityLinks output) {
		for (UIComponent c : root.getComponentResources(context, "head")) {
			if (c instanceof InternalIE8CompatiblityLinks)
				return;
		}
		root.addComponentResource(context, output, "head");
	}

	private void addResourceIfNecessary(UIViewRoot root, FacesContext context, InternalFALink output) {
		for (UIComponent c : root.getComponentResources(context, "head")) {
			if (c instanceof InternalFALink)
				return;
		}
		root.addComponentResource(context, output, "head");
	}

	private void addResourceIfNecessary(UIViewRoot root, FacesContext context, UIOutput output) {
		for (UIComponent c : root.getComponentResources(context, "head")) {
			String library = (String) c.getAttributes().get("library");
			String name = (String) c.getAttributes().get("name");
			if (library != null && library.equals(output.getAttributes().get("library"))) {
				if (name != null && library.equals(output.getAttributes().get("name"))) {
					return;
				}
			}
		}
		root.addComponentResource(context, output, "head");
	}

	/**
	 * Make sure jQuery is loaded before jQueryUI, and that every other
	 * Javascript is loaded later. Also make sure that the BootsFaces resource
	 * files are loaded prior to other resource files, giving the developer the
	 * opportunity to overwrite a CSS or JS file.
	 * 
	 * @param root
	 *            The current UIViewRoot
	 * @param context
	 *            The current FacesContext
	 */
	private void enforceCorrectLoadOrder(UIViewRoot root, FacesContext context) {
		List<UIComponent> resources = new ArrayList<UIComponent>(root.getComponentResources(context, "head"));
		Collections.sort(resources, new Comparator<UIComponent>() {

			@Override
			public int compare(UIComponent o1, UIComponent o2) {
				String name1 = (String) o1.getAttributes().get("name");
//				String lib1 = (String) o1.getAttributes().get("name");
				String name2 = (String) o2.getAttributes().get("name");
//				String lib2 = (String) o2.getAttributes().get("name");
				if (name1 == null)
					return 1;
				if (name2 == null)
					return -1;
				if (name1.endsWith(".js") && (!(name2.endsWith(".js"))))
					return 1;
				if (name2.endsWith(".js") && (!(name1.endsWith(".js"))))
					return -1;
				if (name1.endsWith(".js")) {
					if (name1.contains("jquery-ui"))
						name1 = "2.js"; // make it the second JS file
					else if (name1.contains("jquery"))
						name1 = "1.js"; // make it the second JS file
					else if (name1.contains("bsf.js"))
						name1 = "zzz.js"; // make it the last JS file
					else name1="keep.js"; // don't move it
				}
				if (name2.endsWith(".js")) {
					if (name2.contains("jquery-ui"))
						name2 = "2.js"; // make it the second JS file
					else if (name2.contains("jquery"))
						name2 = "1.js"; // make it the second JS file
					else if (name2.contains("bsf.js"))
						name2 = "zzz.js"; // make it the last JS file
					else name2="keep.js"; // don't move it
				}
				int result = name1.compareTo(name2);

				return result;
			}
		});

//		System.out.println("-------------------------------- sorted");
		for (UIComponent c : resources) {
//			System.out.println((String) c.getAttributes().get("name"));
			root.removeComponentResource(context, c);
		}
		for (UIComponent c : resources) {
			root.addComponentResource(context, c, "head");
		}
//		System.out.println("--------------------------------after");
//		resources = new ArrayList<UIComponent>(root.getComponentResources(context, "head"));
//		for (UIComponent c : resources) {
//			System.out.println((String) c.getAttributes().get("name"));
//		}
//		System.out.println("--------------------------------after");
	}

	/**
	 * Look whether a b:iconAwesome component is used. If so, the
	 * font-awesome.css is removed from the resource list because it's loaded
	 * from the CDN.
	 * 
	 * @return true, if the font-awesome.css is found in the resource list. Note
	 *         the side effect of this method!
	 */
	private boolean isFontAwesomeComponentUsedAndRemoveIt() {
		FacesContext fc = FacesContext.getCurrentInstance();
		UIViewRoot viewRoot = fc.getViewRoot();
		ListIterator<UIComponent> resourceIterator = (viewRoot.getComponentResources(fc, "head")).listIterator();
		UIComponent fontAwesomeResource = null;
		while (resourceIterator.hasNext()) {
			UIComponent resource = (UIComponent) resourceIterator.next();
			String name = (String) resource.getAttributes().get("name");
			// rw.write("\n<!-- res: '"+name+"' -->" );
			if (name != null) {
				if (name.endsWith("font-awesome.css"))
					fontAwesomeResource = resource;
			}
		}
		if (null != fontAwesomeResource) {
			viewRoot.removeComponentResource(fc, fontAwesomeResource);
			return true;
		}
		return false;

	}

	/**
	 * Looks for the header in the JSF tree.
	 * 
	 * @param root
	 *            The root of the JSF tree.
	 * @return null, if the head couldn't be found.
	 */
	private UIComponent findHeader(UIViewRoot root) {
		for (UIComponent c : root.getChildren()) {
			if (c instanceof HtmlHead)
				return c;
		}
		for (UIComponent c : root.getChildren()) {
			if (c instanceof HtmlBody)
				return null;
			if (c instanceof UIOutput)
				if (c.getFacets() != null)
					return c;
		}
		return null;
	}

	/**
	 * Which JSF elements do we listen to?
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		if (source instanceof UIComponent) {
			return true;
		}
		return false;
	}

	/**
	 * Registers a JS file that needs to be include in the header of the HTML
	 * file, but after jQuery and AngularJS.
	 * 
	 * @param library
	 *            The name of the sub-folder of the resources folder.
	 * @param resource
	 *            The name of the resource file within the library folder.
	 */
	public static void addResourceToHeadButAfterJQuery(String library, String resource) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		UIViewRoot v = ctx.getViewRoot();
		Map<String, Object> viewMap = v.getViewMap();
		@SuppressWarnings("unchecked")
		Map<String, String> resourceMap = (Map<String, String>) viewMap.get(RESOURCE_KEY);
		if (null == resourceMap) {
			resourceMap = new HashMap<String, String>();
			viewMap.put(RESOURCE_KEY, resourceMap);
		}
		String key = library + "#" + resource;
		if (!resourceMap.containsKey(key)) {
			resourceMap.put(key, resource);
		}
	}
}
