/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.bootsfaces.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 *
 * @author duncan
 */
public class RemoveBSFResouces implements SystemEventListener {

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        if (false) {
            UIViewRoot root = (UIViewRoot) event.getSource();
            FacesContext fc = FacesContext.getCurrentInstance();

            List<UIComponent> resources = new ArrayList<UIComponent>(root.getComponentResources(fc, "head"));
            ArrayList<UIComponent> bodyComponents = new ArrayList<UIComponent>(root.getComponentResources(fc, "body"));
            resources.addAll(bodyComponents);
            for (UIComponent c : resources) {
                if ("bsf".equals(c.getAttributes().get("library"))) {
                    root.removeComponentResource(fc, c);
                }
            }
        }
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return (source instanceof UIViewRoot);
    }
    
}
