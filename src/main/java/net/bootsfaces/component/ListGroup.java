/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.bootsfaces.component;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import net.bootsfaces.C;

/**
 *
 * @author duncan
 */
@ResourceDependency(library="bsf", name="css/core.css")
@FacesComponent(C.LISTGROUP_COMPONENT_TYPE)
public class ListGroup extends LinksContainer {
    
    private static final  String STYLE = "list-group";
    
    /**
     * <p>The standard component type for this component.</p>
     */
    public static final String COMPONENT_TYPE =C.LISTGROUP_COMPONENT_TYPE;
    /**
     * <p>The component family for this component.</p>
     */
    public static final String COMPONENT_FAMILY = C.BSFCOMPONENT;

    public ListGroup() {
        setRendererType(null);
    }

    @Override
    public void encodeBegin(FacesContext fc) throws IOException {
        List<UIComponent> children = getChildren();
        for (UIComponent child : children) {
            String styleClass = (String) child.getAttributes().get("styleClass");
            if (styleClass != null) {
                styleClass = styleClass + " " + "list-group-item";
                child.getAttributes().put("styleClass", styleClass);
            } else {
                child.getAttributes().put("styleClass", "list-group-item");
            }
        }
        super.encodeBegin(fc);
    }

    
    @Override
    protected String getContainerStyles() {
        return STYLE;
    }
    
    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }
}
