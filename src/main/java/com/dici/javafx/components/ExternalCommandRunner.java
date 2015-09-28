package com.dici.javafx.components;

import static com.dici.javafx.FXUtils.imageViewFromResource;
import static com.dici.javafx.components.Resources.STOP_ACTIVE_ICON;
import static com.dici.javafx.components.Resources.STOP_INACTIVE_ICON;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;

import com.dici.io.StreamPrinter;
import com.dici.process.ProcessUtils;

public class ExternalCommandRunner extends VBox {
    private final Button   stop;
    private final TextArea outputTextArea;
    private Process        runningProcess;
    
    public ExternalCommandRunner() {
        this.stop           = new Button("");
        this.outputTextArea = new TextArea();
        this.stop.setOnAction(ev -> ProcessUtils.killProcess(runningProcess));
        setInactive();
        super.getChildren().addAll(new ToolBar(stop), outputTextArea);
    }
    
    public TextArea getOutputTextArea() { return outputTextArea; }

    public synchronized void run(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        setActive();
        this.runningProcess = processBuilder.start();
        
        StreamPrinter  inputStream    = new StreamPrinter(runningProcess.getInputStream(), this::handleLog);
        StreamPrinter  errorStream    = new StreamPrinter(runningProcess.getErrorStream(), this::handleLog);
        outputTextArea.clear();
        
        new Thread(inputStream).start();
        new Thread(errorStream).start();
        runningProcess.waitFor();
        setInactive();
    }
    
    private void handleLog  (String line)     { Platform.runLater(() -> outputTextArea.appendText(line + "\n")); }
    private void setActive  ()                { setState(STOP_ACTIVE_ICON  )                                     ; }
    private void setInactive()                { setState(STOP_INACTIVE_ICON)                                     ; }
    private void setState   (String iconPath) { stop.setGraphic(imageViewFromResource(iconPath, Resources.class)); }
}