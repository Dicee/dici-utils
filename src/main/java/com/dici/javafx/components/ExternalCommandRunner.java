package com.dici.javafx.components;

import static com.dici.javafx.FXUtils.imageViewFromResource;
import static com.dici.javafx.components.Resources.STOP_ACTIVE_ICON;
import static com.dici.javafx.components.Resources.STOP_INACTIVE_ICON;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
    
    private final Semaphore runMutex = new Semaphore(1);
    
    public ExternalCommandRunner() {
        this.stop           = new Button("");
        this.outputTextArea = new TextArea();
        this.stop.setOnAction(ev -> ProcessUtils.killProcess(runningProcess));
        setInactive();
        super.getChildren().addAll(new ToolBar(stop), outputTextArea);
    }
    
    public TextArea getOutputTextArea() { return outputTextArea; }

    public void run(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        runMutex.acquire();
        setActive();
        ExternalCommandRunner self = this;
        Thread taskThread = new Thread(new Task<Void>() {
            @Override
            public Void call() throws Exception {
                runningProcess = processBuilder.start();
                
                StreamPrinter  inputStream = new StreamPrinter(runningProcess.getInputStream(), self::handleLog);
                StreamPrinter  errorStream = new StreamPrinter(runningProcess.getErrorStream(), self::handleLog);
                outputTextArea.clear();
                
                new Thread(inputStream).start();
                new Thread(errorStream).start();
                runningProcess.waitFor();
                return null;
            }
        
            @Override protected void cancelled() { super.cancelled(); terminate(); }
            @Override protected void failed   () { super.failed   (); terminate(); }
            @Override protected void succeeded() { super.succeeded(); terminate(); }
            
            private void terminate() {
                stop.fire();
                setInactive();
                runMutex.release();
            }
        });
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    private void handleLog  (String line) { Platform.runLater(() -> outputTextArea.appendText(line + "\n")); }
    private void setActive  ()            { setState(STOP_ACTIVE_ICON  , false)                            ; }
    private void setInactive()            { setState(STOP_INACTIVE_ICON, true)                             ; }
    private void setState   (String iconPath, boolean disableButton) { 
        stop.setGraphic(imageViewFromResource(iconPath, Resources.class));
        stop.setDisable(disableButton); 
    }
}