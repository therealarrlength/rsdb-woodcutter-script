package org.arrlength;

import org.arrlength.helpers.AWTHelper;
import org.arrlength.models.CoordinatesModeEnum;
import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.utilities.Sleep;

import javax.swing.*;
import java.awt.*;

// Most DreamBot api methods return boolean

@ScriptManifest(category = Category.WOODCUTTING, name = "Basic WoodCutter", author = "arrlength", version = 1.0)
public class WoodCutter extends AbstractScript {

    private boolean isScriptRunning = false;
    private boolean isInitialRun = true;
    private int resourceGatheredCount = 0;
    private Timer timer;

    // Action area setting
    private CoordinatesModeEnum coordinatesMode = CoordinatesModeEnum.CENTER_COORDINATE;
    private Area resourceGatheringArea = new Area();
    private Tile resourceAreaCenterCoordinates = new Tile();
//    private int resourceAreaRadius;

    // Object names
    private String resourceObjectName;
    private String inventoryResourceName = "Resource";
    private String resourceObjectActionName;
    private String resourceGatheringToolName;

    // GUI vars
    JFrame frame = new JFrame();
    Font titleFont = new Font("Arial", Font.BOLD, 16);


    // Runs once at the beginning of script load
    @Override
    public void onStart() {
        SwingUtilities.invokeLater(() -> createGUI());
        timer = new Timer();
        timer.pause();
    }

    // Repeatedly called by DreamBot framework
    @Override
    public int onLoop() {

        if (isScriptRunning) {
            GameObject closestResourceObject = GameObjects.closest(gameObject -> gameObject != null && gameObject.getName().equals(resourceObjectName) && gameObject.hasAction(resourceObjectActionName));
            if (!Inventory.isFull()) {

                // If player is in tree area then interact with the closest tree
                if (resourceGatheringArea.contains(Players.getLocal())) {

                    // If interaction with the tree is successful
                    if (closestResourceObject.interact(resourceObjectActionName)) {
                        int resourceInventoryCount = Inventory.count(inventoryResourceName);
                        // Sleep the loop until condition is met or 8000 ms
                        Sleep.sleepUntil(() -> Inventory.count(inventoryResourceName) > resourceInventoryCount, 16000);
                        resourceGatheredCount++;
                    }

                } else { // Start walking to area

                    // If character starts walking sleep. CHANGE TO sleepUntil reached area
                    if (Walking.walk(resourceAreaCenterCoordinates)) {
                        Sleep.sleep(3000, 6000);
                    }

                }

            } else if (Inventory.isFull()) {
                log("Inventory full");
                depositResources();
            } else {
                log("Interaction not successful");
            }
        }

        return 600;
    }

    // Executes once at the end of the script when stop button is clicked
    @Override
    public void onExit() {

    }

    // Draws infographics on the screen
    @Override
    public void onPaint(Graphics g) {
        super.onPaint(g);

        g.setColor(Color.ORANGE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("" + timer.formatTime(), 10, 35);

        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString(inventoryResourceName + " gathered: " + resourceGatheredCount, 10, 50);

        // MAYBE SHOW RESOURCE/HOUR
    }

    // MARK: - Helper Methods
    private void depositResources() {

        // POSSIBLY change to depositing everything and only picking up required tool
        while (Bank.open() && Inventory.count(inventoryResourceName) > 0) {
            Bank.depositAll(inventoryResourceName);
        }

        Bank.close();
    }

    private void createGUI() {

        // Creating Java Swing frame
        frame.setTitle("Gather resource");
        frame.setName("Frame");
        frame.setLayout(new BoxLayout(frame, BoxLayout.PAGE_AXIS));

        Container contentPane = frame.getContentPane();

        // Changing default close operation to Dispose to avoid it closing the client too
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Setting GUI in the middle of DreamBot client
        frame.setLocationRelativeTo(Client.getCanvas());

        // Setting GUI size
//        frame.setPreferredSize(new Dimension(400, 300));

        // Setting GUI layout
        contentPane.setLayout(new BorderLayout());

        // INITIALIZING COMPONENTS
        // ** Creating run button
        JButton switchModeButton = new JButton();
        switchModeButton.setText("Switch mode");
        switchModeButton.setBackground(Color.GREEN);

        JPanel resourceAreaPanel = generateGUICCResourceAreaPanel(switchModeButton);

        // ** Adding settings panel to the GUI
        contentPane.add(resourceAreaPanel, BorderLayout.NORTH);

        // ** Setting listener
        switchModeButton.addActionListener(listener -> {
            log("BUTTON PRESSED");
            JPanel oldPanel = (JPanel) AWTHelper.getChildComponentByName(frame, "ResourceAreaPanel");
            frame.getContentPane().remove(oldPanel);

            log("THROU");
            if (coordinatesMode == CoordinatesModeEnum.CENTER_COORDINATE) {
                JPanel newResourceAreaPanel = generateGUICTCResourceAreaPanel(switchModeButton);
                contentPane.add(newResourceAreaPanel, BorderLayout.NORTH);
                coordinatesMode = CoordinatesModeEnum.CORNER_COORDINATES;
            } else if (coordinatesMode == CoordinatesModeEnum.CORNER_COORDINATES) {
                JPanel newResourceAreaPanel = generateGUICCResourceAreaPanel(switchModeButton);
                contentPane.add(newResourceAreaPanel, BorderLayout.NORTH);
                coordinatesMode = CoordinatesModeEnum.CENTER_COORDINATE;
            }

            SwingUtilities.updateComponentTreeUI(frame);
        });

        // * Resource selection panel
        JPanel resourceSelectionPanel = new JPanel();
        resourceSelectionPanel.setLayout(new GridLayout(0, 2, 10, 0));
        resourceSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 5));

        // ** Setting label
        JLabel resourceSelectionTitle = new JLabel();
        resourceSelectionTitle.setText("Select resource");
        resourceSelectionTitle.setFont(titleFont);
        resourceSelectionPanel.add(resourceSelectionTitle);

        JLabel resourceSelectionTitleSpacer = new JLabel();
        resourceSelectionTitleSpacer.setText("");
        resourceSelectionPanel.add(resourceSelectionTitleSpacer);

        // ** Setting upper panel label
        JLabel resourceLabel = new JLabel();
        resourceLabel.setText("Resource:");
        resourceSelectionPanel.add(resourceLabel);

        // ** Creating JComboBox dropdown
        String[] resourceDropdownOptions = new String[]{
                "Tree", "Oak"
        };
        JComboBox<String> resourceComboBox = new JComboBox<>(resourceDropdownOptions);
        resourceComboBox.setName("ResourceComboBox");
        resourceSelectionPanel.add(resourceComboBox);

        // ** Adding resource selection panel to the GUI
        contentPane.add(resourceSelectionPanel, BorderLayout.CENTER);

        // * Creating GUI buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());

        // ** Creating run button
        JButton gatherButton = new JButton();
        gatherButton.setName("GatherButton");
        gatherButton.setText("Gather");
        gatherButton.setBackground(Color.GREEN);

        // ** Setting listener
        gatherButton.addActionListener(listener -> {
            handleGatherButtonPress();
        });

        buttonsPanel.add(gatherButton);

        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));

        // ** Adding Button panel to the GUI
        contentPane.add(buttonsPanel, BorderLayout.PAGE_END);

        // FINISHING UP
        // Sizing GUI to make all components visible
        frame.pack();

        // Setting GUI visible
        frame.setVisible(true);

        //        // ** Creating checkbox
//        JCheckBox lootCheckBox = new JCheckBox();
//        lootCheckBox.setText("Loot bones");
//        settingsPanel.add(lootCheckBox);
//
//        lootCheckBox.addActionListener(new ActionListener() {
//          public void actionPerformed(ActionEvent actionEvent) {
//
//          }
//        });
//
//        settingsPanel.add(lootCheckBox);

    }

    private void handleGatherButtonPress() {

        JComboBox<String> resourceComboBox = (JComboBox<String>) AWTHelper.getChildComponentByName(frame, "ResourceComboBox");
        JPanel resourceAreaPanel = (JPanel) AWTHelper.getChildComponentByName(frame, "ResourceAreaPanel");
        JButton gatherButton = (JButton) AWTHelper.getChildComponentByName(frame, "GatherButton");

        if (!isScriptRunning || isInitialRun) {
            setResourceArea();

            // Setting variables
            resourceObjectName = String.valueOf(resourceComboBox.getSelectedItem());
            setResourceVars();

            isScriptRunning = true;
            isInitialRun = false;
            gatherButton.setText("Stop");
            gatherButton.setBackground(Color.RED);
            log("Started " + resourceObjectActionName + " " + resourceObjectName);

            timer.start();
        } else {
            isScriptRunning = false;
            gatherButton.setText("Gather");
            gatherButton.setBackground(Color.GREEN);
            log("Stopped gathering");
        }

    }

    private void setResourceArea() {
        JPanel resourceAreaPanel = (JPanel) AWTHelper.getChildComponentByName(frame, "ResourceAreaPanel");

        if (coordinatesMode == CoordinatesModeEnum.CENTER_COORDINATE) {
            JTextField xField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CCXTextField");
            JTextField yField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CCYTextField");
            JTextField zField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CCZTextField");
            JTextField radiusField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CCRadiusTextField");

            resourceAreaCenterCoordinates.setX(Integer.valueOf(xField.getText()));
            resourceAreaCenterCoordinates.setY(Integer.valueOf(yField.getText()));
            resourceAreaCenterCoordinates.setZ(Integer.valueOf(zField.getText()));

            int resourceAreaRadius = Integer.valueOf(radiusField.getText());
            resourceGatheringArea = resourceGatheringArea.generateArea(resourceAreaRadius, resourceAreaCenterCoordinates);
        } else if (coordinatesMode == CoordinatesModeEnum.CORNER_COORDINATES) {
            JTextField c1xField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CTC1XTextField");
            JTextField c1yField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CTC1YTextField");
            JTextField c2xField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CTC2XTextField");
            JTextField c2yField = (JTextField) AWTHelper.getChildComponentByName(resourceAreaPanel, "CTC2YTextField");

            int c1x = Integer.valueOf(c1xField.getText());
            int c1y = Integer.valueOf(c1yField.getText());
            int c2x = Integer.valueOf(c2xField.getText());
            int c2y = Integer.valueOf(c2yField.getText());

            resourceGatheringArea = new Area(c1x, c1y, c2x, c2y);
            resourceAreaCenterCoordinates = resourceGatheringArea.getCenter();
        }

    }

    // Creating corner to corner coordinates resource area GUI panel
    private JPanel generateGUICTCResourceAreaPanel(JButton switchModeButton) {
        // * Creating resource area panel
        JPanel resourceAreaPanel = new JPanel();
        resourceAreaPanel.setName("ResourceAreaPanel");
        resourceAreaPanel.setLayout(new GridLayout(0, 2, 10, 0));
        resourceAreaPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        // ** Setting label
        JLabel resourceAreaTitle = new JLabel();
        resourceAreaTitle.setText("Resource Area");
        resourceAreaTitle.setFont(titleFont);
        resourceAreaPanel.add(resourceAreaTitle);

        resourceAreaPanel.add(switchModeButton);

        // ** Setting center tile row label
        JLabel cornerOneCoordinatesLabel = new JLabel();
        cornerOneCoordinatesLabel.setText("Corner 1:");
        resourceAreaPanel.add(cornerOneCoordinatesLabel);

        // ** Creating center tile text fields
        // *** Creating a panel to hold 3 inputs
        JPanel cornerOneInputsPanel = new JPanel();

        // *** Creating and adding labels and text fields for each coordinate
        JLabel cornerOneXCoordinateLabel = new JLabel();
        cornerOneXCoordinateLabel.setText("X:");
        JTextField cornerOneXCoordinateTextField = new JTextField("", 3);
        cornerOneXCoordinateTextField.setName("CTC1XTextField");
        cornerOneInputsPanel.add(cornerOneXCoordinateLabel);
        cornerOneInputsPanel.add(cornerOneXCoordinateTextField);

        JLabel cornerOneYCoordinateLabel = new JLabel();
        cornerOneYCoordinateLabel.setText("Y:");
        JTextField cornerOneYCoordinateTextField = new JTextField("", 3);
        cornerOneYCoordinateTextField.setName("CTC1YTextField");
        cornerOneInputsPanel.add(cornerOneYCoordinateLabel);
        cornerOneInputsPanel.add(cornerOneYCoordinateTextField);

        resourceAreaPanel.add(cornerOneInputsPanel);

        // ** Setting center tile row label
        JLabel cornerTwoCoordinatesLabel = new JLabel();
        cornerTwoCoordinatesLabel.setText("Corner 2:");
        resourceAreaPanel.add(cornerTwoCoordinatesLabel);

        // ** Creating center tile text fields
        // *** Creating a panel to hold 3 inputs
        JPanel cornerTwoInputsPanel = new JPanel();

        // *** Creating and adding labels and text fields for each coordinate
        JLabel cornerTwoXCoordinateLabel = new JLabel();
        cornerTwoXCoordinateLabel.setText("X:");
        JTextField cornerTwoXCoordinateTextField = new JTextField("", 3);
        cornerTwoXCoordinateTextField.setName("CTC2XTextField");
        cornerTwoInputsPanel.add(cornerTwoXCoordinateLabel);
        cornerTwoInputsPanel.add(cornerTwoXCoordinateTextField);

        JLabel cornerTwoYCoordinateLabel = new JLabel();
        cornerTwoYCoordinateLabel.setText("Y:");
        JTextField cornerTwoYCoordinateTextField = new JTextField("", 3);
        cornerTwoYCoordinateTextField.setName("CTC2YTextField");
        cornerTwoInputsPanel.add(cornerTwoYCoordinateLabel);
        cornerTwoInputsPanel.add(cornerTwoYCoordinateTextField);

        resourceAreaPanel.add(cornerTwoInputsPanel);

        return resourceAreaPanel;
    }

    // Create center coordinates resource area GUI panel
    private JPanel generateGUICCResourceAreaPanel(JButton switchModeButton) {
        // * Creating resource area panel
        JPanel resourceAreaPanel = new JPanel();
        resourceAreaPanel.setName("ResourceAreaPanel");
        resourceAreaPanel.setLayout(new GridLayout(0, 2, 10, 0));
        resourceAreaPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        // ** Setting label
        JLabel resourceAreaTitle = new JLabel();
        resourceAreaTitle.setText("Resource Area");
        resourceAreaTitle.setFont(titleFont);
        resourceAreaPanel.add(resourceAreaTitle);

        resourceAreaPanel.add(switchModeButton);

        // ** Setting center tile row label
        JLabel centerTileCoordinatesLabel = new JLabel();
        centerTileCoordinatesLabel.setText("Center:");
        resourceAreaPanel.add(centerTileCoordinatesLabel);

        // ** Creating center tile text fields
        // *** Creating a panel to hold 3 inputs
        JPanel resourceAreaCoordinatesInputsPanel = new JPanel();

        // *** Creating and adding labels and text fields for each coordinate
        JLabel centerXCoordinateLabel = new JLabel();
        centerXCoordinateLabel.setText("X:");
        JTextField centerXCoordinateTextField = new JTextField("", 3);
        centerXCoordinateTextField.setName("CCXTextField");
        resourceAreaCoordinatesInputsPanel.add(centerXCoordinateLabel);
        resourceAreaCoordinatesInputsPanel.add(centerXCoordinateTextField);

        JLabel centerYCoordinateLabel = new JLabel();
        centerYCoordinateLabel.setText("Y:");
        JTextField centerYCoordinateTextField = new JTextField("", 3);
        centerYCoordinateTextField.setName("CCYTextField");
        resourceAreaCoordinatesInputsPanel.add(centerYCoordinateLabel);
        resourceAreaCoordinatesInputsPanel.add(centerYCoordinateTextField);

        JLabel centerZCoordinateLabel = new JLabel();
        centerZCoordinateLabel.setText("Z:");
        JTextField centerZCoordinateTextField = new JTextField("", 3);
        centerZCoordinateTextField.setName("CCZTextField");
        resourceAreaCoordinatesInputsPanel.add(centerZCoordinateLabel);
        resourceAreaCoordinatesInputsPanel.add(centerZCoordinateTextField);

        // **** Creating Get Player Coordinates button
        Font playerLocationButtonFont = new Font("Arial", Font.BOLD, 20);

        JButton getPlayerLocationButton = new JButton();
        getPlayerLocationButton.setPreferredSize(new Dimension(25, 25));
        getPlayerLocationButton.setBackground(Color.CYAN);
        getPlayerLocationButton.setText("⌖");
        getPlayerLocationButton.setFont(playerLocationButtonFont);

        getPlayerLocationButton.addActionListener(listener -> {
            centerXCoordinateTextField.setText(String.valueOf(Players.getLocal().getX()));
            centerYCoordinateTextField.setText(String.valueOf(Players.getLocal().getY()));
            centerZCoordinateTextField.setText(String.valueOf(Players.getLocal().getZ()));
        });

        resourceAreaCoordinatesInputsPanel.add(getPlayerLocationButton);
        resourceAreaPanel.add(resourceAreaCoordinatesInputsPanel);

        // ** Setting radius label
        JLabel radiusLabel = new JLabel();
        radiusLabel.setText("Radius:");
        resourceAreaPanel.add(radiusLabel);

        // ** Creating radius text field
        JTextField resourceAreaRadiusTextField = new JTextField();
        resourceAreaRadiusTextField.setName("CCRadiusTextField");
        resourceAreaPanel.add(resourceAreaRadiusTextField);

        return resourceAreaPanel;
    }

    private void setResourceVars() {
        switch (resourceObjectName) {
            case "Tree":
                inventoryResourceName = "Logs";
                resourceObjectActionName = "Chop Down";
                break;
            case "Oak":
                inventoryResourceName = "Oak Logs";
                resourceObjectActionName = "Chop Down";
                break;
        }
    }
}