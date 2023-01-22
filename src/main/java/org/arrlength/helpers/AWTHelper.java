package org.arrlength.helpers;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.dreambot.api.utilities.Logger.log;

public abstract class AWTHelper {

    public static Container getChildComponentByName(Container parentComponent, String childName) {

        // Define data structures that will hold components
        Map<String, Container> allComponentsMap = new HashMap();
        List<Container> allComponents = new ArrayList<>();

        // Iterating through the components structure and adding it to the List using our recursive function
        addAllChildComponentsToList(allComponents, parentComponent);

        // Iterating through the List and adding them to a HashMap keyed with their name
        for (Container c : allComponents) {
            allComponentsMap.put(c.getName(), c);
        }

        // Returning a component with the given name
        if (allComponentsMap.containsKey(childName)) {
            return allComponentsMap.get(childName);
        } else {
            log("ERROR: No match found when looking for GUI child components.");
            return null;
        }
    }

    private static void addAllChildComponentsToList(List<Container> componentArr, Container parentComponent) {

        // Making a list with all child components
        List<Container> childComponentsArr = Arrays.stream(parentComponent.getComponents()).map(c -> (Container) c).collect(Collectors.toList());

        if (childComponentsArr.size() > 0) {

            for (Container c : childComponentsArr) {

                // Adding a child component to the passed List
                componentArr.add(c);

                // Repeating the process if child has its own child components
                if (c.getComponents().length > 0) {
                    addAllChildComponentsToList(componentArr, c);
                }
            }
        } else {
            return;
        }
    }

}
